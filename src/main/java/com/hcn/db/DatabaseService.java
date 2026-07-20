package com.hcn.db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${spring.datasource.url}")
    private String baseUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    public List<String> listHcnDatabases() {
        return jdbcTemplate.queryForList(
                "SELECT datname FROM pg_database WHERE datname LIKE 'hcn_%' ORDER BY datname", String.class);
    }

    public String assignDbName() {
        List<String> existing = listHcnDatabases();
        int i = 1;
        while (existing.contains("hcn_" + i)) {
            i++;
        }
        return "hcn_" + i;
    }

    public void deleteDatabase(String dbName) {
        if (dbName != null && dbName.matches("hcn_\\d+")) {
            jdbcTemplate.execute("DROP DATABASE IF EXISTS " + dbName);
        }
    }

    public boolean isPostgresRunning() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean databaseExists(String dbName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_database WHERE datname = ?", Integer.class, dbName) > 0;
    }

    public void createDatabase(String dbName) {
        jdbcTemplate.execute("CREATE DATABASE " + dbName);
        createTables(dbName);
    }

    public void truncateTmpTables(String dbName) {
        JdbcTemplate dbTemplate = createTemplateForDb(dbName);
        dbTemplate.execute("DROP TABLE IF EXISTS tmp_matrix, tmp_matrix_node, tmp_prime, tmp_body_node, tmp_body, tmp_hcn, tmp_lapi, tmp_lapi_hcn, tmp_reference_interval_hcn");
        createTables(dbName);
    }

    private void createTables(String dbName) {
        JdbcTemplate dbTemplate = createTemplateForDb(dbName);
        dbTemplate.execute("""
                CREATE TABLE tmp_matrix_node (
                    id INT PRIMARY KEY,
                    prev_matrix_node INT,
                    next_matrix_node INT,
                    body_list INT,
                    transition_from INT,
                    transition_to INT
                )
                """);
        dbTemplate.execute("""
                CREATE TABLE tmp_prime (
                    index INT PRIMARY KEY,
                    int_value INT,
                    value_mantissa DOUBLE PRECISION,
                    value_exponent BIGINT,
                    matrix_node_id INT
                )
                """);
        dbTemplate.execute("""
                CREATE TABLE tmp_body_node (
                    id INT PRIMARY KEY,
                    parent_node INT,
                    body_node_id INT,
                    proved BOOLEAN,
                    value_mantissa DOUBLE PRECISION,
                    value_exponent BIGINT,
                    factor_mantissa DOUBLE PRECISION,
                    factor_exponent BIGINT,
                    active BOOLEAN
                )
                """);
        dbTemplate.execute("""
                CREATE TABLE tmp_body (
                    id INT PRIMARY KEY,
                    body_node INT,
                    value_mantissa DOUBLE PRECISION,
                    value_exponent BIGINT,
                    factor_mantissa DOUBLE PRECISION,
                    factor_exponent BIGINT,
                    parent INT,
                    proved BOOLEAN,
                    smaller_body INT,
                    larger_body INT,
                    smaller_active_body INT,
                    larger_active_body INT,
                    last_generated_hcn INT,
                    first_hcn INT,
                    first_superior_hcn INT,
                    first_dominated_hcn INT,
                    deactivated BOOLEAN,
                    db_id INT
                )
                """);
        dbTemplate.execute("""
                CREATE TABLE tmp_hcn (
                    id INT PRIMARY KEY,
                    body INT,
                    lapi INT,
                    value_mantissa DOUBLE PRECISION,
                    value_exponent BIGINT,
                    factor_mantissa DOUBLE PRECISION,
                    factor_exponent BIGINT
                )
                """);
        dbTemplate.execute("""
                CREATE TABLE tmp_lapi (
                    prime INT PRIMARY KEY,
                    lower_lapi INT,
                    higher_lapi INT,
                    walker INT,
                    value_multiplier_mantissa DOUBLE PRECISION,
                    value_multiplier_exponent BIGINT,
                    factor_multiplier_mantissa DOUBLE PRECISION,
                    factor_multiplier_exponent BIGINT
                )
                """);
        dbTemplate.execute("""
                CREATE TABLE tmp_lapi_hcn (
                    lapi_prime INT,
                    list_position INT,
                    hcn INT
                )
                """);
        dbTemplate.execute("""
                CREATE TABLE tmp_reference_interval_hcn (
                    list_position INT,
                    hcn INT
                )
                """);
        dbTemplate.execute("""
                CREATE TABLE tmp_matrix (
                    last_transition INT,
                    next_lapi INT,
                    lowest_lapi INT,
                    highest_lapi INT,
                    lowest_proved_lapi_within_interval INT,
                    proved_count INT,
                    proved_limit_mantissa DOUBLE PRECISION,
                    proved_limit_exponent BIGINT,
                    total_time_ms BIGINT,
                    matrix_maintain_time_ms BIGINT,
                    generate_hcn_list_time_ms BIGINT,
                    db_mode BOOLEAN,
                    total_nanos BIGINT,
                    reference_interval_lapi INT,
                    reference_interval_value_mantissa DOUBLE PRECISION,
                    reference_interval_value_exponent BIGINT,
                    reference_interval_factor_mantissa DOUBLE PRECISION,
                    reference_interval_factor_exponent BIGINT
                )
                """);
    }

    public JdbcTemplate createTemplateForDb(String dbName) {
        String url = baseUrl.replaceFirst("/[^/]*$", "/" + dbName);
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        return new JdbcTemplate(ds);
    }

    public void createPermanentTables(String dbName) {
        JdbcTemplate dbTemplate = createTemplateForDb(dbName);
        dbTemplate.execute("""
                CREATE TABLE IF NOT EXISTS body (
                    id INT PRIMARY KEY,
                    head INT[],
                    tail INT[]
                )
                """);
        dbTemplate.execute("""
                CREATE TABLE IF NOT EXISTS interval (
                    lapi INT PRIMARY KEY,
                    value_mantissa DOUBLE PRECISION,
                    value_exponent BIGINT,
                    factor_mantissa DOUBLE PRECISION,
                    factor_exponent BIGINT,
                    first_hcn INT,
                    size INT,
                    reference_interval INT
                )
                """);
        dbTemplate.execute("""
                CREATE TABLE IF NOT EXISTS hcn (
                    id BIGINT PRIMARY KEY,
                    body INT,
                    lapi INT
                )
                """);
        dbTemplate.execute("""
                CREATE TABLE IF NOT EXISTS structural_activity (
                    id INT PRIMARY KEY,
                    type VARCHAR(32),
                    start_nanos BIGINT,
                    finish_nanos BIGINT,
                    int_1 INT,
                    int_2 INT
                )
                """);
        dbTemplate.execute("""
                CREATE TABLE IF NOT EXISTS extension_activity (
                    id INT PRIMARY KEY,
                    start_nanos BIGINT,
                    finish_nanos BIGINT,
                    index INT,
                    power INT,
                    created_active_body_count INT,
                    deactivated_body_count INT
                )
                """);
        dbTemplate.execute("""
                CREATE TABLE IF NOT EXISTS hcn_generation_activity (
                    id INT PRIMARY KEY,
                    start_nanos BIGINT,
                    finish_nanos BIGINT,
                    start_lapi INT,
                    end_lapi INT
                )
                """);
        dbTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sql_insert_activity (
                    id INT PRIMARY KEY,
                    start_nanos BIGINT,
                    finish_nanos BIGINT,
                    row_count INT,
                    table_name VARCHAR(32)
                )
                """);
        dbTemplate.execute("""
                CREATE TABLE IF NOT EXISTS body_deletion_event (
                    id INT PRIMARY KEY,
                    nanos BIGINT,
                    deleted_body_count INT,
                    non_proved_body_count INT
                )
                """);
    }
}
