package com.hcn.db;

import com.hcn.core.*;
import com.hcn.core.dbspecific.Body;
import com.hcn.core.dbspecific.Interval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class IntervalStorageService {

    @Autowired
    private DatabaseService databaseService;

    public void saveIntervals(String dbName, List<Interval> intervals) {
        if (dbName == null || intervals.isEmpty()) return;

        try (Connection conn = databaseService.getConnection(dbName)) {
            conn.setAutoCommit(false);
            saveIntervalRows(conn, intervals);
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveBodies(Connection conn, List<Interval> intervals) throws SQLException {
        Set<Long> bodyIds = new HashSet<>();
        List<HcnBody> bodiesToSave = new ArrayList<>();

        for (Interval interval : intervals) {
            for (Hcn hcn : interval.getHcnList()) {
                Body body = hcn.getHcnGenerator().getBody();
                // TODO: use central ID from Interval/Body once implemented
                // For now skip - needs ID assignment
            }
        }

        if (bodiesToSave.isEmpty()) return;

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO basic_data_body (id, head, tail) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING")) {
            for (HcnBody body : bodiesToSave) {
                Body b = body.getHcnGenerator().getBody();
                // TODO: ps.setLong(1, body.getStoredId());
                // ps.setArray(2, conn.createArrayOf("integer", toIntegerArray(b.getHead())));
                // ps.setArray(3, conn.createArrayOf("integer", toIntegerArray(b.getTail())));
                // ps.addBatch();
            }
            // ps.executeBatch();
        }
    }

    private void saveIntervalRows(Connection conn, List<Interval> intervals) throws SQLException {
        List<HcnGenerator> newGenerators = new ArrayList<>();

        // Get next HCN id
        long nextHcnId;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(id), 0) FROM basic_data_hcn")) {
            rs.next();
            nextHcnId = rs.getLong(1) + 1;
        }

        // Compute nextHcnSerial
        long nextHcnSerial = 1; // default if no intervals saved yet
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT starter_hcn_serial, starter_hcn_id FROM basic_data_interval WHERE reference_interval_lapi IS NULL ORDER BY lapi DESC LIMIT 1")) {
            if (rs.next()) {
                long lastSelfRefSerial = rs.getLong(1);
                long lastSelfRefHcnId = rs.getLong(2);
                long hcnListSize = nextHcnId - lastSelfRefHcnId;

                // Count how many intervals were saved after the last self-referenced one
                long lastSelfRefLapi;
                try (Statement stmt2 = conn.createStatement();
                     ResultSet rs2 = stmt2.executeQuery(
                        "SELECT lapi FROM basic_data_interval WHERE reference_interval_lapi IS NULL ORDER BY lapi DESC LIMIT 1")) {
                    rs2.next();
                    lastSelfRefLapi = rs2.getLong(1);
                }
                long countAfter;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM basic_data_interval WHERE lapi > ?")) {
                    ps.setLong(1, lastSelfRefLapi);
                    ResultSet rs3 = ps.executeQuery();
                    rs3.next();
                    countAfter = rs3.getLong(1);
                }

                nextHcnSerial = lastSelfRefSerial + hcnListSize * (1 + countAfter);
            }
        }

        try (PreparedStatement intervalPs = conn.prepareStatement(
                "INSERT INTO basic_data_interval (lapi, value_mantissa, value_exponent, factor_mantissa, factor_exponent, reference_interval_lapi, starter_hcn_id, starter_hcn_serial) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
             PreparedStatement hcnPs = conn.prepareStatement(
                "INSERT INTO basic_data_hcn (body_id, last_active_prime) VALUES (?, ?)");
             PreparedStatement bodyPs = conn.prepareStatement(
                "INSERT INTO basic_data_body (id, head, tail) VALUES (?, ?, ?)")) {
            for (Interval interval : intervals) {
                intervalPs.setInt(1, interval.getLapi());
                intervalPs.setDouble(2, interval.getValue().getMantissa());
                intervalPs.setLong(3, interval.getValue().getExponent());
                intervalPs.setDouble(4, interval.getFactor().getMantissa());
                intervalPs.setLong(5, interval.getFactor().getExponent());
                if (interval.getReferenceInterval() != null && interval.getReferenceInterval() != interval) {
                    intervalPs.setInt(6, interval.getReferenceInterval().getLapi());
                    intervalPs.setNull(7, Types.BIGINT);
                    intervalPs.setLong(8, nextHcnSerial);
                    nextHcnSerial += interval.getReferenceInterval().getHcnList().size();
                } else {
                    intervalPs.setNull(6, Types.INTEGER);
                    intervalPs.setLong(7, nextHcnId);
                    for (Hcn hcn : interval.getHcnList()) {
                        hcnPs.setLong(1, hcn.getHcnGenerator().getId());
                        hcnPs.setInt(2, hcn.getLastActivePrime());
                        hcnPs.addBatch();

                        if (!hcn.getHcnGenerator().isStoredInDb()) {
                            hcn.getHcnGenerator().setStoredInDb(true);
                            newGenerators.add(hcn.getHcnGenerator());
                        }
                    }
                    nextHcnId += interval.getHcnList().size();
                    intervalPs.setLong(8, nextHcnSerial);
                    nextHcnSerial += interval.getHcnList().size();
                }
                intervalPs.addBatch();
            }

            for (HcnGenerator gen : newGenerators) {
                Body body = gen.getBody();
                bodyPs.setInt(1, gen.getId());
                bodyPs.setArray(2, conn.createArrayOf("integer", toIntegerArray(body.getHead())));
                bodyPs.setArray(3, conn.createArrayOf("integer", toIntegerArray(body.getTail())));
                bodyPs.addBatch();
            }

            intervalPs.executeBatch();
            hcnPs.executeBatch();
            bodyPs.executeBatch();
        }
    }

    private void saveIntervalHcns(Connection conn, List<Interval> intervals) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO basic_data_hcn (body_id, last_active_prime) VALUES (?, ?)")) {
            for (Interval interval : intervals) {
                for (Hcn hcn : interval.getHcnList()) {
                    ps.setLong(1, hcn.getHcnGenerator().getId());
                    ps.setInt(2, hcn.getLastActivePrime());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private Integer[] toIntegerArray(int[] arr) {
        if (arr == null) return null;
        Integer[] result = new Integer[arr.length];
        for (int i = 0; i < arr.length; i++) result[i] = arr[i];
        return result;
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) ps.setNull(index, Types.BIGINT);
        else ps.setLong(index, value);
    }
}
