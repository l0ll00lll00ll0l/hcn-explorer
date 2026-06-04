package com.hcn.db;

import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class DatabaseService {

    private static final String PREFIX = "hcn_";
    private static final String ADMIN_URL = "jdbc:postgresql://localhost:5433/hcn";
    private static final String USER = "hcn";
    private static final String PASS = "hcn";

    public List<DatabaseInfo> listDatabases() {
        List<DatabaseInfo> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(ADMIN_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT datname FROM pg_database WHERE datname LIKE 'hcn_%'")) {
            while (rs.next()) {
                String name = rs.getString(1);
                if (name.substring(PREFIX.length()).matches("\\d+")) {
                    DatabaseInfo info = loadInfo(name);
                    if (info != null) list.add(info);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        list.sort((a, b) -> Integer.compare(a.getNumber(), b.getNumber()));
        return list;
    }

    public String createDatabase() {
        int number = findNextNumber();
        String dbName = PREFIX + number;
        try (Connection conn = DriverManager.getConnection(ADMIN_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE " + dbName + " OWNER hcn");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create database: " + dbName, e);
        }
        return dbName;
    }

    public void createTempSchema(String dbName, boolean basicData) {
        try (Connection conn = getConnection(dbName);
             Statement stmt = conn.createStatement()) {
            ResultSet tables = conn.getMetaData().getTables(null, null, "temp_matrix", null);
            if (tables.next()) return; // already exists
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        createSchema(dbName, basicData);
    }

    public void deleteDatabase(String dbName) {
        if (!dbName.startsWith(PREFIX)) return;
        try (Connection conn = DriverManager.getConnection(ADMIN_URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            // terminate existing connections
            stmt.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='" + dbName + "'");
            stmt.execute("DROP DATABASE IF EXISTS " + dbName);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete database: " + dbName, e);
        }
    }

    public Connection getConnection(String dbName) throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://localhost:5433/" + dbName, USER, PASS);
    }

    private int findNextNumber() {
        try (Connection conn = DriverManager.getConnection(ADMIN_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT datname FROM pg_database WHERE datname LIKE 'hcn_%'")) {
            List<Integer> existing = new ArrayList<>();
            while (rs.next()) {
                String name = rs.getString(1);
                if (name.substring(PREFIX.length()).matches("\\d+")) {
                    existing.add(Integer.parseInt(name.substring(PREFIX.length())));
                }
            }
            for (int i = 1; ; i++) {
                if (!existing.contains(i)) return i;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private DatabaseInfo loadInfo(String dbName) {
        try (Connection conn = getConnection(dbName);
             Statement stmt = conn.createStatement()) {
            ResultSet tables = conn.getMetaData().getTables(null, null, "temp_matrix", null);
            if (!tables.next()) {
                return new DatabaseInfo(dbName, 0, 0, false);
            }
            ResultSet rs = stmt.executeQuery("SELECT proved_count, last_proved_prime_index, basic_data FROM temp_matrix LIMIT 1");
            if (rs.next()) {
                return new DatabaseInfo(dbName, rs.getInt(1), rs.getInt(2), rs.getBoolean(3));
            }
            return new DatabaseInfo(dbName, 0, 0, false);
        } catch (SQLException e) {
            return new DatabaseInfo(dbName, 0, 0, false);
        }
    }

    private void createSchema(String dbName, boolean basicData) {
        try (Connection conn = getConnection(dbName);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE temp_fixed_power_group (
                    id BIGINT PRIMARY KEY,
                    value_mantissa DOUBLE PRECISION,
                    value_exponent BIGINT,
                    factor_mantissa DOUBLE PRECISION,
                    factor_exponent BIGINT,
                    parent_prime_index_id BIGINT,
                    offspring_prime_index_id BIGINT
                )
            """);
            stmt.execute("""
                CREATE TABLE temp_active_prime_index (
                    id BIGINT PRIMARY KEY,
                    prime_index INT NOT NULL,
                    smallest_body_id BIGINT,
                    next_active_prime_index_id BIGINT,
                    parent_active_prime_index_id BIGINT,
                    offspring_fixed_power_group_id BIGINT,
                    parent_fixed_power_group_id BIGINT,
                    member_of_fixed_power_group_id BIGINT,
                    fixed_power_group_order INT
                )
            """);
            stmt.execute("""
                CREATE TABLE temp_prime_index_power (
                    id BIGINT PRIMARY KEY,
                    prime_index_id BIGINT NOT NULL,
                    power INT NOT NULL,
                    proved BOOLEAN DEFAULT FALSE
                )
            """);
            stmt.execute("""
                CREATE TABLE temp_hcn_body (
                    id BIGINT PRIMARY KEY,
                    parent_id BIGINT,
                    pip_id BIGINT,
                    proved BOOLEAN DEFAULT FALSE,
                    value_mantissa DOUBLE PRECISION,
                    value_exponent BIGINT,
                    factor_mantissa DOUBLE PRECISION,
                    factor_exponent BIGINT,
                    smaller_body_id BIGINT,
                    larger_body_id BIGINT,
                    last_generated_hcn_id BIGINT
                )
            """);
            stmt.execute("""
                CREATE TABLE temp_hcn (
                    id BIGINT PRIMARY KEY,
                    body_id BIGINT,
                    last_active_prime INT,
                    value_mantissa DOUBLE PRECISION,
                    value_exponent BIGINT,
                    factor_mantissa DOUBLE PRECISION,
                    factor_exponent BIGINT
                )
            """);
            stmt.execute("""
                CREATE TABLE temp_last_active_prime_index_group (
                    id BIGINT PRIMARY KEY,
                    last_active_prime_index INT,
                    walker_body_id BIGINT,
                    lower_lapi_group_id BIGINT,
                    higher_lapi_group_id BIGINT
                )
            """);
            stmt.execute("""
                CREATE TABLE temp_lapi_hcn_list (
                    lapi_group_id BIGINT,
                    hcn_id BIGINT,
                    order_in_list INT,
                    PRIMARY KEY (lapi_group_id, hcn_id)
                )
            """);
            stmt.execute("""
                CREATE TABLE temp_matrix (
                    id BIGINT PRIMARY KEY,
                    last_active_prime_index_id BIGINT,
                    lowest_lapi_group_id BIGINT,
                    highest_lapi_group_id BIGINT,
                    next_lapi_group_id BIGINT,
                    proved_limit_mantissa DOUBLE PRECISION,
                    proved_limit_exponent BIGINT,
                    proved_count INT DEFAULT 0,
                    last_proved_prime_index INT DEFAULT -1,
                    lowest_proved_lapi_within_interval INT DEFAULT 1,
                    basic_data BOOLEAN DEFAULT FALSE,
                    total_nanos BIGINT DEFAULT 0,
                    extend_matrix_nanos BIGINT DEFAULT 0,
                    generate_hcn_list_nanos BIGINT DEFAULT 0
                )
            """);
            if (basicData) {
                stmt.execute("ALTER TABLE temp_hcn_body ADD COLUMN basic_data_id INT DEFAULT -1");
                stmt.execute("ALTER TABLE temp_matrix ADD COLUMN db_nanos BIGINT DEFAULT 0");
                stmt.execute("ALTER TABLE temp_matrix ADD COLUMN next_basic_data_id INT DEFAULT 1");
                stmt.execute("ALTER TABLE temp_matrix ADD COLUMN reference_interval_lapi INT");
                stmt.execute("ALTER TABLE temp_matrix ADD COLUMN reference_interval_value_mantissa DOUBLE PRECISION");
                stmt.execute("ALTER TABLE temp_matrix ADD COLUMN reference_interval_value_exponent BIGINT");
                stmt.execute("ALTER TABLE temp_matrix ADD COLUMN reference_interval_factor_mantissa DOUBLE PRECISION");
                stmt.execute("ALTER TABLE temp_matrix ADD COLUMN reference_interval_factor_exponent BIGINT");
                stmt.execute("""
                    CREATE TABLE temp_reference_hcn (
                        order_in_list INT PRIMARY KEY,
                        hcn_id BIGINT
                    )
                """);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create schema in: " + dbName, e);
        }
    }

    public void createBasicDataTables(String dbName) {
        try (Connection conn = getConnection(dbName);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS basic_data_interval (
                    lapi INT PRIMARY KEY,
                    value_mantissa DOUBLE PRECISION,
                    value_exponent BIGINT,
                    factor_mantissa DOUBLE PRECISION,
                    factor_exponent BIGINT,
                    reference_interval_lapi INT,
                    starter_hcn_id BIGINT
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS basic_data_hcn (
                    id BIGSERIAL PRIMARY KEY,
                    body_id BIGINT,
                    last_active_prime INT
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS basic_data_body (
                    id BIGINT PRIMARY KEY,
                    head INT[],
                    tail INT[]
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create basic data tables in: " + dbName, e);
        }
    }

    public static class DatabaseInfo {
        private final String name;
        private final int provedCount;
        private final int lastProvedPrimeIndex;
        private final boolean basicData;

        public DatabaseInfo(String name, int provedCount, int lastProvedPrimeIndex, boolean basicData) {
            this.name = name;
            this.provedCount = provedCount;
            this.lastProvedPrimeIndex = lastProvedPrimeIndex;
            this.basicData = basicData;
        }

        public String getName() { return name; }
        public int getProvedCount() { return provedCount; }
        public int getLastProvedPrimeIndex() { return lastProvedPrimeIndex; }
        public boolean isBasicData() { return basicData; }
        public int getNumber() { return Integer.parseInt(name.substring(PREFIX.length())); }
    }
}
