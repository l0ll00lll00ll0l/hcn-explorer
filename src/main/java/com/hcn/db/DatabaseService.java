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
        createSchema(dbName);
        return dbName;
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
            ResultSet tables = conn.getMetaData().getTables(null, null, "matrix", null);
            if (!tables.next()) {
                return new DatabaseInfo(dbName, 0, 0, "detailed");
            }
            ResultSet rs = stmt.executeQuery("SELECT proved_count, last_proved_prime_index, mode FROM matrix LIMIT 1");
            if (rs.next()) {
                return new DatabaseInfo(dbName, rs.getInt(1), rs.getInt(2), rs.getString(3));
            }
            return new DatabaseInfo(dbName, 0, 0, "detailed");
        } catch (SQLException e) {
            return new DatabaseInfo(dbName, 0, 0, "detailed");
        }
    }

    private void createSchema(String dbName) {
        try (Connection conn = getConnection(dbName);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE scientific_number (
                    id BIGSERIAL PRIMARY KEY,
                    mantissa DOUBLE PRECISION,
                    exponent BIGINT
                )
            """);
            stmt.execute("""
                CREATE TABLE fixed_power_group (
                    id BIGSERIAL PRIMARY KEY,
                    value_id BIGINT REFERENCES scientific_number(id),
                    factor_id BIGINT REFERENCES scientific_number(id),
                    parent_prime_index_id BIGINT,
                    offspring_prime_index_id BIGINT
                )
            """);
            stmt.execute("""
                CREATE TABLE active_prime_index (
                    id BIGSERIAL PRIMARY KEY,
                    prime_index INT NOT NULL,
                    smallest_body_id BIGINT,
                    next_active_prime_index_id BIGINT REFERENCES active_prime_index(id),
                    parent_active_prime_index_id BIGINT REFERENCES active_prime_index(id),
                    offspring_fixed_power_group_id BIGINT REFERENCES fixed_power_group(id),
                    parent_fixed_power_group_id BIGINT REFERENCES fixed_power_group(id),
                    member_of_fixed_power_group_id BIGINT REFERENCES fixed_power_group(id),
                    fixed_power_group_order INT
                )
            """);
            stmt.execute("""
                ALTER TABLE fixed_power_group
                    ADD CONSTRAINT fk_fpg_parent_pi FOREIGN KEY (parent_prime_index_id) REFERENCES active_prime_index(id),
                    ADD CONSTRAINT fk_fpg_offspring_pi FOREIGN KEY (offspring_prime_index_id) REFERENCES active_prime_index(id)
            """);
            stmt.execute("""
                CREATE TABLE prime_index_power (
                    id BIGSERIAL PRIMARY KEY,
                    prime_index_id BIGINT NOT NULL REFERENCES active_prime_index(id),
                    power INT NOT NULL,
                    proved BOOLEAN DEFAULT FALSE
                )
            """);
            stmt.execute("""
                CREATE TABLE hcn_body (
                    id BIGSERIAL PRIMARY KEY,
                    parent_id BIGINT REFERENCES hcn_body(id),
                    pip_id BIGINT REFERENCES prime_index_power(id),
                    proved BOOLEAN DEFAULT FALSE,
                    value_id BIGINT REFERENCES scientific_number(id),
                    factor_id BIGINT REFERENCES scientific_number(id),
                    smaller_body_id BIGINT REFERENCES hcn_body(id),
                    larger_body_id BIGINT REFERENCES hcn_body(id),
                    last_generated_hcn_id BIGINT
                )
            """);
            stmt.execute("""
                ALTER TABLE active_prime_index
                    ADD CONSTRAINT fk_api_smallest_body FOREIGN KEY (smallest_body_id) REFERENCES hcn_body(id)
            """);
            stmt.execute("""
                CREATE TABLE hcn (
                    id BIGSERIAL PRIMARY KEY,
                    body_id BIGINT REFERENCES hcn_body(id),
                    last_active_prime INT,
                    value_id BIGINT REFERENCES scientific_number(id),
                    factor_id BIGINT REFERENCES scientific_number(id)
                )
            """);
            stmt.execute("""
                ALTER TABLE hcn_body
                    ADD CONSTRAINT fk_body_last_gen_hcn FOREIGN KEY (last_generated_hcn_id) REFERENCES hcn(id)
            """);
            stmt.execute("""
                CREATE TABLE last_active_prime_index_group (
                    id BIGSERIAL PRIMARY KEY,
                    last_active_prime_index INT,
                    walker_body_id BIGINT REFERENCES hcn_body(id),
                    lower_lapi_group_id BIGINT REFERENCES last_active_prime_index_group(id),
                    higher_lapi_group_id BIGINT REFERENCES last_active_prime_index_group(id)
                )
            """);
            stmt.execute("""
                CREATE TABLE lapi_hcn_list (
                    lapi_group_id BIGINT REFERENCES last_active_prime_index_group(id),
                    hcn_id BIGINT REFERENCES hcn(id),
                    order_in_list INT,
                    PRIMARY KEY (lapi_group_id, hcn_id)
                )
            """);
            stmt.execute("""
                CREATE TABLE matrix (
                    id BIGSERIAL PRIMARY KEY,
                    mode VARCHAR(10) NOT NULL DEFAULT 'detailed',
                    last_active_prime_index_id BIGINT REFERENCES active_prime_index(id),
                    lowest_lapi_group_id BIGINT REFERENCES last_active_prime_index_group(id),
                    highest_lapi_group_id BIGINT REFERENCES last_active_prime_index_group(id),
                    next_lapi_group_id BIGINT REFERENCES last_active_prime_index_group(id),
                    proved_limit_id BIGINT REFERENCES scientific_number(id),
                    proved_count INT DEFAULT 0,
                    last_proved_prime_index INT DEFAULT -1,
                    lowest_proved_lapi_within_interval INT DEFAULT 1
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create schema in: " + dbName, e);
        }
    }

    public static class DatabaseInfo {
        private final String name;
        private final int provedCount;
        private final int lastProvedPrimeIndex;
        private final String mode;

        public DatabaseInfo(String name, int provedCount, int lastProvedPrimeIndex, String mode) {
            this.name = name;
            this.provedCount = provedCount;
            this.lastProvedPrimeIndex = lastProvedPrimeIndex;
            this.mode = mode;
        }

        public String getName() { return name; }
        public int getProvedCount() { return provedCount; }
        public int getLastProvedPrimeIndex() { return lastProvedPrimeIndex; }
        public String getMode() { return mode; }
        public int getNumber() { return Integer.parseInt(name.substring(PREFIX.length())); }
    }
}
