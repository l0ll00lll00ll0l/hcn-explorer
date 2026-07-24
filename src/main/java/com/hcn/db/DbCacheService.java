package com.hcn.db;

import com.hcn.event.MatrixExtensionActivity;
import com.hcn.newCore.Body;
import com.hcn.newCore.Matrix;
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
    private Matrix matrix;
    private final Map<Integer, DbInterval> intervalCache = new HashMap<>();
    private final Map<Integer, DbBody> bodyCache = new LinkedHashMap<>();
    private final PrimeCenter primeCenter = new PrimeCenter();
    private final List<DbBody> bodyOrder = new ArrayList<>();
    private int activeBodyCount = 0;
    private final List<DbInterval> intervalOrder = new ArrayList<>();
    private int activeIntervalCount = 0;
    private int maxIntervalSize = 0;
    private int maxIntervalActiveBodyCount = 0;

    public void setDbName(String dbName) { this.dbName = dbName; }
    public void setMatrix(Matrix matrix) { this.matrix = matrix; }

    public Map<Integer, DbInterval> getIntervalCache() { return intervalCache; }
    public int getIntervalCacheSize() { return intervalCache.size(); }
    public int getBodyCacheSize() { return bodyCache.size(); }

    public List<Map.Entry<Integer, DbBody>> getLastBodies(int count) {
        List<Map.Entry<Integer, DbBody>> all = new ArrayList<>(bodyCache.entrySet());
        return all.subList(Math.max(0, all.size() - count), all.size());
    }

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
        bodyOrder.clear();
        intervalOrder.clear();
        activeBodyCount = 0;
        activeIntervalCount = 0;
    }

    public Integer getHighestLapi() {
        if (highestLapi == null && dbName != null) {
            highestLapi = t().queryForObject("SELECT MAX(lapi) FROM interval", Integer.class);
            initializeNonDeletedDbBodyData();
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
        t().query("SELECT lapi, value_mantissa, value_exponent, factor_mantissa, factor_exponent, first_hcn, size, reference_interval, active_body_count FROM interval WHERE lapi IN (" + in + ")", rs -> {
            DbInterval iv = new DbInterval(rs.getInt("lapi"), rs.getDouble("value_mantissa"), rs.getLong("value_exponent"),
                    rs.getDouble("factor_mantissa"), rs.getLong("factor_exponent"),
                    rs.getLong("first_hcn"), rs.getInt("size"), (Integer) rs.getObject("reference_interval"), rs.getInt("active_body_count"));
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

        if (!newBodies.isEmpty()) {
            fetchBodies(newBodies);
        }

        selfreferredIntervals.forEach(interval -> interval.getHcnlist().forEach(hcn -> {
            hcn.setDbBody(bodyCache.get(hcn.getBodyId()));
            ScientificNumber[] valueMultiplier = calculateMultipliers(interval.getHcnlist().get(0), hcn);
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

    public void initialBodySubTabData(int width) {
        getHighestLapi();
        int required = width - activeBodyCount;
        if (required > 0) fetchOrderBodies(0, required);
    }

    public synchronized void fetchOrderBodies(int firstIdx, int step) {
        int needed = step - firstIdx;
        if (needed <= 0) return;
        int deletedOffset = bodyOrder.size() - activeBodyCount;
        String sql = "SELECT bl.body_id, bl.first_hcn_lapi, bl.first_superior_hcn_lapi, bl.first_dominated_hcn_lapi, b.head, b.tail " +
                "FROM body_lifecycle bl JOIN body b ON b.id = bl.body_id " +
                "ORDER BY bl.ctid DESC LIMIT ? OFFSET ?";
        List<DbBody> prepend = new ArrayList<>();
        t().query(sql, new Object[]{needed, deletedOffset}, rs -> {
            int id = rs.getInt("body_id");
            Integer[] headArr = (Integer[]) rs.getArray("head").getArray();
            Integer[] tailArr = (Integer[]) rs.getArray("tail").getArray();
            DbBody body = new DbBody(toIntArray(headArr), toIntArray(tailArr));
            body.setFirstHcnLapi((Integer) rs.getObject("first_hcn_lapi"));
            body.setFirstSuperiorHcnLapi((Integer) rs.getObject("first_superior_hcn_lapi"));
            body.setFirstDominatedHcnLapi((Integer) rs.getObject("first_dominated_hcn_lapi"));
            bodyCache.put(id, body);
            prepend.add(body);
        });
        Collections.reverse(prepend);
        bodyOrder.addAll(0, prepend);
    }

    public synchronized List<DbBody> getGraphBodies() { return new ArrayList<>(bodyOrder); }
    public int getActiveBodyCount() { return activeBodyCount; }

    public void initialIntervalSubTabData(int width) {
        getHighestLapi();
        if (intervalOrder.isEmpty() && highestLapi != null) {
            fetchOrderIntervals(highestLapi, width);
        }
        if (maxIntervalSize == 0) {
            Integer ms = t().queryForObject("SELECT MAX(size) FROM interval", Integer.class);
            Integer ma = t().queryForObject("SELECT MAX(active_body_count) FROM interval", Integer.class);
            if (ms != null) maxIntervalSize = ms;
            if (ma != null) maxIntervalActiveBodyCount = ma;
        }
    }

    public synchronized void fetchOrderIntervals(int firstLapi, int step) {
        int fetchFrom = firstLapi - step + 1;
        if (fetchFrom < 1) fetchFrom = 1;
        if (fetchFrom > firstLapi) return;
        Map<Integer, DbInterval> fetched = new LinkedHashMap<>();
        t().query("SELECT lapi, value_mantissa, value_exponent, factor_mantissa, factor_exponent, first_hcn, size, reference_interval, active_body_count FROM interval WHERE lapi BETWEEN ? AND ? ORDER BY lapi ASC",
                new Object[]{fetchFrom, firstLapi}, rs -> {
            DbInterval iv = new DbInterval(rs.getInt("lapi"), rs.getDouble("value_mantissa"), rs.getLong("value_exponent"),
                    rs.getDouble("factor_mantissa"), rs.getLong("factor_exponent"),
                    rs.getLong("first_hcn"), rs.getInt("size"), (Integer) rs.getObject("reference_interval"), rs.getInt("active_body_count"));
            fetched.put(iv.getLapi(), iv);
        });
        t().query("SELECT interval, index, power, created_active_body_count, deleted_active_body_count, deleted_deactivated_body_count, start_nanos, finish_nanos FROM extension_activity WHERE interval BETWEEN ? AND ? ORDER BY id",
                new Object[]{fetchFrom, firstLapi}, rs -> {
            DbInterval iv = fetched.get(rs.getInt("interval"));
            if (iv != null) iv.getExtensions().add(new MatrixExtensionActivity(
                    rs.getInt("index"), rs.getInt("power"), rs.getInt("interval"),
                    rs.getInt("created_active_body_count"), rs.getInt("deleted_active_body_count"),
                    rs.getInt("deleted_deactivated_body_count"), rs.getLong("start_nanos"), rs.getLong("finish_nanos")));
        });
        intervalOrder.addAll(0, fetched.values());
        activeIntervalCount += fetched.size();
    }

    public synchronized List<DbInterval> getGraphIntervals() { return new ArrayList<>(intervalOrder); }
    public int getActiveIntervalCount() { return activeIntervalCount; }
    public int getMaxIntervalSize() { return maxIntervalSize; }
    public int getMaxIntervalActiveBodyCount() { return maxIntervalActiveBodyCount; }

    public void fetchBodies(Set<Integer> ids) {


            String in = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            t().query("SELECT body_id, first_hcn_lapi, first_superior_hcn_lapi, first_dominated_hcn_lapi FROM body_lifecycle WHERE body_id IN (" + in + ") ORDER BY ctid", rs -> {
                int id = rs.getInt("body_id");
                DbBody body = new DbBody(new int[0], new int[0]);
                body.setFirstHcnLapi((Integer) rs.getObject("first_hcn_lapi"));
                body.setFirstSuperiorHcnLapi((Integer) rs.getObject("first_superior_hcn_lapi"));
                body.setFirstDominatedHcnLapi((Integer) rs.getObject("first_dominated_hcn_lapi"));
                bodyCache.put(id, body);
            });

                t().query("SELECT id, head, tail FROM body WHERE id IN (" + in + ")", rs -> {
                    int id = rs.getInt("id");
                    Integer[] headArr = (Integer[]) rs.getArray("head").getArray();
                    Integer[] tailArr = (Integer[]) rs.getArray("tail").getArray();
                    DbBody body = bodyCache.get(id);
                    if (body != null) {
                        body.setHead(toIntArray(headArr));
                        body.setTail(toIntArray(tailArr));
                    } else {
                        bodyCache.put(id, new DbBody(toIntArray(headArr), toIntArray(tailArr)));
                    }
                });

    }

    private void initializeNonDeletedDbBodyData() {
        Body walker = Matrix.lastTransition.getBodyList().getSmallestBody();
        while (walker != null) {
            DbBody dbBody = walker.getDbBody();
            dbBody.setFirstHcnLapi(walker.getFirstHcn() != null ? walker.getFirstHcn().getLapi() : null);
            dbBody.setFirstSuperiorHcnLapi(walker.getFirstSuperiorHcn() != null ? walker.getFirstSuperiorHcn().getLapi() : null);
            dbBody.setFirstDominatedHcnLapi(walker.getFirstDominatedHcn() != null ? walker.getFirstDominatedHcn().getLapi() : null);
            bodyOrder.add(dbBody);
            activeBodyCount++;
            if (walker.getDbId() != null) bodyCache.put(walker.getDbId(), dbBody);
            walker = walker.getLargerBody();
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
