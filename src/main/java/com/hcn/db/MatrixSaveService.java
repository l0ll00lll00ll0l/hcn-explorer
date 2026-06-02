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

    private long nextPipId;
    private long nextBodyId;
    private long nextHcnId;
    private long nextFpgId;

    private long assignId(PrimeIndexPower obj) { if (obj.getTempId() == null) obj.setTempId(nextPipId++); return obj.getTempId(); }
    private long assignId(HcnBody obj) { if (obj.getTempId() == null) obj.setTempId(nextBodyId++); return obj.getTempId(); }
    private long assignId(Hcn obj) { if (obj.getTempId() == null) obj.setTempId(nextHcnId++); return obj.getTempId(); }
    private long assignId(FixedPowerGroup obj) { if (obj.getTempId() == null) obj.setTempId(nextFpgId++); return obj.getTempId(); }

    public void save(Object matrix, String dbName) {
        if (dbName == null || !(matrix instanceof Matrix)) return;

        Matrix m = (Matrix) matrix;
        nextPipId = 1;
        nextBodyId = 1;
        nextHcnId = 1;
        nextFpgId = 1;

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
        savePrimeIndexPowersBatch(conn, allApis);

        List<HcnBody> allBodies = collectAllBodies(m);
        progress.startPhase(4, "HcnBodies", allBodies.size());
        saveHcnBodiesBatch(conn, allBodies);

        progress.startPhase(5, "FixedPowerGroups", 0);
        saveFixedPowerGroups(conn);

        progress.startPhase(6, "HCNs", 0);
        saveHcnsBatch(conn, m);

        progress.startPhase(7, "LapiGroups", 0);
        saveLapiGroups(conn, m);

        progress.startPhase(8, "Back-fill API refs", 0);
        backfillApiRefs(conn);

        progress.startPhase(9, "Matrix", 1);
        saveMatrix(conn, m);
        progress.increment();
    }

    private void clearAll(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE temp_lapi_hcn_list, temp_matrix, temp_last_active_prime_index_group, temp_hcn, temp_hcn_body, temp_prime_index_power, temp_fixed_power_group, temp_active_prime_index");
        }
    }

    // --- ActivePrimeIndexes ---
    private void saveActivePrimeIndexesBatch(Connection conn, List<ActivePrimeIndex> allApis) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO temp_active_prime_index (id, prime_index) VALUES (?, ?)")) {
            for (ActivePrimeIndex api : allApis) {
                ps.setLong(1, api.getIndex());
                ps.setInt(2, api.getIndex());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // --- PrimeIndexPowers ---
    private void savePrimeIndexPowersBatch(Connection conn, List<ActivePrimeIndex> allApis) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO temp_prime_index_power (id, prime_index_id, power, proved) VALUES (?, ?, ?, ?)")) {
            for (ActivePrimeIndex api : allApis) {
                for (PrimeIndexPower pip : api.getPips().values()) {
                    ps.setLong(1, assignId(pip));
                    ps.setLong(2, api.getIndex());
                    ps.setInt(3, pip.getPower());
                    ps.setBoolean(4, pip.isProved());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    // --- HcnBodies ---
    private void saveHcnBodiesBatch(Connection conn, List<HcnBody> allBodies) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO temp_hcn_body (id, parent_id, pip_id, proved, value_mantissa, value_exponent, factor_mantissa, factor_exponent, smaller_body_id, larger_body_id, last_generated_hcn_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (HcnBody body : allBodies) {
                ps.setLong(1, assignId(body));
                setNullableId(ps, 2, body.getParent());
                setNullableLong(ps, 3, body.getPip() != null ? assignId(body.getPip()) : null);
                ps.setBoolean(4, body.isProved());
                setScientificNumber(ps, 5, body.getValue());
                setScientificNumber(ps, 7, body.getFactor());
                setNullableId(ps, 9, body.getSmallerBody());
                setNullableId(ps, 10, body.getLargerBody());
                if (body.getLastGeneratedHcn() != null) {
                    ps.setLong(11, assignId(body.getLastGeneratedHcn()));
                } else {
                    ps.setNull(11, Types.BIGINT);
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // --- FixedPowerGroups ---
    private void saveFixedPowerGroups(Connection conn) throws SQLException {
        // Collect all FPGs
        Set<FixedPowerGroup> allFpgs = new LinkedHashSet<>();
        for (Map.Entry<ActivePrimeIndex, Long> entry : getAllSavedApis()) {
            if (entry.getKey().getOffspringFixedPowerGroup() != null) {
                allFpgs.add(entry.getKey().getOffspringFixedPowerGroup());
            }
        }
        if (allFpgs.isEmpty()) return;

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO temp_fixed_power_group (id, value_mantissa, value_exponent, factor_mantissa, factor_exponent, parent_prime_index_id, offspring_prime_index_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            for (FixedPowerGroup fpg : allFpgs) {
                ps.setLong(1, assignId(fpg));
                setScientificNumber(ps, 2, fpg.getValue());
                setScientificNumber(ps, 4, fpg.getFactor());
                setNullableLong(ps, 6, fpg.getParentPrimeIndex() != null ? (long) fpg.getParentPrimeIndex().getIndex() : null);
                setNullableLong(ps, 7, fpg.getOffspringPrimeIndex() != null ? (long) fpg.getOffspringPrimeIndex().getIndex() : null);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // Update FPG members on active_prime_index
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE temp_active_prime_index SET member_of_fixed_power_group_id = ?, fixed_power_group_order = ? WHERE id = ?")) {
            for (FixedPowerGroup fpg : allFpgs) {
                int order = 0;
                for (ActivePrimeIndex member : fpg.getFixedPowerGroup()) {
                    ps.setLong(1, fpg.getTempId());
                    ps.setInt(2, order++);
                    ps.setLong(3, member.getIndex());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    // --- HCNs ---
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
        for (HcnBody body : collectAllBodiesFromPips()) {
            if (body.getLastGeneratedHcn() != null) {
                allHcns.add(body.getLastGeneratedHcn());
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO temp_hcn (id, body_id, last_active_prime, value_mantissa, value_exponent, factor_mantissa, factor_exponent) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            for (Hcn hcn : allHcns) {
                ps.setLong(1, assignId(hcn));
                setNullableLong(ps, 2, hcn.getBody() != null ? assignId(hcn.getBody()) : null);
                ps.setInt(3, hcn.getLastActivePrime());
                setScientificNumber(ps, 4, hcn.getValue());
                setScientificNumber(ps, 6, hcn.getFactor());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // --- LapiGroups ---
    private void saveLapiGroups(Connection conn, Matrix m) throws SQLException {
        // Save chain lapi groups (lowest to highest)
        List<LastActivePrimeIndexGroup> chainLapis = new ArrayList<>();
        LastActivePrimeIndexGroup lapi = m.getLowestLapiGroup();
        while (lapi != null) {
            chainLapis.add(lapi);
            lapi = lapi.getHigherLapiGroup();
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO temp_last_active_prime_index_group (id, last_active_prime_index, walker_body_id, lower_lapi_group_id, higher_lapi_group_id) VALUES (?, ?, ?, ?, ?)")) {
            for (LastActivePrimeIndexGroup g : chainLapis) {
                ps.setLong(1, g.getLastActivePrimeIndex());
                ps.setInt(2, g.getLastActivePrimeIndex());
                setNullableLong(ps, 3, g.getWalkerBody() != null ? assignId(g.getWalkerBody()) : null);
                setNullableLong(ps, 4, g.getLowerLapiGroup() != null ? (long) g.getLowerLapiGroup().getLastActivePrimeIndex() : null);
                setNullableLong(ps, 5, g.getHigherLapiGroup() != null ? (long) g.getHigherLapiGroup().getLastActivePrimeIndex() : null);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // Save nextLapiGroup separately
        if (m.getNextLapiGroup() != null && !chainLapis.contains(m.getNextLapiGroup())) {
            LastActivePrimeIndexGroup next = m.getNextLapiGroup();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO temp_last_active_prime_index_group (id, last_active_prime_index, walker_body_id, lower_lapi_group_id, higher_lapi_group_id) VALUES (?, ?, ?, ?, ?)")) {
                ps.setLong(1, next.getLastActivePrimeIndex());
                ps.setInt(2, next.getLastActivePrimeIndex());
                setNullableLong(ps, 3, next.getWalkerBody() != null ? assignId(next.getWalkerBody()) : null);
                // nextLapiGroup's lower is always highestLapiGroup (not wired yet at save time)
                setNullableLong(ps, 4, (long) m.getHighestLapiGroup().getLastActivePrimeIndex());
                ps.setNull(5, Types.BIGINT);
                ps.executeUpdate();
            }
            chainLapis.add(next);
        }

        // HCN lists for all lapi groups
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO temp_lapi_hcn_list (lapi_group_id, hcn_id, order_in_list) VALUES (?, ?, ?)")) {
            for (LastActivePrimeIndexGroup g : chainLapis) {
                int order = 0;
                for (Hcn hcn : g.getHcnList()) {
                    ps.setLong(1, g.getLastActivePrimeIndex());
                    ps.setLong(2, assignId(hcn));
                    ps.setInt(3, order++);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    // --- Back-fill API references (need FPG ids which are assigned in phase 5) ---
    private void backfillApiRefs(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE temp_active_prime_index SET next_active_prime_index_id = ?, parent_active_prime_index_id = ?, offspring_fixed_power_group_id = ?, parent_fixed_power_group_id = ?, smallest_body_id = ? WHERE id = ?")) {
            for (Map.Entry<ActivePrimeIndex, Long> entry : getAllSavedApis()) {
                ActivePrimeIndex api = entry.getKey();
                setNullableLong(ps, 1, api.getNextActivePrimeIndex() != null ? (long) api.getNextActivePrimeIndex().getIndex() : null);
                setNullableLong(ps, 2, api.getParentActivePrimeIndex() != null ? (long) api.getParentActivePrimeIndex().getIndex() : null);
                setNullableLong(ps, 3, api.getOffspringFixedPowerGroup() != null ? assignId(api.getOffspringFixedPowerGroup()) : null);
                setNullableLong(ps, 4, api.getParentFixedPowerGroup() != null ? assignId(api.getParentFixedPowerGroup()) : null);
                setNullableLong(ps, 5, api.getHcnBodyList().getSmallestBody() != null ? assignId(api.getHcnBodyList().getSmallestBody()) : null);
                ps.setLong(6, api.getIndex());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // --- Matrix ---
    private void saveMatrix(Connection conn, Matrix m) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO temp_matrix (id, last_active_prime_index_id, lowest_lapi_group_id, highest_lapi_group_id, next_lapi_group_id, proved_limit_mantissa, proved_limit_exponent, proved_count, last_proved_prime_index, lowest_proved_lapi_within_interval, basic_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setLong(1, 1);
            setNullableLong(ps, 2, m.getLastActivePrimeIndex() != null ? (long) m.getLastActivePrimeIndex().getIndex() : null);
            setNullableLong(ps, 3, m.getLowestLapiGroup() != null ? (long) m.getLowestLapiGroup().getLastActivePrimeIndex() : null);
            setNullableLong(ps, 4, m.getHighestLapiGroup() != null ? (long) m.getHighestLapiGroup().getLastActivePrimeIndex() : null);
            setNullableLong(ps, 5, m.getNextLapiGroup() != null ? (long) m.getNextLapiGroup().getLastActivePrimeIndex() : null);
            setScientificNumber(ps, 6, m.getProvedLimit());
            ps.setInt(8, m.getProvedCount());
            ps.setInt(9, m.getLastProvedPrimeIndex());
            ps.setInt(10, m.getLowestProvedLapiWithinInterval());
            ps.setBoolean(11, m.isBasicData());
            ps.executeUpdate();
        }
    }

    // --- Helpers ---
    private Set<Map.Entry<ActivePrimeIndex, Long>> getAllSavedApis() {
        // Return all APIs that have tempId assigned
        Set<Map.Entry<ActivePrimeIndex, Long>> result = new LinkedHashSet<>();
        // We don't keep a map anymore - iterate would need stored list
        // Actually let's keep a reference
        return savedApiEntries;
    }

    private Set<Map.Entry<ActivePrimeIndex, Long>> savedApiEntries = new LinkedHashSet<>();

    private List<ActivePrimeIndex> collectAllActivePrimeIndexes(Matrix m) {
        List<ActivePrimeIndex> all = new ArrayList<>();
        collectApiChain(findFirstActivePrimeIndex(m), all);
        savedApiEntries.clear();
        for (ActivePrimeIndex api : all) {
            savedApiEntries.add(Map.entry(api, (long) api.getIndex()));
        }
        return all;
    }

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
        for (Map.Entry<ActivePrimeIndex, Long> entry : savedApiEntries) {
            for (PrimeIndexPower pip : entry.getKey().getPips().values()) {
                all.addAll(pip.getActiveHcnBodies());
            }
        }
        return new ArrayList<>(all);
    }

    private Set<HcnBody> collectAllBodiesFromPips() {
        Set<HcnBody> all = new LinkedHashSet<>();
        for (Map.Entry<ActivePrimeIndex, Long> entry : savedApiEntries) {
            for (PrimeIndexPower pip : entry.getKey().getPips().values()) {
                all.addAll(pip.getActiveHcnBodies());
            }
        }
        return all;
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) ps.setNull(index, Types.BIGINT);
        else ps.setLong(index, value);
    }

    private void setNullableId(PreparedStatement ps, int index, HcnBody body) throws SQLException {
        if (body == null) ps.setNull(index, Types.BIGINT);
        else ps.setLong(index, assignId(body));
    }

    private void setScientificNumber(PreparedStatement ps, int startIndex, ScientificNumber sn) throws SQLException {
        if (sn != null) {
            ps.setDouble(startIndex, sn.getMantissa());
            ps.setLong(startIndex + 1, sn.getExponent());
        } else {
            ps.setNull(startIndex, Types.DOUBLE);
            ps.setNull(startIndex + 1, Types.BIGINT);
        }
    }
}
