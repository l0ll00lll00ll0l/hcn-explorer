package com.hcn.db;

import com.hcn.core.*;
import com.hcn.core.basicdata.Body;
import com.hcn.core.basicdata.Interval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class BasicDataService {

    @Autowired
    private DatabaseService databaseService;

    private int nextBasicDataId = 1;

    public int getNextBasicDataId() { return nextBasicDataId; }
    public void setNextBasicDataId(int id) { this.nextBasicDataId = id; }

    public int assignBasicDataId(HcnGenerator gen) {
        if (gen.getBasicDataId() == -1) gen.setBasicDataId(nextBasicDataId++);
        return gen.getBasicDataId();
    }

    public void flushAllIntervals(String dbName, List<Interval> intervals) {
        if (intervals.isEmpty() || dbName == null) return;

        try (Connection conn = databaseService.getConnection(dbName)) {
            conn.setAutoCommit(false);

            // Get next HCN id
            long nextHcnId;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(id), 0) FROM basic_data_hcn")) {
                rs.next();
                nextHcnId = rs.getLong(1) + 1;
            }

            List<HcnGenerator> newBodies = new ArrayList<>();

            try (PreparedStatement intervalPs = conn.prepareStatement(
                    "INSERT INTO basic_data_interval (lapi, value_mantissa, value_exponent, factor_mantissa, factor_exponent, reference_interval_lapi, starter_hcn_id) VALUES (?, ?, ?, ?, ?, ?, ?)");
                 PreparedStatement hcnPs = conn.prepareStatement(
                    "INSERT INTO basic_data_hcn (body_id, last_active_prime) VALUES (?, ?)");
                 PreparedStatement bodyPs = conn.prepareStatement(
                    "INSERT INTO basic_data_body (id, head, tail, body_chain) VALUES (?, ?, ?, ?)")) {

                for (Interval interval : intervals) {
                    intervalPs.setInt(1, interval.getLapi());
                    intervalPs.setDouble(2, interval.getValue().getMantissa());
                    intervalPs.setLong(3, interval.getValue().getExponent());
                    intervalPs.setDouble(4, interval.getFactor().getMantissa());
                    intervalPs.setLong(5, interval.getFactor().getExponent());
                    if (interval.getReferenceInterval() != null && interval.getReferenceInterval() != interval) {
                        intervalPs.setInt(6, interval.getReferenceInterval().getLapi());
                        intervalPs.setNull(7, Types.BIGINT);
                    } else {
                        intervalPs.setNull(6, Types.INTEGER);
                        intervalPs.setLong(7, nextHcnId);
                        for (Hcn hcn : interval.getHcnList()) {
                            boolean isNew = hcn.getHcnGenerator().getBasicDataId() == -1;
                            int bodyId = assignBasicDataId(hcn.getHcnGenerator());
                            hcnPs.setInt(1, bodyId);
                            hcnPs.setInt(2, hcn.getLastActivePrime());
                            hcnPs.addBatch();

                            if (isNew) {
                                newBodies.add(hcn.getHcnGenerator());
                            }
                        }
                        nextHcnId += interval.getHcnList().size();
                    }
                    intervalPs.addBatch();
                }

                for (HcnGenerator gen : newBodies) {
                    Body body = gen.getBody();
                    bodyPs.setInt(1, gen.getBasicDataId());
                    bodyPs.setArray(2, conn.createArrayOf("integer", toIntegerArray(body.getHead())));
                    bodyPs.setArray(3, conn.createArrayOf("integer", toIntegerArray(body.getTail())));
                    bodyPs.setString(4, gen.getCurrentHcnBody().parentChainString());
                    bodyPs.addBatch();
                }

                intervalPs.executeBatch();
                hcnPs.executeBatch();
                bodyPs.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Integer[] toIntegerArray(int[] arr) {
        if (arr == null) return null;
        Integer[] result = new Integer[arr.length];
        for (int i = 0; i < arr.length; i++) result[i] = arr[i];
        return result;
    }
}
