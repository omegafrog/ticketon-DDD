package org.codenbug.bench;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MaterializedTriggerBenchmarkMain {

    private static final int DEFAULT_LAYOUTS = 10_000;
    private static final int DEFAULT_EVENTS = 100_000;
    private static final int DEFAULT_SEATS = 10_000_000;
    private static final int DEFAULT_QUERY_REPEATS = 5;
    private static final int DEFAULT_BATCH_SIZE = 10_000;
    private static final int DEFAULT_PROGRESS_EVERY = 1_000_000;

    // H2: keep identifiers lower-case to match MySQL-ish DDL
    private static final String DEFAULT_JDBC_URL =
        "jdbc:h2:file:./nplus1-test/build/bench/bench;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";

    public static void main(String[] args) throws Exception {
        Args a = Args.parse(args);
        Dialect d = Dialect.fromJdbcUrl(a.jdbcUrl);

        System.out.println("jdbcUrl=" + a.jdbcUrl);
        System.out.println("dialect=" + d);
        System.out.println("layouts=" + a.layouts + ", events=" + a.events + ", seats=" + a.seats + ", repeats=" + a.queryRepeats);
        System.out.println("batchSize=" + a.batchSize);
        System.out.println("progressEvery=" + a.progressEvery);
        System.out.println("only=" + a.only);
        System.out.println();

        // Scenario 1: No materialized stats table maintenance (query computes aggregation on the fly)
        if (a.only == Only.BOTH || a.only == Only.NO_MATERIALIZED) {
            runScenario(d, "NO_MATERIALIZED", a, false);
            System.out.println();
        }

        // Scenario 2: Materialized stats maintained by trigger (query joins to stats)
        if (a.only == Only.BOTH || a.only == Only.MATERIALIZED_TRIGGER) {
            runScenario(d, "MATERIALIZED_TRIGGER", a, true);
        }
    }

    private static void runScenario(Dialect d, String name, Args a, boolean withTrigger) throws SQLException {
        try (Connection conn = DriverManager.getConnection(a.jdbcUrl, a.username, a.password)) {
            conn.setAutoCommit(false);

            System.out.println("=== " + name + " ===");
            recreateSchema(d, conn);

            if (withTrigger) {
                installTrigger(d, conn);
            }

            long t0 = System.nanoTime();
            insertSeatLayouts(conn, a.layouts, a.batchSize);
            insertEvents(conn, a.events, a.layouts, a.batchSize);
            insertSeats(conn, a.seats, a.layouts, a.batchSize, a.progressEvery);
            createIndexes(conn);
            conn.commit();
            long loadNanos = System.nanoTime() - t0;
            System.out.println("data_load=" + format(loadNanos));

            if (!withTrigger) {
                // Keep seat_layout_stats empty in this scenario on purpose.
                truncateSeatLayoutStats(conn);
            }

            // Warmup (1x)
            if (withTrigger) {
                execQueryJoinStats(conn);
            } else {
                execQueryCorrelatedSubqueries(conn);
            }

            List<Long> times = new ArrayList<>();
            for (int i = 0; i < a.queryRepeats; i++) {
                long q0 = System.nanoTime();
                if (withTrigger) {
                    execQueryJoinStats(conn);
                } else {
                    execQueryCorrelatedSubqueries(conn);
                }
                times.add(System.nanoTime() - q0);
            }

            long[] arr = times.stream().mapToLong(Long::longValue).toArray();
            Arrays.sort(arr);
            int p95Index = (int)Math.ceil(arr.length * 0.95) - 1;
            if (p95Index < 0) {
                p95Index = 0;
            }
            if (p95Index >= arr.length) {
                p95Index = arr.length - 1;
            }
            System.out.println("query_runs=" + a.queryRepeats);
            System.out.println("query_min=" + format(arr[0]));
            System.out.println("query_p50=" + format(arr[arr.length / 2]));
            System.out.println("query_p95=" + format(arr[p95Index]));
            System.out.println("query_max=" + format(arr[arr.length - 1]));
        }
    }

    private static void recreateSchema(Dialect d, Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            // Triggers first (some DBs require drop-before-table)
            st.execute("DROP TRIGGER IF EXISTS seat_ai_stats");

            st.execute("DROP TABLE IF EXISTS seat");
            st.execute("DROP TABLE IF EXISTS event");
            st.execute("DROP TABLE IF EXISTS seat_layout_stats");
            st.execute("DROP TABLE IF EXISTS seat_layout");

            if (d == Dialect.MYSQL) {
                st.execute("CREATE TABLE seat_layout (" +
                    "id BIGINT PRIMARY KEY, " +
                    "location VARCHAR(255) NOT NULL, " +
                    "hall_name VARCHAR(255) NOT NULL" +
                    ") ENGINE=InnoDB");

                st.execute("CREATE TABLE event (" +
                    "id VARCHAR(64) PRIMARY KEY, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "deleted BOOLEAN NOT NULL, " +
                    "created_at DATETIME(6) NOT NULL, " +
                    "seat_layout_id BIGINT NOT NULL" +
                    ") ENGINE=InnoDB");

                st.execute("CREATE TABLE seat (" +
                    "seat_id VARCHAR(64) PRIMARY KEY, " +
                    "layout_id BIGINT NOT NULL, " +
                    "amount INT NOT NULL" +
                    ") ENGINE=InnoDB");

                st.execute("CREATE TABLE seat_layout_stats (" +
                    "layout_id BIGINT PRIMARY KEY, " +
                    "seat_count INT NOT NULL DEFAULT 0, " +
                    "min_price INT NOT NULL DEFAULT 0, " +
                    "max_price INT NOT NULL DEFAULT 0, " +
                    "last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB");
            } else {
                st.execute("CREATE TABLE seat_layout (" +
                    "id BIGINT PRIMARY KEY, " +
                    "layout TEXT, " +
                    "location VARCHAR(255) NOT NULL, " +
                    "hall_name VARCHAR(255) NOT NULL" +
                    ")");

                st.execute("CREATE TABLE event (" +
                    "id VARCHAR(64) PRIMARY KEY, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "deleted BOOLEAN NOT NULL, " +
                    "created_at TIMESTAMP NOT NULL, " +
                    "seat_layout_id BIGINT NOT NULL" +
                    ")");

                st.execute("CREATE TABLE seat (" +
                    "seat_id VARCHAR(64) PRIMARY KEY, " +
                    "layout_id BIGINT NOT NULL, " +
                    "amount INT NOT NULL" +
                    ")");

                st.execute("CREATE TABLE seat_layout_stats (" +
                    "layout_id BIGINT PRIMARY KEY, " +
                    "seat_count INT NOT NULL, " +
                    "min_price INT NOT NULL, " +
                    "max_price INT NOT NULL, " +
                    "last_updated TIMESTAMP NOT NULL" +
                    ")");
            }
        }
        conn.commit();
    }

    private static void installTrigger(Dialect d, Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("DROP TRIGGER IF EXISTS seat_ai_stats");
            if (d == Dialect.MYSQL) {
                // Single-statement trigger body (no BEGIN/END) to avoid client delimiter issues.
                st.execute(
                    "CREATE TRIGGER seat_ai_stats AFTER INSERT ON seat FOR EACH ROW " +
                        "INSERT INTO seat_layout_stats (layout_id, seat_count, min_price, max_price) " +
                        "VALUES (NEW.layout_id, 1, NEW.amount, NEW.amount) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "seat_count = seat_count + 1, " +
                        "min_price = IF(min_price = 0, NEW.amount, LEAST(min_price, NEW.amount)), " +
                        "max_price = GREATEST(max_price, NEW.amount)"
                );
            } else {
                st.execute("CREATE TRIGGER seat_ai_stats AFTER INSERT ON seat FOR EACH ROW CALL \"" +
                    H2SeatLayoutStatsAfterInsertTrigger.class.getName() + "\"");
            }
        }
        conn.commit();
    }

    private static void insertSeatLayouts(Connection conn, int layouts, int batchSize) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO seat_layout (id, location, hall_name) VALUES (?, ?, ?)")
        ) {
            for (int i = 1; i <= layouts; i++) {
                ps.setLong(1, i);
                ps.setString(2, "Venue-" + i);
                ps.setString(3, "Hall-" + i);
                ps.addBatch();
                if (i % batchSize == 0) {
                    ps.executeBatch();
                    conn.commit();
                }
            }
            ps.executeBatch();
            conn.commit();
        }
    }

    private static void insertEvents(Connection conn, int events, int layouts, int batchSize) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO event (id, title, deleted, created_at, seat_layout_id) VALUES (?, ?, FALSE, CURRENT_TIMESTAMP(6), ?)")
        ) {
            for (int i = 1; i <= events; i++) {
                ps.setString(1, "evt-" + i);
                ps.setString(2, "Event " + i);
                ps.setLong(3, (i % layouts) + 1L);
                ps.addBatch();
                if (i % batchSize == 0) {
                    ps.executeBatch();
                    conn.commit();
                }
            }
            ps.executeBatch();
            conn.commit();
        }
    }

    private static void insertSeats(Connection conn, int seats, int layouts, int batchSize, int progressEvery) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO seat (seat_id, layout_id, amount) VALUES (?, ?, ?)")
        ) {
            for (int i = 1; i <= seats; i++) {
                ps.setString(1, "seat-" + i);
                ps.setLong(2, (i % layouts) + 1L);
                ps.setInt(3, 1000 + (i % 100_000));
                ps.addBatch();
                if (i % batchSize == 0) {
                    ps.executeBatch();
                    conn.commit();
                }
                if (progressEvery > 0 && i % progressEvery == 0) {
                    System.out.println("insert_seat_progress=" + i + "/" + seats);
                    System.out.flush();
                }
            }
            ps.executeBatch();
            conn.commit();
        }
    }

    private static void createIndexes(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE INDEX idx_seat_layout_amount ON seat(layout_id, amount)");
            st.execute("CREATE INDEX idx_event_deleted_created ON event(deleted, created_at)");
            st.execute("CREATE INDEX idx_event_layout ON event(seat_layout_id)");
        }
        conn.commit();
    }

    private static void truncateSeatLayoutStats(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("TRUNCATE TABLE seat_layout_stats");
        }
        conn.commit();
    }

    private static void execQueryJoinStats(Connection conn) throws SQLException {
        String sql = "SELECT e20.id, e20.title, COALESCE(sls.seat_count, 0) AS seat_count, " +
            "COALESCE(sls.min_price, 0) AS min_price, COALESCE(sls.max_price, 0) AS max_price " +
            "FROM (" +
            "  SELECT id, title, seat_layout_id, created_at" +
            "  FROM event" +
            "  WHERE deleted = FALSE" +
            "  ORDER BY created_at DESC" +
            "  LIMIT 20" +
            ") e20 " +
            "JOIN seat_layout sl ON sl.id = e20.seat_layout_id " +
            "LEFT JOIN seat_layout_stats sls ON sls.layout_id = e20.seat_layout_id " +
            "ORDER BY e20.created_at DESC";
        try (Statement st = conn.createStatement()) {
            st.executeQuery(sql).close();
        }
    }

    private static void execQueryCorrelatedSubqueries(Connection conn) throws SQLException {
        // Baseline without materialized stats: compute count/min/max via correlated subqueries
        // for the exact 20 events that would be returned in a page.
        String sql = "SELECT e20.id, e20.title, " +
            "(SELECT COUNT(*) FROM seat s2 WHERE s2.layout_id = e20.seat_layout_id) AS seat_count, " +
            "(SELECT MIN(amount) FROM seat s2 WHERE s2.layout_id = e20.seat_layout_id) AS min_price, " +
            "(SELECT MAX(amount) FROM seat s2 WHERE s2.layout_id = e20.seat_layout_id) AS max_price " +
            "FROM (" +
            "  SELECT id, title, seat_layout_id, created_at" +
            "  FROM event" +
            "  WHERE deleted = FALSE" +
            "  ORDER BY created_at DESC" +
            "  LIMIT 20" +
            ") e20 " +
            "JOIN seat_layout sl ON sl.id = e20.seat_layout_id " +
            "ORDER BY e20.created_at DESC";
        try (Statement st = conn.createStatement()) {
            st.executeQuery(sql).close();
        }
    }

    private static String format(long nanos) {
        if (nanos < 1_000_000) {
            long us = Math.max(1, nanos / 1_000);
            return us + "us";
        }
        Duration d = Duration.ofNanos(nanos);
        long ms = d.toMillis();
        if (ms < 1000) {
            return ms + "ms";
        }
        long s = ms / 1000;
        long remMs = ms % 1000;
        return s + "." + String.format("%03d", remMs) + "s";
    }

    private static final class Args {
        final String jdbcUrl;
        final String username;
        final String password;
        final int layouts;
        final int events;
        final int seats;
        final int queryRepeats;
        final int batchSize;
        final int progressEvery;
        final Only only;

        private Args(String jdbcUrl, String username, String password, int layouts, int events, int seats, int queryRepeats,
            int batchSize, int progressEvery, Only only) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
            this.layouts = layouts;
            this.events = events;
            this.seats = seats;
            this.queryRepeats = queryRepeats;
            this.batchSize = batchSize;
            this.progressEvery = progressEvery;
            this.only = only;
        }

        static Args parse(String[] args) {
            String jdbcUrl = value(args, "--jdbc", DEFAULT_JDBC_URL);
            String username = value(args, "--user", "sa");
            String password = value(args, "--pass", "");
            int layouts = intValue(args, "--layouts", DEFAULT_LAYOUTS);
            int events = intValue(args, "--events", DEFAULT_EVENTS);
            int seats = intValue(args, "--seats", DEFAULT_SEATS);
            int repeats = intValue(args, "--repeats", DEFAULT_QUERY_REPEATS);
            int batchSize = intValue(args, "--batch", DEFAULT_BATCH_SIZE);
            int progressEvery = intValue(args, "--progressEvery", DEFAULT_PROGRESS_EVERY);
            Only only = Only.from(value(args, "--only", "both"));
            return new Args(jdbcUrl, username, password, layouts, events, seats, repeats, batchSize, progressEvery, only);
        }

        private static String value(String[] args, String key, String def) {
            for (int i = 0; i < args.length; i++) {
                if (key.equals(args[i]) && i + 1 < args.length) {
                    return args[i + 1];
                }
            }
            return def;
        }

        private static int intValue(String[] args, String key, int def) {
            String v = value(args, key, null);
            if (v == null) {
                return def;
            }
            return Integer.parseInt(v);
        }
    }

    private enum Only {
        BOTH,
        NO_MATERIALIZED,
        MATERIALIZED_TRIGGER;

        static Only from(String raw) {
            if (raw == null) {
                return BOTH;
            }
            String v = raw.trim().toLowerCase();
            if (v.equals("no") || v.equals("no_materialized") || v.equals("nomaterialized")) {
                return NO_MATERIALIZED;
            }
            if (v.equals("trigger") || v.equals("materialized") || v.equals("materialized_trigger") || v.equals("with_trigger")) {
                return MATERIALIZED_TRIGGER;
            }
            return BOTH;
        }
    }

    private enum Dialect {
        H2,
        MYSQL;

        static Dialect fromJdbcUrl(String jdbcUrl) {
            if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:mysql:")) {
                return MYSQL;
            }
            return H2;
        }
    }
}
