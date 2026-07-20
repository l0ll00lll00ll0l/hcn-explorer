package com.hcn.db;

import com.hcn.newCore.PrimeCenter;
import com.hcn.newCore.ScientificNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DbCacheService {

    @Autowired
    private DatabaseService databaseService;

    private String dbName;
    private Integer highestLapi;
    private final Map<Integer, DbInterval> intervalCache = new HashMap<>();
    private final Map<Integer, DbBody> bodyCache = new HashMap<>();
    private final PrimeCenter primeCenter = new PrimeCenter();

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public Map<Integer, DbInterval> getIntervalCache() { return intervalCache; }
    public int getIntervalCacheSize() { return intervalCache.size(); }
    public int getBodyCacheSize() { return bodyCache.size(); }

    public void generateHcnData(int firstLapi, int lastLapi) {
        List<DbInterval> fetched = getMissingIntervals(firstLapi, lastLapi);
        if (!fetched.isEmpty()) {
            DbInterval first = intervalCache.get(firstLapi);
            Integer refLapi = first.getReferenceInterval();
            if (refLapi != null && !intervalCache.containsKey(refLapi)) {
                fetched.add(0, getMissingIntervals(refLapi, refLapi).get(0));
            }

            updateHcnList(fetched);
        }
    }

    public void clear() {
        highestLapi = null;
        intervalCache.clear();
        bodyCache.clear();
    }

    public Integer getHighestLapi() {
        if (highestLapi == null && dbName != null) {
            highestLapi = t().queryForObject("SELECT MAX(lapi) FROM interval", Integer.class);
        }
        return highestLapi;
    }

    public List<DbInterval> getMissingIntervals(int firstLapi, int lastLapi) {
        List<Integer> missing = new ArrayList<>();
        for (int lapi = firstLapi; lapi <= lastLapi; lapi++) {
            if (!intervalCache.containsKey(lapi)) missing.add(lapi);
        }
        if (missing.isEmpty()) return Collections.emptyList();
        String in = missing.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<DbInterval> fetched = new ArrayList<>();
        t().query("SELECT lapi, value_mantissa, value_exponent, factor_mantissa, factor_exponent, first_hcn, size, reference_interval FROM interval WHERE lapi IN (" + in + ")", rs -> {
            DbInterval iv = new DbInterval(rs.getInt("lapi"), rs.getDouble("value_mantissa"), rs.getLong("value_exponent"),
                    rs.getDouble("factor_mantissa"), rs.getLong("factor_exponent"),
                    rs.getLong("first_hcn"), rs.getInt("size"), (Integer) rs.getObject("reference_interval"));
            intervalCache.put(iv.getLapi(), iv);
            fetched.add(iv);
        });
        return fetched;
    }

    public void updateHcnList(List<DbInterval> newIntervals) {

        List<DbInterval> selfreferredIntervals = new ArrayList<>();
        List<DbInterval> referredIntervals = new ArrayList<>();
        Set<Integer> newBodies = new HashSet<>();

        newIntervals.forEach(newInterval -> {
            if (newInterval.getReferenceInterval() != null && newInterval.getReferenceInterval() == newInterval.getLapi()) {
                selfreferredIntervals.add(newInterval);
            } else {
                referredIntervals.add(newInterval);
            }
        });

        if (!selfreferredIntervals.isEmpty()) {
            String rangeClause = selfreferredIntervals.stream()
                    .map(iv -> "(id BETWEEN " + iv.getFirstHcn() + " AND " + (iv.getFirstHcn() + iv.getSize() - 1) + ")")
                    .collect(Collectors.joining(" OR "));
            LinkedList<DbInterval> queue = new LinkedList<>(selfreferredIntervals);
            t().query("SELECT id, body, lapi FROM hcn WHERE " + rangeClause + " ORDER BY id", rs -> {
                long id = rs.getLong("id");
                int bodyId = rs.getInt("body");
                if (!bodyCache.containsKey(bodyId)) newBodies.add(bodyId);
                DbHcn hcn = new DbHcn(id, bodyId, rs.getInt("lapi"));
                while (!queue.isEmpty() && id >= queue.peek().getFirstHcn() + queue.peek().getSize()) {
                    queue.poll();
                }
                if (!queue.isEmpty()) queue.peek().getHcnlist().add(hcn);
            });
        }

        fetchBodies(newBodies);

        selfreferredIntervals.forEach(interval -> interval.getHcnlist().forEach(hcn -> {
            hcn.setDbBody(bodyCache.get(hcn.getBodyId()));
            ScientificNumber[]  valueMultiplier = calculateMultipliers(interval.getHcnlist().get(0), hcn);
            hcn.setValue(valueMultiplier[0].multiply(interval.getValue()));
            hcn.setFactor(valueMultiplier[1].multiply(interval.getFactor()));
        }));
        referredIntervals.forEach(interval -> {
            DbInterval referenceInterval = intervalCache.get(interval.getReferenceInterval());
            int intDiff = interval.getLapi() - referenceInterval.getLapi();
            long idDiff = interval.getFirstHcn() - referenceInterval.getFirstHcn();
            ScientificNumber valueMultiplier = interval.getValue().divide(referenceInterval.getValue());
            ScientificNumber factorMultiplier = interval.getFactor().divide(referenceInterval.getFactor());
            referenceInterval.getHcnlist().forEach(referenceHcn -> {
                DbHcn newHcn = new DbHcn(referenceHcn.getId() + idDiff, referenceHcn.getBodyId(), referenceHcn.getLapi() + intDiff);
                newHcn.setDbBody(referenceHcn.getDbBody());
                newHcn.setValue(referenceHcn.getValue().multiply(valueMultiplier));
                newHcn.setFactor(referenceHcn.getFactor().multiply(factorMultiplier));
                interval.getHcnlist().add(newHcn);
            });
        });
    }

    private ScientificNumber[] calculateMultipliers(DbHcn referenceHcn, DbHcn hcn) {
        ScientificNumber valueMultiplier = new ScientificNumber(1, 0);
        ScientificNumber factorMultiplier = new ScientificNumber(1, 0);

        TreeMap<Integer, Integer> referenceIndexes = referenceHcn.getReferenceIndexes();
        TreeMap<Integer, Integer> hcnIndexes = hcn.getReferenceIndexes();

        Integer currentIndex = 0;
        Integer currentReferenceKey = referenceIndexes.firstKey();
        Integer currentHcnKey = hcnIndexes.firstKey();
        Integer nextReferenceKey = referenceIndexes.higherKey(currentReferenceKey);
        Integer nextHcnKey = hcnIndexes.higherKey(currentHcnKey);

        while (currentIndex != null) {
            if (nextReferenceKey != null && currentIndex.equals(nextReferenceKey)) {
                currentReferenceKey = nextReferenceKey;
                nextReferenceKey = referenceIndexes.higherKey(currentReferenceKey);
            }
            if (nextHcnKey != null && currentIndex.equals(nextHcnKey)) {
                currentHcnKey = nextHcnKey;
                nextHcnKey = hcnIndexes.higherKey(currentHcnKey);
            }

            int referencePower = referenceIndexes.get(currentReferenceKey);
            int hcnPower = hcnIndexes.get(currentHcnKey);
            if (referencePower != hcnPower) {
                valueMultiplier = valueMultiplier.multiply(new ScientificNumber(Math.pow(primeCenter.getPrime(currentIndex).getIntValue(), hcnPower - referencePower), 0));
                factorMultiplier = factorMultiplier.multiply(new ScientificNumber((double) (hcnPower + 1) / (referencePower + 1), 0));
                currentIndex++;
            } else {
                if (nextHcnKey != null) {
                    currentIndex = Math.min(nextReferenceKey, nextHcnKey);
                } else {
                    currentIndex = nextReferenceKey;
                }
            }
        }

        return new ScientificNumber[] {valueMultiplier, factorMultiplier};
    }

    public void fetchBodies(Set<Integer> ids) {
        Set<Integer> missing = ids.stream().filter(id -> !bodyCache.containsKey(id)).collect(Collectors.toSet());
        if (!missing.isEmpty()) {
            String in = missing.stream().map(String::valueOf).collect(Collectors.joining(","));
            t().query("SELECT id, head, tail FROM body WHERE id IN (" + in + ")", rs -> {
                Integer[] headArr = (Integer[]) rs.getArray("head").getArray();
                Integer[] tailArr = (Integer[]) rs.getArray("tail").getArray();
                DbBody body = new DbBody(toIntArray(headArr), toIntArray(tailArr));
                bodyCache.put(rs.getInt("id"), body);
            });
        }
    }

    private int[] toIntArray(Integer[] arr) {
        int[] result = new int[arr.length];
        for (int i = 0; i < arr.length; i++) result[i] = arr[i];
        return result;
    }

    private JdbcTemplate t() {
        return databaseService.createTemplateForDb(dbName);
    }
}
