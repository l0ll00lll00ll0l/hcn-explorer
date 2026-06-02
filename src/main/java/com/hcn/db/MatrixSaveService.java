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

    private Map<Object, Long> savedScientificNumbers;
    private Map<ActivePrimeIndex, Long> savedActivePrimeIndexes;
    private Map<PrimeIndexPower, Long> savedPips;
    private Map<HcnBody, Long> savedBodies;
    private Map<Hcn, Long> savedHcns;
    private Map<FixedPowerGroup, Long> savedFpgs;
    private Map<LastActivePrimeIndexGroup, Long> savedLapiGroups;

    public void save(Object matrix, String dbName, String mode) {
        if (dbName == null || !(matrix instanceof Matrix)) return;

        Matrix m = (Matrix) matrix;

        savedScientificNumbers = new IdentityHashMap<>();
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

            saveAll(conn, m, mode);

            progress.startPhase(10, "Committing to disk", 1);
            conn.commit();
            progress.increment();
            progress.done();
        } catch (Exception e) {
            progress.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveAll(Connection conn, Matrix m, String mode) throws SQLException {
        List<ActivePrimeIndex> allApis = collectAllActivePrimeIndexes(m);
        progress.startPhase(2, "ActivePrimeIndexes", allApis.size());
        saveActivePrimeIndexesBatch(conn, allApis);

        // Collect bodies early so we can find extra PIPs from deactivated bodies
        List<HcnBody> allBodies = collectAllBodies(m);

        int pipCount = allApis.stream().mapToInt(a -> a.getPips().size()).sum();
        progress.startPhase(3, "PrimeIndexPowers", pipCount);
        savePrimeIndexPowersBatch(conn, allBodies);

        progress.startPhase(4, "HcnBodies", allBodies.size());
        saveHcnBodiesBatch(conn, allBodies);

        progress.startPhase(5, "FixedPowerGroups", savedActivePrimeIndexes.size());
        saveFixedPowerGroups(conn, m);

        progress.startPhase(6, "HCNs", 0);
        saveHcnsBatch(conn, m);
        saveReferenceIntervalHcns(conn, m);

        progress.startPhase(7, "LapiGroups", 0);
        saveLapiGroups(conn, m);

        progress.startPhase(9, "Back-fill references", savedActivePrimeIndexes.size() + savedBodies.size());
        backfillReferencesBatch(conn, m);

        progress.startPhase(9, "Matrix", 1);
        saveMatrix(conn, m, mode);
        progress.increment();
    }

    private void clearAll(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE reference_interval_hcn, lapi_hcn_list, matrix, last_active_prime_index_group, hcn, hcn_body, prime_index_power, fixed_power_group, active_prime_index, scientific_number");
        }
    }

    // --- Phase 1: ScientificNumbers (batch) ---
    private void saveScientificNumbersBatch(Connection conn, List<ScientificNumber> sns) throws SQLException {
        if (sns.isEmpty()) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO scientific_number (mantissa, exponent) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            for (ScientificNumber sn : sns) {
                if (sn == null || savedScientificNumbers.containsKey(sn)) continue;
                ps.setDouble(1, sn.getMantissa());
                ps.setLong(2, sn.getExponent());
                ps.addBatch();
            }
            ps.executeBatch();
            ResultSet keys = ps.getGeneratedKeys();
            for (ScientificNumber sn : sns) {
                if (sn == null || savedScientificNumbers.containsKey(sn)) continue;
                keys.next();
                savedScientificNumbers.put(sn, keys.getLong(1));
            }
        }
    }

    private long getOrSaveScientificNumber(ScientificNumber sn) {
        if (sn == null) return -1;
        Long existing = savedScientificNumbers.get(sn);
        return existing != null ? existing : -1;
    }

    private void ensureScientificNumbersSaved(Connection conn, Collection<ScientificNumber> sns) throws SQLException {
        List<ScientificNumber> unsaved = new ArrayList<>();
        for (ScientificNumber sn : sns) {
            if (sn != null && !savedScientificNumbers.containsKey(sn)) unsaved.add(sn);
        }
        if (unsaved.isEmpty()) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO scientific_number (mantissa, exponent) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            for (ScientificNumber sn : unsaved) {
                ps.setDouble(1, sn.getMantissa());
                ps.setLong(2, sn.getExponent());
                ps.addBatch();
            }
            ps.executeBatch();
            ResultSet keys = ps.getGeneratedKeys();
            for (ScientificNumber sn : unsaved) {
                keys.next();
                savedScientificNumbers.put(sn, keys.getLong(1));
            }
        }
    }

    // --- Phase 2: ActivePrimeIndexes (batch) ---
    private void saveActivePrimeIndexesBatch(Connection conn, List<ActivePrimeIndex> allApis) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO active_prime_index (prime_index) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
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
        progress.increment();
    }

    // --- Phase 3: PrimeIndexPowers (batch) ---
    private void savePrimeIndexPowersBatch(Connection conn, List<HcnBody> allBodies) throws SQLException {
        // Collect all PIPs from active APIs
        Set<PrimeIndexPower> pipSet = new LinkedHashSet<>();
        for (ActivePrimeIndex api : savedActivePrimeIndexes.keySet()) {
            pipSet.addAll(api.getPips().values());
        }
        // Also collect PIPs from deactivated bodies
        for (HcnBody body : allBodies) {
            if (body.getPip() != null) pipSet.add(body.getPip());
        }

        List<PrimeIndexPower> allPips = new ArrayList<>(pipSet);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO prime_index_power (prime_index_id, power, proved) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            for (PrimeIndexPower pip : allPips) {
                Long apiId = savedActivePrimeIndexes.get(pip.getActivePrimeIndex());
                if (apiId == null) continue; // skip if API not saved
                ps.setLong(1, apiId);
                ps.setInt(2, pip.getPower());
                ps.setBoolean(3, pip.isProved());
                ps.addBatch();
            }
            ps.executeBatch();
            ResultSet keys = ps.getGeneratedKeys();
            for (PrimeIndexPower pip : allPips) {
                if (savedActivePrimeIndexes.get(pip.getActivePrimeIndex()) == null) continue;
                keys.next();
                savedPips.put(pip, keys.getLong(1));
            }
        }
        progress.increment();
    }

    // --- Phase 4: HcnBodies (batch) ---
    private void saveHcnBodiesBatch(Connection conn, List<HcnBody> allBodies) throws SQLException {
        List<HcnBody> ordered = topologicalSortBodies(allBodies);

        // First: batch save all scientific numbers needed by bodies
        List<ScientificNumber> bodySns = new ArrayList<>();
        for (HcnBody body : ordered) {
            bodySns.add(body.getValue());
            bodySns.add(body.getFactor());
        }
        ensureScientificNumbersSaved(conn, bodySns);

        // Batch insert bodies (parent_id requires topological order, so parents have IDs already)
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO hcn_body (parent_id, pip_id, proved, value_id, factor_id, generator_id, stored_in_db) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            for (HcnBody body : ordered) {
                Long parentId = body.getParent() != null ? savedBodies.get(body.getParent()) : null;
                Long pipId = savedPips.get(body.getPip());
                long valueId = getOrSaveScientificNumber(body.getValue());
                long factorId = getOrSaveScientificNumber(body.getFactor());

                setNullableLong(ps, 1, parentId);
                setNullableLong(ps, 2, pipId);
                ps.setBoolean(3, body.isProved());
                setNullableLong(ps, 4, valueId > 0 ? valueId : null);
                setNullableLong(ps, 5, factorId > 0 ? factorId : null);
                if (body.getHcnGenerator() != null) {
                    ps.setInt(6, body.getHcnGenerator().getId());
                    ps.setBoolean(7, body.getHcnGenerator().isStoredInDb());
                } else {
                    ps.setNull(6, Types.INTEGER);
                    ps.setBoolean(7, false);
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
        progress.increment();

        // Batch update smaller/larger links
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE hcn_body SET smaller_body_id = ?, larger_body_id = ? WHERE id = ?")) {
            int count = 0;
            for (HcnBody body : ordered) {
                Long smallerId = body.getSmallerBody() != null ? savedBodies.get(body.getSmallerBody()) : null;
                Long largerId = body.getLargerBody() != null ? savedBodies.get(body.getLargerBody()) : null;
                if (smallerId != null || largerId != null) {
                    setNullableLong(ps, 1, smallerId);
                    setNullableLong(ps, 2, largerId);
                    ps.setLong(3, savedBodies.get(body));
                    ps.addBatch();
                    count++;
                }
            }
            if (count > 0) ps.executeBatch();
        }
    }

    // --- Phase 5: FixedPowerGroups ---
    private void saveFixedPowerGroups(Connection conn, Matrix m) throws SQLException {
        // Collect all FPGs
        List<FixedPowerGroup> allFpgs = new ArrayList<>();
        for (ActivePrimeIndex api : savedActivePrimeIndexes.keySet()) {
            if (api.getOffspringFixedPowerGroup() != null && !allFpgs.contains(api.getOffspringFixedPowerGroup())) {
                allFpgs.add(api.getOffspringFixedPowerGroup());
            }
        }
        if (allFpgs.isEmpty()) return;

        // Save scientific numbers for FPGs
        List<ScientificNumber> fpgSns = new ArrayList<>();
        for (FixedPowerGroup fpg : allFpgs) {
            fpgSns.add(fpg.getValue());
            fpgSns.add(fpg.getFactor());
        }
        ensureScientificNumbersSaved(conn, fpgSns);

        // Batch insert FPGs
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO fixed_power_group (value_id, factor_id, parent_prime_index_id, offspring_prime_index_id) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            for (FixedPowerGroup fpg : allFpgs) {
                setNullableLong(ps, 1, getOrSaveScientificNumber(fpg.getValue()) > 0 ? getOrSaveScientificNumber(fpg.getValue()) : null);
                setNullableLong(ps, 2, getOrSaveScientificNumber(fpg.getFactor()) > 0 ? getOrSaveScientificNumber(fpg.getFactor()) : null);
                setNullableLong(ps, 3, savedActivePrimeIndexes.get(fpg.getParentPrimeIndex()));
                setNullableLong(ps, 4, savedActivePrimeIndexes.get(fpg.getOffspringPrimeIndex()));
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
                "UPDATE active_prime_index SET member_of_fixed_power_group_id = ?, fixed_power_group_order = ? WHERE id = ?")) {
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
        if (m.getReferenceInterval() != null) {
            allHcns.addAll(m.getReferenceInterval().getHcnList());
        }

        List<Hcn> hcnList = new ArrayList<>(allHcns);

        // Save scientific numbers for HCNs
        List<ScientificNumber> hcnSns = new ArrayList<>();
        for (Hcn hcn : hcnList) {
            hcnSns.add(hcn.getValue());
            hcnSns.add(hcn.getFactor());
        }
        ensureScientificNumbersSaved(conn, hcnSns);

        // Batch insert HCNs
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO hcn (body_id, last_active_prime, value_id, factor_id) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            for (Hcn hcn : hcnList) {
                Long bodyId = hcn.getHcnBody() != null ? savedBodies.get(hcn.getHcnBody()) : null;
                long valueId = getOrSaveScientificNumber(hcn.getValue());
                long factorId = getOrSaveScientificNumber(hcn.getFactor());
                setNullableLong(ps, 1, bodyId);
                ps.setInt(2, hcn.getLastActivePrime());
                setNullableLong(ps, 3, valueId > 0 ? valueId : null);
                setNullableLong(ps, 4, factorId > 0 ? factorId : null);
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

    private void saveReferenceIntervalHcns(Connection conn, Matrix m) throws SQLException {
        if (m.getReferenceInterval() == null || m.getReferenceInterval().getHcnList().isEmpty()) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO reference_interval_hcn (order_in_list, hcn_id) VALUES (?, ?)")) {
            int order = 0;
            for (Hcn hcn : m.getReferenceInterval().getHcnList()) {
                Long hcnId = savedHcns.get(hcn);
                if (hcnId != null) {
                    ps.setInt(1, order++);
                    ps.setLong(2, hcnId);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    // --- Phase 7: LapiGroups ---
    private void saveLapiGroups(Connection conn, Matrix m) throws SQLException {
        // Collect all lapi groups
        List<LastActivePrimeIndexGroup> allLapis = new ArrayList<>();
        LastActivePrimeIndexGroup lapi = m.getLowestLapiGroup();
        while (lapi != null) {
            allLapis.add(lapi);
            lapi = lapi.getHigherLapiGroup();
        }
        if (m.getNextLapiGroup() != null && !allLapis.contains(m.getNextLapiGroup())) {
            allLapis.add(m.getNextLapiGroup());
        }

        // Batch insert
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO last_active_prime_index_group (last_active_prime_index, walker_body_id) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
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
                "UPDATE last_active_prime_index_group SET lower_lapi_group_id = ?, higher_lapi_group_id = ? WHERE id = ?")) {
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
                "INSERT INTO lapi_hcn_list (lapi_group_id, hcn_id, order_in_list) VALUES (?, ?, ?)")) {
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
        // Batch update ActivePrimeIndexes
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE active_prime_index SET next_active_prime_index_id = ?, parent_active_prime_index_id = ?, offspring_fixed_power_group_id = ?, parent_fixed_power_group_id = ?, smallest_body_id = ? WHERE id = ?")) {
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
        progress.increment();

        // Batch update HcnBody lastGeneratedHcn
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE hcn_body SET last_generated_hcn_id = ? WHERE id = ?")) {
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
        progress.increment();
    }

    // --- Phase 9: Matrix ---
    private void saveMatrix(Connection conn, Matrix m, String mode) throws SQLException {
        Long lastApiId = savedActivePrimeIndexes.get(m.getLastActivePrimeIndex());
        Long lowestLapiId = savedLapiGroups.get(m.getLowestLapiGroup());
        Long highestLapiId = savedLapiGroups.get(m.getHighestLapiGroup());
        Long nextLapiId = m.getNextLapiGroup() != null ? savedLapiGroups.get(m.getNextLapiGroup()) : null;
        long provedLimitId = -1;
        if (m.getProvedLimit() != null) {
            ensureScientificNumbersSaved(conn, List.of(m.getProvedLimit()));
            provedLimitId = getOrSaveScientificNumber(m.getProvedLimit());
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO matrix (mode, last_active_prime_index_id, lowest_lapi_group_id, highest_lapi_group_id, next_lapi_group_id, proved_limit_id, proved_count, last_proved_prime_index, basic_data, total_nanos, extend_matrix_nanos, generate_hcn_list_nanos, db_nanos, global_id_counter) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, mode);
            setNullableLong(ps, 2, lastApiId);
            setNullableLong(ps, 3, lowestLapiId);
            setNullableLong(ps, 4, highestLapiId);
            setNullableLong(ps, 5, nextLapiId);
            setNullableLong(ps, 6, provedLimitId > 0 ? provedLimitId : null);
            ps.setInt(7, m.getProvedCount());
            ps.setInt(8, m.getLastProvedPrimeIndex());
            ps.setBoolean(9, GeneratorConfig.isBasicData());
            ps.setLong(10, m.getTotalNanos());
            ps.setLong(11, m.getExtendMatrixNanos());
            ps.setLong(12, m.getGenerateHcnListNanos());
            ps.setLong(13, m.getDbNanos());
            ps.setInt(14, HcnGenerator.getGlobalIdCounter());
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
        // Active bodies from PIPs
        for (ActivePrimeIndex api : savedActivePrimeIndexes.keySet()) {
            for (PrimeIndexPower pip : api.getPips().values()) {
                all.addAll(pip.getActiveHcnBodies());
            }
        }
        // Bodies referenced by HCNs in lapi groups (may include deactivated bodies)
        LastActivePrimeIndexGroup lapi = m.getLowestLapiGroup();
        while (lapi != null) {
            for (Hcn hcn : lapi.getHcnList()) {
                if (hcn.getHcnBody() != null) all.add(hcn.getHcnBody());
            }
            lapi = lapi.getHigherLapiGroup();
        }
        if (m.getNextLapiGroup() != null) {
            for (Hcn hcn : m.getNextLapiGroup().getHcnList()) {
                if (hcn.getHcnBody() != null) all.add(hcn.getHcnBody());
            }
        }
        // Bodies referenced by referenceInterval HCNs
        if (m.getReferenceInterval() != null) {
            for (Hcn hcn : m.getReferenceInterval().getHcnList()) {
                if (hcn.getHcnBody() != null) all.add(hcn.getHcnBody());
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
