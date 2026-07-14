package com.hcn.db;

import com.hcn.event.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityReadService {

    @Autowired
    private DatabaseService databaseService;

    private JdbcTemplate t(String dbName) {
        return databaseService.createTemplateForDb(dbName);
    }

    public List<MatrixMainActivity> getMatrixMainActivities(String dbName) {
        return t(dbName).query("SELECT start_nanos, finish_nanos, int_1, int_2 FROM structural_activity WHERE type='MATRIX_MAIN' ORDER BY id",
                (rs, i) -> {
                    MatrixMainActivity a = new MatrixMainActivity(rs.getInt("int_1"), rs.getLong("start_nanos"), rs.getLong("finish_nanos"));
                    a.setLastLapi(rs.getInt("int_2"));
                    return a;
                });
    }

    public List<ApiNodeCreationActivity> getApiNodeCreationActivities(String dbName) {
        return t(dbName).query("SELECT start_nanos, finish_nanos, int_1 FROM structural_activity WHERE type='API_NODE' ORDER BY id",
                (rs, i) -> new ApiNodeCreationActivity(rs.getInt("int_1"), rs.getLong("start_nanos"), rs.getLong("finish_nanos")));
    }

    public List<TransitionNodeCreationActivity> getTransitionNodeCreationActivities(String dbName) {
        return t(dbName).query("SELECT start_nanos, finish_nanos, int_1 FROM structural_activity WHERE type='TRANSITION_NODE' ORDER BY id",
                (rs, i) -> new TransitionNodeCreationActivity(rs.getInt("int_1"), rs.getLong("start_nanos"), rs.getLong("finish_nanos")));
    }

    public List<MatrixExtensionActivity> getMatrixExtensionActivities(String dbName) {
        return t(dbName).query("SELECT start_nanos, finish_nanos, index, power, created_active_body_count, deactivated_body_count FROM extension_activity ORDER BY id",
                (rs, i) -> new MatrixExtensionActivity(rs.getInt("index"), rs.getInt("power"),
                        rs.getInt("created_active_body_count"), rs.getInt("deactivated_body_count"),
                        rs.getLong("start_nanos"), rs.getLong("finish_nanos")));
    }

    public List<HcnGenerationActivity> getHcnGenerationActivities(String dbName) {
        return t(dbName).query("SELECT start_nanos, finish_nanos, start_lapi, end_lapi FROM hcn_generation_activity ORDER BY id",
                (rs, i) -> new HcnGenerationActivity(rs.getInt("start_lapi"), rs.getInt("end_lapi"),
                        rs.getLong("start_nanos"), rs.getLong("finish_nanos")));
    }

    public List<SqlInsertActivity> getSqlInsertActivities(String dbName) {
        return t(dbName).query("SELECT start_nanos, finish_nanos, row_count, table_name FROM sql_insert_activity ORDER BY id",
                (rs, i) -> new SqlInsertActivity(SqlTable.valueOf(rs.getString("table_name")),
                        rs.getInt("row_count"), rs.getLong("start_nanos"), rs.getLong("finish_nanos")));
    }
}
