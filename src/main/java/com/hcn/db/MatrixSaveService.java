package com.hcn.db;

import com.hcn.detailed.*;
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

    // Identity maps for deduplication during save
    private Map<Object, Long> savedScientificNumbers;
    private Map<ActivePrimeIndex, Long> savedActivePrimeIndexes;
    private Map<PrimeIndexPower, Long> savedPips;
    private Map<HcnBody, Long> savedBodies;
    private Map<Hcn, Long> savedHcns;
    private Map<FixedPowerGroup, Long> savedFpgs;
    private Map<LastActivePrimeIndexGroup, Long> savedLapiGroups;

    public void save(Object matrix, String dbName) {
        if (dbName == null) return;

        String mode;
        com.hcn.detailed.Matrix detailedMatrix = null;

        if (matrix instanceof com.hcn.detailed.Matrix) {
            mode = "detailed";
            detailedMatrix = (com.hcn.detailed.Matrix) matrix;
        } else if (matrix instanceof com.hcn.core.Matrix) {
            mode = "core";
        } else {
            return;
        }

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

            if (detailedMatrix != null) {
                saveDetailed(conn, detailedMatrix, mode);
            } else {
                saveCoreMetadataOnly(conn, (com.hcn.core.Matrix) matrix, mode);
            }

            progress.startPhase(10, "Committing to disk", 1);
            conn.commit();
            progress.increment();
            progress.done();
        } catch (Exception e) {
            progress.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveCoreMetadataOnly(Connection conn, com.hcn.core.Matrix m, String mode) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO matrix (mode, proved_count, last_proved_prime_index) VALUES (?, ?, ?)")) {
            ps.setString(1, mode);
            ps.setInt(2, m.getProvedCount());
            ps.setInt(3, m.getLastActivePrimeIndex().getIndex());
            ps.executeUpdate();
        }
    }

    private void saveDetailed(Connection conn, com.hcn.detailed.Matrix m, String mode) throws SQLException {
        // Phase 2: ActivePrimeIndexes (without FK links)
        List<ActivePrimeIndex> allApis = collectAllActivePrimeIndexes(m);
        progress.startPhase(2, "ActivePrimeIndexes", allApis.size());
        saveActivePrimeIndexes(conn, allApis);

        // Phase 3: PrimeIndexPowers
        int pipCount = allApis.stream().mapToInt(a -> a.getPips().size()).sum();
        progress.startPhase(3, "PrimeIndexPowers", pipCount);
        savePrimeIndexPowers(conn, m);

        // Phase 4: HcnBodies (without lastGeneratedHcn)
        List<HcnBody> allBodies = collectAllBodies(m);
        progress.startPhase(4, "HcnBodies", allBodies.size());
        saveHcnBodies(conn, allBodies);

        // Phase 5: FixedPowerGroups
        progress.startPhase(5, "FixedPowerGroups", savedActivePrimeIndexes.size());
        saveFixedPowerGroups(conn, m);

        // Phase 6: HCNs
        progress.startPhase(6, "HCNs", 0);
        saveHcns(conn, m);

        // Phase 7: LapiGroups
        progress.startPhase(7, "LapiGroups", 0);
        saveLapiGroups(conn, m);

        // Phase 8: Back-fill FKs
        progress.startPhase(8, "Back-fill references", savedActivePrimeIndexes.size() + savedBodies.size());
        backfillReferences(conn, m);

        // Phase 9: Matrix
        progress.startPhase(9, "Matrix", 1);
        saveMatrix(conn, m, mode);
        progress.increment();
    }

    private void clearAll(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE lapi_hcn_list, matrix, last_active_prime_index_group, hcn, hcn_body, prime_index_power, fixed_power_group, active_prime_index, scientific_number CASCADE");
        }
    }

    // --- Phase 1: ScientificNumber ---
    private long saveScientificNumber(Connection conn, ScientificNumber sn) throws SQLException {
        if (sn == null) return -1;
        Long existing = savedScientificNumbers.get(sn);
        if (existing != null) return existing;

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO scientific_number (mantissa, exponent) VALUES (?, ?) RETURNING id")) {
            ps.setDouble(1, sn.getMantissa());
            ps.setLong(2, sn.getExponent());
            ResultSet rs = ps.executeQuery();
            rs.next();
            long id = rs.getLong(1);
            savedScientificNumbers.put(sn, id);
            return id;
        }
    }

    // --- Phase 2: ActivePrimeIndexes (no FKs yet) ---
    private void saveActivePrimeIndexes(Connection conn, List<ActivePrimeIndex> allApis) throws SQLException {
        for (ActivePrimeIndex api : allApis) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO active_prime_index (prime_index) VALUES (?) RETURNING id")) {
                ps.setInt(1, api.getIndex());
                ResultSet rs = ps.executeQuery();
                rs.next();
                savedActivePrimeIndexes.put(api, rs.getLong(1));
            }
            progress.increment();
        }
    }

    private List<ActivePrimeIndex> collectAllActivePrimeIndexes(com.hcn.detailed.Matrix m) {
        List<ActivePrimeIndex> all = new ArrayList<>();
        ActivePrimeIndex first = findFirstActivePrimeIndex(m);
        collectApiChain(first, all);
        return all;
    }

    private void collectApiChain(ActivePrimeIndex api, List<ActivePrimeIndex> all) {
        if (api == null || all.contains(api)) return;
        all.add(api);
        if (api.getOffspringFixedPowerGroup() != null) {
            // Collect FPG members
            for (ActivePrimeIndex member : api.getOffspringFixedPowerGroup().getFixedPowerGroup()) {
                all.add(member);
            }
            // Continue with offspring of FPG
            collectApiChain(api.getOffspringFixedPowerGroup().getOffspringPrimeIndex(), all);
        } else if (api.getNextActivePrimeIndex() != null) {
            collectApiChain(api.getNextActivePrimeIndex(), all);
        }
    }

    // --- Phase 3: PrimeIndexPowers ---
    private void savePrimeIndexPowers(Connection conn, com.hcn.detailed.Matrix m) throws SQLException {
        for (Map.Entry<ActivePrimeIndex, Long> entry : savedActivePrimeIndexes.entrySet()) {
            ActivePrimeIndex api = entry.getKey();
            long apiId = entry.getValue();
            for (PrimeIndexPower pip : api.getPips().values()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO prime_index_power (prime_index_id, power, proved) VALUES (?, ?, ?) RETURNING id")) {
                    ps.setLong(1, apiId);
                    ps.setInt(2, pip.getPower());
                    ps.setBoolean(3, pip.isProved());
                    ResultSet rs = ps.executeQuery();
                    rs.next();
                    savedPips.put(pip, rs.getLong(1));
                }
                progress.increment();
            }
        }
    }

    // --- Phase 4: HcnBodies (without lastGeneratedHcn) ---
    private void saveHcnBodies(Connection conn, List<HcnBody> allBodies) throws SQLException {
        List<HcnBody> ordered = topologicalSortBodies(allBodies);

        for (HcnBody body : ordered) {
            long valueId = saveScientificNumber(conn, body.getValue());
            long factorId = saveScientificNumber(conn, body.getFactor());
            Long parentId = body.getParent() != null ? savedBodies.get(body.getParent()) : null;
            Long pipId = savedPips.get(body.getPip());

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO hcn_body (parent_id, pip_id, proved, value_id, factor_id) VALUES (?, ?, ?, ?, ?) RETURNING id")) {
                setNullableLong(ps, 1, parentId);
                setNullableLong(ps, 2, pipId);
                ps.setBoolean(3, body.isProved());
                setNullableLong(ps, 4, valueId > 0 ? valueId : null);
                setNullableLong(ps, 5, factorId > 0 ? factorId : null);
                ResultSet rs = ps.executeQuery();
                rs.next();
                savedBodies.put(body, rs.getLong(1));
            }
            progress.increment();
        }

        // Update smaller/larger body links
        for (HcnBody body : ordered) {
            Long bodyId = savedBodies.get(body);
            Long smallerId = body.getSmallerBody() != null ? savedBodies.get(body.getSmallerBody()) : null;
            Long largerId = body.getLargerBody() != null ? savedBodies.get(body.getLargerBody()) : null;
            if (smallerId != null || largerId != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE hcn_body SET smaller_body_id = ?, larger_body_id = ? WHERE id = ?")) {
                    setNullableLong(ps, 1, smallerId);
                    setNullableLong(ps, 2, largerId);
                    ps.setLong(3, bodyId);
                    ps.executeUpdate();
                }
            }
        }
    }

    // --- Phase 5: FixedPowerGroups ---
    private void saveFixedPowerGroups(Connection conn, com.hcn.detailed.Matrix m) throws SQLException {
        for (ActivePrimeIndex api : savedActivePrimeIndexes.keySet()) {
            if (api.getOffspringFixedPowerGroup() != null) {
                saveFixedPowerGroup(conn, api.getOffspringFixedPowerGroup());
            }
        }
    }

    private void saveFixedPowerGroup(Connection conn, FixedPowerGroup fpg) throws SQLException {
        if (savedFpgs.containsKey(fpg)) return;

        long valueId = saveScientificNumber(conn, fpg.getValue());
        long factorId = saveScientificNumber(conn, fpg.getFactor());
        Long parentPiId = savedActivePrimeIndexes.get(fpg.getParentPrimeIndex());
        Long offspringPiId = savedActivePrimeIndexes.get(fpg.getOffspringPrimeIndex());

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO fixed_power_group (value_id, factor_id, parent_prime_index_id, offspring_prime_index_id) VALUES (?, ?, ?, ?) RETURNING id")) {
            setNullableLong(ps, 1, valueId > 0 ? valueId : null);
            setNullableLong(ps, 2, factorId > 0 ? factorId : null);
            setNullableLong(ps, 3, parentPiId);
            setNullableLong(ps, 4, offspringPiId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            long fpgId = rs.getLong(1);
            savedFpgs.put(fpg, fpgId);
        }

        // Update member active_prime_indexes
        int order = 0;
        for (ActivePrimeIndex member : fpg.getFixedPowerGroup()) {
            Long memberId = savedActivePrimeIndexes.get(member);
            if (memberId != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE active_prime_index SET member_of_fixed_power_group_id = ?, fixed_power_group_order = ? WHERE id = ?")) {
                    ps.setLong(1, savedFpgs.get(fpg));
                    ps.setInt(2, order++);
                    ps.setLong(3, memberId);
                    ps.executeUpdate();
                }
            }
        }
    }

    // --- Phase 6: HCNs ---
    private void saveHcns(Connection conn, com.hcn.detailed.Matrix m) throws SQLException {
        // Collect all HCNs from lapi groups
        Set<Hcn> allHcns = new LinkedHashSet<>();
        LastActivePrimeIndexGroup lapi = m.getLowestLapiGroup();
        while (lapi != null) {
            allHcns.addAll(lapi.getHcnList());
            lapi = lapi.getHigherLapiGroup();
        }
        // Also from nextLapiGroup
        if (m.getNextLapiGroup() != null) {
            allHcns.addAll(m.getNextLapiGroup().getHcnList());
        }
        // Also collect lastGeneratedHcn from all bodies
        for (HcnBody body : savedBodies.keySet()) {
            if (body.getLastGeneratedHcn() != null) {
                allHcns.add(body.getLastGeneratedHcn());
            }
        }

        for (Hcn hcn : allHcns) {
            if (savedHcns.containsKey(hcn)) continue;
            long valueId = saveScientificNumber(conn, hcn.getValue());
            long factorId = saveScientificNumber(conn, hcn.getFactor());
            Long bodyId = hcn.getBody() != null ? savedBodies.get(hcn.getBody()) : null;

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO hcn (body_id, last_active_prime, value_id, factor_id) VALUES (?, ?, ?, ?) RETURNING id")) {
                setNullableLong(ps, 1, bodyId);
                ps.setInt(2, hcn.getLastActivePrime());
                setNullableLong(ps, 3, valueId > 0 ? valueId : null);
                setNullableLong(ps, 4, factorId > 0 ? factorId : null);
                ResultSet rs = ps.executeQuery();
                rs.next();
                savedHcns.put(hcn, rs.getLong(1));
            }
        }
    }

    // --- Phase 7: LapiGroups ---
    private void saveLapiGroups(Connection conn, com.hcn.detailed.Matrix m) throws SQLException {
        // Save from lowest to highest
        LastActivePrimeIndexGroup lapi = m.getLowestLapiGroup();
        while (lapi != null) {
            saveSingleLapiGroup(conn, lapi);
            lapi = lapi.getHigherLapiGroup();
        }

        // Save nextLapiGroup if it exists and wasn't already saved
        if (m.getNextLapiGroup() != null && !savedLapiGroups.containsKey(m.getNextLapiGroup())) {
            saveSingleLapiGroup(conn, m.getNextLapiGroup());
        }

        // Also save HCNs that are in nextLapiGroup but weren't saved in phase 6
        if (m.getNextLapiGroup() != null) {
            for (Hcn hcn : m.getNextLapiGroup().getHcnList()) {
                if (!savedHcns.containsKey(hcn)) {
                    saveHcn(conn, hcn);
                }
            }
        }

        // Update lower/higher links
        for (Map.Entry<LastActivePrimeIndexGroup, Long> entry : savedLapiGroups.entrySet()) {
            LastActivePrimeIndexGroup g = entry.getKey();
            Long lapiId = entry.getValue();
            Long lowerId = g.getLowerLapiGroup() != null ? savedLapiGroups.get(g.getLowerLapiGroup()) : null;
            Long higherId = g.getHigherLapiGroup() != null ? savedLapiGroups.get(g.getHigherLapiGroup()) : null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE last_active_prime_index_group SET lower_lapi_group_id = ?, higher_lapi_group_id = ? WHERE id = ?")) {
                setNullableLong(ps, 1, lowerId);
                setNullableLong(ps, 2, higherId);
                ps.setLong(3, lapiId);
                ps.executeUpdate();
            }

            // Save hcn list
            int order = 0;
            for (Hcn hcn : g.getHcnList()) {
                Long hcnId = savedHcns.get(hcn);
                if (hcnId != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO lapi_hcn_list (lapi_group_id, hcn_id, order_in_list) VALUES (?, ?, ?)")) {
                        ps.setLong(1, lapiId);
                        ps.setLong(2, hcnId);
                        ps.setInt(3, order++);
                        ps.executeUpdate();
                    }
                }
            }
        }
    }

    private void saveSingleLapiGroup(Connection conn, LastActivePrimeIndexGroup lapi) throws SQLException {
        Long walkerBodyId = lapi.getWalkerBody() != null ? savedBodies.get(lapi.getWalkerBody()) : null;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO last_active_prime_index_group (last_active_prime_index, walker_body_id) VALUES (?, ?) RETURNING id")) {
            ps.setInt(1, lapi.getLastActivePrimeIndex());
            setNullableLong(ps, 2, walkerBodyId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            savedLapiGroups.put(lapi, rs.getLong(1));
        }
    }

    private void saveHcn(Connection conn, Hcn hcn) throws SQLException {
        long valueId = saveScientificNumber(conn, hcn.getValue());
        long factorId = saveScientificNumber(conn, hcn.getFactor());
        Long bodyId = hcn.getBody() != null ? savedBodies.get(hcn.getBody()) : null;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO hcn (body_id, last_active_prime, value_id, factor_id) VALUES (?, ?, ?, ?) RETURNING id")) {
            setNullableLong(ps, 1, bodyId);
            ps.setInt(2, hcn.getLastActivePrime());
            setNullableLong(ps, 3, valueId > 0 ? valueId : null);
            setNullableLong(ps, 4, factorId > 0 ? factorId : null);
            ResultSet rs = ps.executeQuery();
            rs.next();
            savedHcns.put(hcn, rs.getLong(1));
        }
    }

    // --- Phase 8: Back-fill FKs ---
    private void backfillReferences(Connection conn, com.hcn.detailed.Matrix m) throws SQLException {
        // ActivePrimeIndex: next, parent, offspring/parent FPG, smallestBody
        for (Map.Entry<ActivePrimeIndex, Long> entry : savedActivePrimeIndexes.entrySet()) {
            ActivePrimeIndex current = entry.getKey();
            Long id = entry.getValue();
            Long nextId = current.getNextActivePrimeIndex() != null ? savedActivePrimeIndexes.get(current.getNextActivePrimeIndex()) : null;
            Long parentId = current.getParentActivePrimeIndex() != null ? savedActivePrimeIndexes.get(current.getParentActivePrimeIndex()) : null;
            Long offspringFpgId = current.getOffspringFixedPowerGroup() != null ? savedFpgs.get(current.getOffspringFixedPowerGroup()) : null;
            Long parentFpgId = current.getParentFixedPowerGroup() != null ? savedFpgs.get(current.getParentFixedPowerGroup()) : null;
            Long smallestBodyId = current.getHcnBodyList().getSmallestBody() != null ? savedBodies.get(current.getHcnBodyList().getSmallestBody()) : null;

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE active_prime_index SET next_active_prime_index_id = ?, parent_active_prime_index_id = ?, offspring_fixed_power_group_id = ?, parent_fixed_power_group_id = ?, smallest_body_id = ? WHERE id = ?")) {
                setNullableLong(ps, 1, nextId);
                setNullableLong(ps, 2, parentId);
                setNullableLong(ps, 3, offspringFpgId);
                setNullableLong(ps, 4, parentFpgId);
                setNullableLong(ps, 5, smallestBodyId);
                ps.setLong(6, id);
                ps.executeUpdate();
            }
            progress.increment();
        }

        // HcnBody: lastGeneratedHcn
        for (Map.Entry<HcnBody, Long> entry : savedBodies.entrySet()) {
            HcnBody body = entry.getKey();
            if (body.getLastGeneratedHcn() != null) {
                Long hcnId = savedHcns.get(body.getLastGeneratedHcn());
                if (hcnId != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE hcn_body SET last_generated_hcn_id = ? WHERE id = ?")) {
                        ps.setLong(1, hcnId);
                        ps.setLong(2, entry.getValue());
                        ps.executeUpdate();
                    }
                }
            }
            progress.increment();
        }
    }

    // --- Phase 9: Matrix ---
    private void saveMatrix(Connection conn, com.hcn.detailed.Matrix m, String mode) throws SQLException {
        Long lastApiId = savedActivePrimeIndexes.get(m.getLastActivePrimeIndex());
        Long lowestLapiId = savedLapiGroups.get(m.getLowestLapiGroup());
        Long highestLapiId = savedLapiGroups.get(m.getHighestLapiGroup());
        Long nextLapiId = m.getNextLapiGroup() != null ? savedLapiGroups.get(m.getNextLapiGroup()) : null;
        long provedLimitId = saveScientificNumber(conn, m.getProvedLimit());

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO matrix (mode, last_active_prime_index_id, lowest_lapi_group_id, highest_lapi_group_id, next_lapi_group_id, proved_limit_id, proved_count, last_proved_prime_index) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, mode);
            setNullableLong(ps, 2, lastApiId);
            setNullableLong(ps, 3, lowestLapiId);
            setNullableLong(ps, 4, highestLapiId);
            setNullableLong(ps, 5, nextLapiId);
            setNullableLong(ps, 6, provedLimitId > 0 ? provedLimitId : null);
            ps.setInt(7, m.getProvedCount());
            ps.setInt(8, m.getLastProvedPrimeIndex());
            ps.executeUpdate();
        }
    }

    // --- Helpers ---
    private ActivePrimeIndex findFirstActivePrimeIndex(com.hcn.detailed.Matrix m) {
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

    private List<HcnBody> collectAllBodies(com.hcn.detailed.Matrix m) {
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
