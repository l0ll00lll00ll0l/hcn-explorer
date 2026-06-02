package com.hcn.db;

import com.hcn.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class MatrixSaveService {

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private SaveProgress progress;

    private Map<ActivePrimeIndex, Long> savedActivePrimeIndexes;
    private Map<PrimeIndexPower, Long> savedPips;
    private Map<HcnBody, Long> savedBodies;
    private Map<Hcn, Long> savedHcns;
    private Map<FixedPowerGroup, Long> savedFpgs;
    private Map<LastActivePrimeIndexGroup, Long> savedLapiGroups;

    public void save(Object matrix, String dbName) {
        if (dbName == null || !(matrix instanceof Matrix)) return;

        Matrix m = (Matrix) matrix;

        savedActivePrimeIndexes = new IdentityHashMap<>();
        savedPips = new IdentityHashMap<>();
        savedBodies = new IdentityHashMap<>();
        savedHcns = new IdentityHashMap<>();
        savedFpgs = new IdentityHashMap<>();
        savedLapiGroups = new IdentityHashMap<>();

        try (Connection conn = databaseService.getConnection(dbName)) {
            conn.setAutoCommit(false);
            progress.start();

            progress.startPhase(1, "Clearing old data", 1);
            clearAll(conn);
            progress.increment();

            saveAll(conn, m);

            progress.startPhase(10, "Committing to disk", 1);
            conn.commit();
            progress.increment();
            progress.done();
        } catch (Exception e) {
            progress.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveAll(Connection conn, Matrix m) throws SQLException {
        List<ActivePrimeIndex> allApis = collectAllActivePrimeIndexes(m);
        progress.startPhase(2, "ActivePrimeIndexes", allApis.size());
        saveActivePrimeIndexesBatch(conn, allApis);

        progress.startPhase(3, "PrimeIndexPowers", 0);
        savePrimeIndexPowersBatch(conn);

        List<HcnBody> allBodies = collectAllBodies(m);
        progress.startPhase(4, "HcnBodies", allBodies.size());
        saveHcnBodiesBatch(conn, allBodies);

        progress.startPhase(5, "FixedPowerGroups", 0);
        saveFixedPowerGroups(conn);

        progress.startPhase(6, "HCNs", 0);
        saveHcnsBatch(conn, m);

        progress.startPhase(7, "LapiGroups", 0);
        saveLapiGroups(conn, m);

        progress.startPhase(8, "Back-fill references", 0);
        backfillReferencesBatch(conn, m);

        progress.startPhase(9, "Matrix", 1);
        saveMatrix(conn, m);
        progress.increment();
    }

    private void clearAll(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE temp_lapi_hcn_list, temp_matrix, temp_last_active_prime_index_group, temp_hcn, temp_hcn_body, temp_prime_index_power, temp_fixed_power_group, temp_active_prime_index");
        }
    }

    // --- Phase 2: ActivePrimeIndexes (batch) ---
    private void saveActivePrimeIndexesBatch(Connection conn, List<ActivePrimeIndex> allApis) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO temp_active_prime_index (prime_index) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            for (ActivePrimeIndex api : allApis) {
                ps.setInt(1, api.getIndex());
                ps.addBatch();
            }
            ps.executeBatch();
            ResultSet keys = ps.getGeneratedKeys();
            for (ActivePrimeIndex api : allApis) {
                keys.next();
                savedActivePrimeIndexes.put(api, keys.getLong(1));
            }
        }
    }

    // --- Phase 3: PrimeIndexPowers (batch) ---
    private void savePrimeIndexPowersBatch(Connection conn) throws SQLException {
        List<PrimeIndexPower> allPips = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO temp_prime_index_power (prime_index_id, power, proved) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            for (Map.Entry<ActivePrimeIndex, Long> entry : savedActivePrimeIndexes.entrySet()) {
                ActivePrimeIndex api = entry.getKey();
                long apiId = entry.getValue();
                for (PrimeIndexPower pip : api.getPips().values()) {
                    ps.setLong(1, apiId);
                    ps.setInt(2, pip.getPower());
                    ps.setBoolean(3, pip.isProved());
                    ps.addBatch();
                    allPips.add(pip);
                }
            }
            ps.executeBatch();
            ResultSet keys = ps.getGeneratedKeys();
            for (PrimeIndexPower pip : allPips) {
                keys.next();
                savedPips.put(pip, keys.getLong(1));
            }
        }
    }

    // --- Phase 4: HcnBodies (batch) ---
    private void saveHcnBodiesBatch(Connection conn, List<HcnBody> allBodies) throws SQLException {
        List<HcnBody> ordered = topologicalSortBodies(allBodies);

        // Batch insert bodies (without parent_id - will backfill after)
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO temp_hcn_body (pip_id, proved, value_mantissa, value_exponent, factor_mantissa, factor_exponent) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            for (HcnBody body : ordered) {
                Long pipId = savedPips.get(body.getPip());
                setNullableLong(ps, 1, pipId);
                ps.setBoolean(2, body.isProved());
                if (body.getValue() != null) {
                    ps.setDouble(3, body.getValue().getMantissa());
                    ps.setLong(4, body.getValue().getExponent());
                } else {
                    ps.setNull(3, Types.DOUBLE);
                    ps.setNull(4, Types.BIGINT);
                }
                if (body.getFactor() != null) {
                    ps.setDouble(5, body.getFactor().getMantissa());
                    ps.setLong(6, body.getFactor().getExponent());
                } else {
                    ps.setNull(5, Types.DOUBLE);
                    ps.setNull(6, Types.BIGINT);
                }
                ps.addBatch();
            }
            ps.executeBatch();
            ResultSet keys = ps.getGeneratedKeys();
            for (HcnBody body : ordered) {
                keys.next();
                savedBodies.put(body, keys.getLong(1));
            }
        }

        // Batch update parent_id, smaller_body_id, larger_body_id
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE temp_hcn_body SET parent_id = ?, smaller_body_id = ?, larger_body_id = ? WHERE id = ?")) {
            int count = 0;
            for (HcnBody body : ordered) {
                Long parentId = body.getParent() != null ? savedBodies.get(body.getParent()) : null;
                Long smallerId = body.getSmallerBody() != null ? savedBodies.get(body.getSmallerBody()) : null;
                Long largerId = body.getLargerBody() != null ? savedBodies.get(body.getLargerBody()) : null;
                if (parentId != null || smallerId != null || largerId != null) {
                    setNullableLong(ps, 1, parentId);
                    setNullableLong(ps, 2, smallerId);
                    setNullableLong(ps, 3, largerId);
                    ps.setLong(4, savedBodies.get(body));
                    ps.addBatch();
                    count++;
                }
            }
            if (count > 0) ps.executeBatch();
        }
    }

    // --- Phase 5: FixedPowerGroups ---
    private void saveFixedPowerGroups(Connection conn) throws SQLException {
        List<FixedPowerGroup> allFpgs = new ArrayList<>();
        for (ActivePrimeIndex api : savedActivePrimeIndexes.keySet()) {
            if (api.getOffspringFixedPowerGroup() != null && !allFpgs.contains(api.getOffspringFixedPowerGroup())) {
                allFpgs.add(api.getOffspringFixedPowerGroup());
            }
        }
        if (allFpgs.isEmpty()) return;

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO temp_fixed_power_group (value_mantissa, value_exponent, factor_mantissa, factor_exponent, parent_prime_index_id, offspring_prime_index_id) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            for (FixedPowerGroup fpg : allFpgs) {
                ps.setDouble(1, fpg.getValue().getMantissa());
                ps.setLong(2, fpg.getValue().getExponent());
                ps.setDouble(3, fpg.getFactor().getMantissa());
                ps.setLong(4, fpg.getFactor().getExponent());
                setNullableLong(ps, 5, savedActivePrimeIndexes.get(fpg.getParentPrimeIndex()));
                setNullableLong(ps, 6, savedActivePrimeIndexes.get(fpg.getOffspringPrimeIndex()));
                ps.addBatch();
            }
            ps.executeBatch();
            ResultSet keys = ps.getGeneratedKeys();
            for (FixedPowerGroup fpg : allFpgs) {
                keys.next();
                savedFpgs.put(fpg, keys.getLong(1));
            }
        }

        // Batch update FPG members
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE temp_active_prime_index SET member_of_fixed_power_group_id = ?, fixed_power_group_order = ? WHERE id = ?")) {
            for (FixedPowerGroup fpg : allFpgs) {
                int order = 0;
                for (ActivePrimeIndex member : fpg.getFixedPowerGroup()) {
                    Long memberId = savedActivePrimeIndexes.get(member);
                    if (memberId != null) {
                        ps.setLong(1, savedFpgs.get(fpg));
                        ps.setInt(2, order++);
                        ps.setLong(3, memberId);
                        ps.addBatch();
                    }
                }
            }
            ps.executeBatch();
        }
    }

    // --- Phase 6: HCNs (batch) ---
    private void saveHcnsBatch(Connection conn, Matrix m) throws SQLException {
        Set<Hcn> allHcns = new LinkedHashSet<>();
        LastActivePrimeIndexGroup lapi = m.getLowestLapiGroup();
        while (lapi != null) {
            allHcns.addAll(lapi.getHcnList());
            lapi = lapi.getHigherLapiGroup();
        }
        if (m.getNextLapiGroup() != null) {
            allHcns.addAll(m.getNextLapiGroup().getHcnList());
        }
        for (HcnBody body : savedBodies.keySet()) {
            if (body.getLastGeneratedHcn() != null) {
                allHcns.add(body.getLastGeneratedHcn());
            }
        }

        List<Hcn> hcnList = new ArrayList<>(allHcns);

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO temp_hcn (body_id, last_active_prime, value_mantissa, value_exponent, factor_mantissa, factor_exponent) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            for (Hcn hcn : hcnList) {
                Long bodyId = hcn.getBody() != null ? savedBodies.get(hcn.getBody()) : null;
                setNullableLong(ps, 1, bodyId);
                ps.setInt(2, hcn.getLastActivePrime());
                if (hcn.getValue() != null) {
                    ps.setDouble(3, hcn.getValue().getMantissa());
                    ps.setLong(4, hcn.getValue().getExponent());
                } else {
                    ps.setNull(3, Types.DOUBLE);
                    ps.setNull(4, Types.BIGINT);
                }
                if (hcn.getFactor() != null) {
                    ps.setDouble(5, hcn.getFactor().getMantissa());
                    ps.setLong(6, hcn.getFactor().getExponent());
                } else {
                    ps.setNull(5, Types.DOUBLE);
                    ps.setNull(6, Types.BIGINT);
                }
                ps.addBatch();
            }
            ps.executeBatch();
            ResultSet keys = ps.getGeneratedKeys();
            for (Hcn hcn : hcnList) {
                keys.next();
                savedHcns.put(hcn, keys.getLong(1));
            }
        }
    }

    // --- Phase 7: LapiGroups ---
    private void saveLapiGroups(Connection conn, Matrix m) throws SQLException {
        List<LastActivePrimeIndexGroup> allLapis = new ArrayList<>();
        LastActivePrimeIndexGroup lapi = m.getLowestLapiGroup();
        while (lapi != null) {
            allLapis.add(lapi);
            lapi = lapi.getHigherLapiGroup();
        }
        if (m.getNextLapiGroup() != null && !allLapis.contains(m.getNextLapiGroup())) {
            allLapis.add(m.getNextLapiGroup());
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO temp_last_active_prime_index_group (last_active_prime_index, walker_body_id) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            for (LastActivePrimeIndexGroup g : allLapis) {
                Long walkerBodyId = g.getWalkerBody() != null ? savedBodies.get(g.getWalkerBody()) : null;
                ps.setInt(1, g.getLastActivePrimeIndex());
                setNullableLong(ps, 2, walkerBodyId);
                ps.addBatch();
            }
            ps.executeBatch();
            ResultSet keys = ps.getGeneratedKeys();
            for (LastActivePrimeIndexGroup g : allLapis) {
                keys.next();
                savedLapiGroups.put(g, keys.getLong(1));
            }
        }

        // Batch update lower/higher links
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE temp_last_active_prime_index_group SET lower_lapi_group_id = ?, higher_lapi_group_id = ? WHERE id = ?")) {
            for (LastActivePrimeIndexGroup g : allLapis) {
                Long lowerId = g.getLowerLapiGroup() != null ? savedLapiGroups.get(g.getLowerLapiGroup()) : null;
                Long higherId = g.getHigherLapiGroup() != null ? savedLapiGroups.get(g.getHigherLapiGroup()) : null;
                setNullableLong(ps, 1, lowerId);
                setNullableLong(ps, 2, higherId);
                ps.setLong(3, savedLapiGroups.get(g));
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // Batch insert hcn lists
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO temp_lapi_hcn_list (lapi_group_id, hcn_id, order_in_list) VALUES (?, ?, ?)")) {
            for (LastActivePrimeIndexGroup g : allLapis) {
                int order = 0;
                for (Hcn hcn : g.getHcnList()) {
                    Long hcnId = savedHcns.get(hcn);
                    if (hcnId != null) {
                        ps.setLong(1, savedLapiGroups.get(g));
                        ps.setLong(2, hcnId);
                        ps.setInt(3, order++);
                        ps.addBatch();
                    }
                }
            }
            ps.executeBatch();
        }
    }

    // --- Phase 8: Back-fill (batch) ---
    private void backfillReferencesBatch(Connection conn, Matrix m) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE temp_active_prime_index SET next_active_prime_index_id = ?, parent_active_prime_index_id = ?, offspring_fixed_power_group_id = ?, parent_fixed_power_group_id = ?, smallest_body_id = ? WHERE id = ?")) {
            for (Map.Entry<ActivePrimeIndex, Long> entry : savedActivePrimeIndexes.entrySet()) {
                ActivePrimeIndex current = entry.getKey();
                setNullableLong(ps, 1, current.getNextActivePrimeIndex() != null ? savedActivePrimeIndexes.get(current.getNextActivePrimeIndex()) : null);
                setNullableLong(ps, 2, current.getParentActivePrimeIndex() != null ? savedActivePrimeIndexes.get(current.getParentActivePrimeIndex()) : null);
                setNullableLong(ps, 3, current.getOffspringFixedPowerGroup() != null ? savedFpgs.get(current.getOffspringFixedPowerGroup()) : null);
                setNullableLong(ps, 4, current.getParentFixedPowerGroup() != null ? savedFpgs.get(current.getParentFixedPowerGroup()) : null);
                setNullableLong(ps, 5, current.getHcnBodyList().getSmallestBody() != null ? savedBodies.get(current.getHcnBodyList().getSmallestBody()) : null);
                ps.setLong(6, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // Batch update HcnBody lastGeneratedHcn
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE temp_hcn_body SET last_generated_hcn_id = ? WHERE id = ?")) {
            int count = 0;
            for (Map.Entry<HcnBody, Long> entry : savedBodies.entrySet()) {
                HcnBody body = entry.getKey();
                if (body.getLastGeneratedHcn() != null) {
                    Long hcnId = savedHcns.get(body.getLastGeneratedHcn());
                    if (hcnId != null) {
                        ps.setLong(1, hcnId);
                        ps.setLong(2, entry.getValue());
                        ps.addBatch();
                        count++;
                    }
                }
            }
            if (count > 0) ps.executeBatch();
        }
    }

    // --- Phase 9: Matrix ---
    private void saveMatrix(Connection conn, Matrix m) throws SQLException {
        Long lastApiId = savedActivePrimeIndexes.get(m.getLastActivePrimeIndex());
        Long lowestLapiId = savedLapiGroups.get(m.getLowestLapiGroup());
        Long highestLapiId = savedLapiGroups.get(m.getHighestLapiGroup());
        Long nextLapiId = m.getNextLapiGroup() != null ? savedLapiGroups.get(m.getNextLapiGroup()) : null;

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO temp_matrix (last_active_prime_index_id, lowest_lapi_group_id, highest_lapi_group_id, next_lapi_group_id, proved_limit_mantissa, proved_limit_exponent, proved_count, last_proved_prime_index) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            setNullableLong(ps, 1, lastApiId);
            setNullableLong(ps, 2, lowestLapiId);
            setNullableLong(ps, 3, highestLapiId);
            setNullableLong(ps, 4, nextLapiId);
            if (m.getProvedLimit() != null) {
                ps.setDouble(5, m.getProvedLimit().getMantissa());
                ps.setLong(6, m.getProvedLimit().getExponent());
            } else {
                ps.setNull(5, Types.DOUBLE);
                ps.setNull(6, Types.BIGINT);
            }
            ps.setInt(7, m.getProvedCount());
            ps.setInt(8, m.getLastProvedPrimeIndex());
            ps.executeUpdate();
        }
    }

    // --- Helpers ---
    private ActivePrimeIndex findFirstActivePrimeIndex(Matrix m) {
        ActivePrimeIndex current = m.getLastActivePrimeIndex();
        while (true) {
            if (current.getParentActivePrimeIndex() != null) {
                current = current.getParentActivePrimeIndex();
            } else if (current.getParentFixedPowerGroup() != null) {
                current = current.getParentFixedPowerGroup().getParentPrimeIndex();
            } else {
                return current;
            }
        }
    }

    private List<ActivePrimeIndex> collectAllActivePrimeIndexes(Matrix m) {
        List<ActivePrimeIndex> all = new ArrayList<>();
        collectApiChain(findFirstActivePrimeIndex(m), all);
        return all;
    }

    private void collectApiChain(ActivePrimeIndex api, List<ActivePrimeIndex> all) {
        if (api == null || all.contains(api)) return;
        all.add(api);
        if (api.getOffspringFixedPowerGroup() != null) {
            for (ActivePrimeIndex member : api.getOffspringFixedPowerGroup().getFixedPowerGroup()) {
                all.add(member);
            }
            collectApiChain(api.getOffspringFixedPowerGroup().getOffspringPrimeIndex(), all);
        } else if (api.getNextActivePrimeIndex() != null) {
            collectApiChain(api.getNextActivePrimeIndex(), all);
        }
    }

    private List<HcnBody> collectAllBodies(Matrix m) {
        Set<HcnBody> all = new LinkedHashSet<>();
        for (ActivePrimeIndex api : savedActivePrimeIndexes.keySet()) {
            for (PrimeIndexPower pip : api.getPips().values()) {
                all.addAll(pip.getActiveHcnBodies());
            }
        }
        return new ArrayList<>(all);
    }

    private List<HcnBody> topologicalSortBodies(List<HcnBody> bodies) {
        Set<HcnBody> bodySet = new LinkedHashSet<>(bodies);
        List<HcnBody> sorted = new ArrayList<>();
        Set<HcnBody> visited = new HashSet<>();
        for (HcnBody body : bodies) {
            topoVisit(body, bodySet, visited, sorted);
        }
        return sorted;
    }

    private void topoVisit(HcnBody body, Set<HcnBody> bodySet, Set<HcnBody> visited, List<HcnBody> sorted) {
        if (!visited.add(body)) return;
        if (body.getParent() != null && bodySet.contains(body.getParent())) {
            topoVisit(body.getParent(), bodySet, visited, sorted);
        }
        sorted.add(body);
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }
}
