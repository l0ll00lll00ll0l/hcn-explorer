package com.hcn.db;

import com.hcn.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class MatrixLoadService {

    @Autowired
    private DatabaseService databaseService;

    private Map<Long, ActivePrimeIndex> activePrimeIndexes;
    private Map<Long, PrimeIndexPower> pips;
    private Map<Long, HcnBody> bodies;
    private Map<Long, Hcn> hcns;
    private Map<Long, FixedPowerGroup> fpgs;
    private Map<Long, LastActivePrimeIndexGroup> lapiGroups;

    public Matrix load(String dbName) {
        activePrimeIndexes = new HashMap<>();
        pips = new HashMap<>();
        bodies = new HashMap<>();
        hcns = new HashMap<>();
        fpgs = new HashMap<>();
        lapiGroups = new HashMap<>();

        try (Connection conn = databaseService.getConnection(dbName)) {
            loadActivePrimeIndexes(conn);
            loadPrimeIndexPowers(conn);
            loadHcnBodies(conn);
            loadFixedPowerGroups(conn);
            loadHcns(conn);
            loadLapiGroups(conn);
            return assembleMatrix(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Load failed for " + dbName, e);
        }
    }

    private void loadActivePrimeIndexes(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, prime_index FROM temp_active_prime_index")) {
            while (rs.next()) {
                activePrimeIndexes.put(rs.getLong(1), new ActivePrimeIndex(rs.getInt(2)));
            }
        }
        // Wire next/parent links
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, next_active_prime_index_id, parent_active_prime_index_id FROM temp_active_prime_index")) {
            while (rs.next()) {
                ActivePrimeIndex api = activePrimeIndexes.get(rs.getLong(1));
                long nextId = rs.getLong(2);
                if (!rs.wasNull()) api.setNextActivePrimeIndex(activePrimeIndexes.get(nextId));
                long parentId = rs.getLong(3);
                if (!rs.wasNull()) api.setParentActivePrimeIndex(activePrimeIndexes.get(parentId));
            }
        }
    }

    private void loadPrimeIndexPowers(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, prime_index_id, power, proved FROM temp_prime_index_power ORDER BY prime_index_id, power")) {
            while (rs.next()) {
                long id = rs.getLong(1);
                ActivePrimeIndex api = activePrimeIndexes.get(rs.getLong(2));
                int power = rs.getInt(3);
                boolean proved = rs.getBoolean(4);
                PrimeIndexPower pip = new PrimeIndexPower(api, power);
                pip.setProved(proved);
                api.getPips().put(power, pip);
                pips.put(id, pip);
            }
        }
    }

    private void loadHcnBodies(Connection conn) throws SQLException {
        // First pass: create all bodies
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, pip_id, proved, value_mantissa, value_exponent, factor_mantissa, factor_exponent FROM temp_hcn_body")) {
            while (rs.next()) {
                long id = rs.getLong(1);
                HcnBody body = new HcnBody();
                long pipId = rs.getLong(2);
                if (!rs.wasNull()) body.setPip(pips.get(pipId));
                body.setProved(rs.getBoolean(3));
                double valMantissa = rs.getDouble(4);
                if (!rs.wasNull()) body.setValue(new ScientificNumber(valMantissa, rs.getLong(5)));
                double facMantissa = rs.getDouble(6);
                if (!rs.wasNull()) body.setFactor(new ScientificNumber(facMantissa, rs.getLong(7)));
                bodies.put(id, body);
            }
        }
        // Second pass: wire parent, smaller, larger
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, parent_id, smaller_body_id, larger_body_id FROM temp_hcn_body")) {
            while (rs.next()) {
                HcnBody body = bodies.get(rs.getLong(1));
                long parentId = rs.getLong(2);
                if (!rs.wasNull()) {
                    HcnBody parent = bodies.get(parentId);
                    body.setParentForLoad(parent);
                    parent.getOffsprings().add(body);
                }
                long smallerId = rs.getLong(3);
                if (!rs.wasNull()) body.setSmallerBody(bodies.get(smallerId));
                long largerId = rs.getLong(4);
                if (!rs.wasNull()) body.setLargerBody(bodies.get(largerId));
            }
        }
        // Register bodies with their PIPs
        for (HcnBody body : bodies.values()) {
            if (body.getPip() != null) {
                body.getPip().addActiveHcnBody(body);
            }
        }
        // Wire smallestBody on ActivePrimeIndexes
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, smallest_body_id FROM temp_active_prime_index WHERE smallest_body_id IS NOT NULL")) {
            while (rs.next()) {
                ActivePrimeIndex api = activePrimeIndexes.get(rs.getLong(1));
                HcnBody smallest = bodies.get(rs.getLong(2));
                api.getHcnBodyList().setSmallestBodyForLoad(smallest);
            }
        }
    }

    private void loadFixedPowerGroups(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, value_mantissa, value_exponent, factor_mantissa, factor_exponent, parent_prime_index_id, offspring_prime_index_id FROM temp_fixed_power_group")) {
            while (rs.next()) {
                long id = rs.getLong(1);
                FixedPowerGroup fpg = new FixedPowerGroup();
                fpg.setValueForLoad(new ScientificNumber(rs.getDouble(2), rs.getLong(3)));
                fpg.setFactorForLoad(new ScientificNumber(rs.getDouble(4), rs.getLong(5)));
                long parentPiId = rs.getLong(6);
                if (!rs.wasNull()) fpg.setParentPrimeIndex(activePrimeIndexes.get(parentPiId));
                long offspringPiId = rs.getLong(7);
                if (!rs.wasNull()) fpg.setOffspringPrimeIndexForLoad(activePrimeIndexes.get(offspringPiId));
                fpgs.put(id, fpg);
            }
        }
        // Wire FPG members
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, member_of_fixed_power_group_id FROM temp_active_prime_index WHERE member_of_fixed_power_group_id IS NOT NULL ORDER BY fixed_power_group_order")) {
            while (rs.next()) {
                ActivePrimeIndex api = activePrimeIndexes.get(rs.getLong(1));
                FixedPowerGroup fpg = fpgs.get(rs.getLong(2));
                fpg.getFixedPowerGroup().add(api);
            }
        }
        // Wire offspring/parent FPG on ActivePrimeIndexes
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, offspring_fixed_power_group_id, parent_fixed_power_group_id FROM temp_active_prime_index")) {
            while (rs.next()) {
                ActivePrimeIndex api = activePrimeIndexes.get(rs.getLong(1));
                long offFpgId = rs.getLong(2);
                if (!rs.wasNull()) api.setOffspringFixedPowerGroupForLoad(fpgs.get(offFpgId));
                long parFpgId = rs.getLong(3);
                if (!rs.wasNull()) api.setParentFixedPowerGroupForLoad(fpgs.get(parFpgId));
            }
        }
    }

    private void loadHcns(Connection conn) throws SQLException {
        // Load which bodies have generators
        Map<Long, Long> bodyToLastHcnId = new HashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, last_generated_hcn_id FROM temp_hcn_body WHERE last_generated_hcn_id IS NOT NULL")) {
            while (rs.next()) {
                bodyToLastHcnId.put(rs.getLong(1), rs.getLong(2));
            }
        }

        // Create HcnGenerators for bodies that have them
        Map<Long, HcnGenerator> bodyIdToGenerator = new HashMap<>();
        for (Map.Entry<Long, Long> entry : bodyToLastHcnId.entrySet()) {
            HcnBody body = bodies.get(entry.getKey());
            HcnGenerator gen = new HcnGenerator(body);
            body.setHcnGenerator(gen);
            bodyIdToGenerator.put(entry.getKey(), gen);
        }

        // Load all HCNs
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, body_id, last_active_prime, value_mantissa, value_exponent, factor_mantissa, factor_exponent FROM temp_hcn")) {
            while (rs.next()) {
                long id = rs.getLong(1);
                long bodyId = rs.getLong(2);
                HcnBody body = bodies.get(bodyId);
                int lastActivePrime = rs.getInt(3);

                HcnGenerator gen = bodyIdToGenerator.get(bodyId);
                if (gen == null) {
                    gen = new HcnGenerator(body);
                }
                Hcn hcn = new Hcn(gen, lastActivePrime);

                double valMantissa = rs.getDouble(4);
                if (!rs.wasNull()) hcn.setValue(new ScientificNumber(valMantissa, rs.getLong(5)));
                double facMantissa = rs.getDouble(6);
                if (!rs.wasNull()) hcn.setFactor(new ScientificNumber(facMantissa, rs.getLong(7)));

                hcns.put(id, hcn);
            }
        }

        // Wire lastGeneratedHcn
        for (Map.Entry<Long, Long> entry : bodyToLastHcnId.entrySet()) {
            HcnGenerator gen = bodyIdToGenerator.get(entry.getKey());
            Hcn lastHcn = hcns.get(entry.getValue());
            gen.setLastGeneratedHcn(lastHcn);
        }
    }

    private void loadLapiGroups(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, last_active_prime_index, walker_body_id FROM temp_last_active_prime_index_group")) {
            while (rs.next()) {
                long id = rs.getLong(1);
                int lapiIndex = rs.getInt(2);
                HcnBody walkerBody = null;
                long walkerBodyId = rs.getLong(3);
                if (!rs.wasNull()) walkerBody = bodies.get(walkerBodyId);

                LastActivePrimeIndexGroup group = new LastActivePrimeIndexGroup();
                group.setLastActivePrimeIndex(lapiIndex);
                group.setWalkerBody(walkerBody);
                if (walkerBody != null) walkerBody.addWalkerBodyForLapi(group);
                lapiGroups.put(id, group);
            }
        }
        // Wire lower/higher links
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, lower_lapi_group_id, higher_lapi_group_id FROM temp_last_active_prime_index_group")) {
            while (rs.next()) {
                LastActivePrimeIndexGroup group = lapiGroups.get(rs.getLong(1));
                long lowerId = rs.getLong(2);
                if (!rs.wasNull()) group.setLowerLapiGroup(lapiGroups.get(lowerId));
                long higherId = rs.getLong(3);
                if (!rs.wasNull()) group.setHigherLapiGroup(lapiGroups.get(higherId));
            }
        }
        // Load hcn lists
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT lapi_group_id, hcn_id FROM temp_lapi_hcn_list ORDER BY lapi_group_id, order_in_list")) {
            while (rs.next()) {
                LastActivePrimeIndexGroup group = lapiGroups.get(rs.getLong(1));
                Hcn hcn = hcns.get(rs.getLong(2));
                group.getHcnList().add(hcn);
            }
        }
    }

    private Matrix assembleMatrix(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT last_active_prime_index_id, lowest_lapi_group_id, highest_lapi_group_id, next_lapi_group_id, proved_limit_mantissa, proved_limit_exponent, proved_count, last_proved_prime_index, lowest_proved_lapi_within_interval, basic_data FROM temp_matrix LIMIT 1")) {
            if (!rs.next()) throw new RuntimeException("No matrix row found");

            ActivePrimeIndex lastApi = activePrimeIndexes.get(rs.getLong(1));
            LastActivePrimeIndexGroup lowestLapi = lapiGroups.get(rs.getLong(2));
            LastActivePrimeIndexGroup highestLapi = lapiGroups.get(rs.getLong(3));
            long nextLapiId = rs.getLong(4);
            LastActivePrimeIndexGroup nextLapi = rs.wasNull() ? null : lapiGroups.get(nextLapiId);
            double plMantissa = rs.getDouble(5);
            ScientificNumber provedLimit = rs.wasNull() ? null : new ScientificNumber(plMantissa, rs.getLong(6));
            int provedCount = rs.getInt(7);
            int lastProvedPrimeIndex = rs.getInt(8);
            int lowestProvedLapi = rs.getInt(9);
            boolean basicData = rs.getBoolean(10);

            if (basicData) {
                return com.hcn.core.basicdata.BasicDataMatrix.fromLoad(lastApi, lowestLapi, highestLapi, nextLapi, provedLimit, provedCount, lastProvedPrimeIndex, lowestProvedLapi);
            }
            return Matrix.fromLoad(lastApi, lowestLapi, highestLapi, nextLapi, provedLimit, provedCount, lastProvedPrimeIndex, lowestProvedLapi);
        }
    }
}
