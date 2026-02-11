package org.codenbug.bench;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.h2.api.Trigger;

/**
 * H2 row-level trigger to maintain seat_layout_stats on INSERT into seat.
 *
 * Notes:
 * - This is for benchmarking only.
 * - H2 triggers are per-row and implemented in Java, so the write overhead will be higher than MySQL triggers.
 */
public class H2SeatLayoutStatsAfterInsertTrigger implements Trigger {

    private static final String UPDATE_SQL = "UPDATE seat_layout_stats " +
        "SET seat_count = seat_count + 1, " +
        "min_price = CASE WHEN seat_count = 0 THEN ? WHEN min_price > ? THEN ? ELSE min_price END, " +
        "max_price = CASE WHEN seat_count = 0 THEN ? WHEN max_price < ? THEN ? ELSE max_price END, " +
        "last_updated = CURRENT_TIMESTAMP " +
        "WHERE layout_id = ?";

    private static final String INSERT_SQL = "INSERT INTO seat_layout_stats " +
        "(layout_id, seat_count, min_price, max_price, last_updated) " +
        "VALUES (?, 1, ?, ?, CURRENT_TIMESTAMP)";

    private int amountIdx = 0;
    private int layoutIdIdx = 0;

    // Cache column index mapping per table definition (best-effort)
    private static final Map<String, int[]> INDEX_CACHE = new ConcurrentHashMap<>();

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before,
        int type) throws SQLException {
        // Determine column positions from INFORMATION_SCHEMA once.
        // Seat table is expected to have columns: amount, available, layout_id, grade, seat_id, signature (Hibernate order)
        // but we resolve dynamically to avoid depending on column order.
        String cacheKey = (schemaName == null ? "" : schemaName) + ":" + tableName;
        int[] idx = INDEX_CACHE.get(cacheKey);
        if (idx == null) {
            idx = resolveSeatColumnIndices(conn, tableName);
            INDEX_CACHE.put(cacheKey, idx);
        }
        this.amountIdx = idx[0];
        this.layoutIdIdx = idx[1];
    }

    private static int[] resolveSeatColumnIndices(Connection conn, String tableName) throws SQLException {
        // Defaults if resolution fails
        int amount = 0;
        int layoutId = 2;

        // H2 stores quoted identifiers case-sensitively; use LOWER() match.
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT COLUMN_NAME, ORDINAL_POSITION " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE lower(TABLE_NAME) = lower(?)")) {
            ps.setString(1, tableName);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString(1);
                    int pos = rs.getInt(2) - 1; // 0-based
                    if ("amount".equalsIgnoreCase(name)) {
                        amount = pos;
                    } else if ("layout_id".equalsIgnoreCase(name)) {
                        layoutId = pos;
                    }
                }
            }
        }

        return new int[] {amount, layoutId};
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        if (newRow == null) {
            return;
        }

        Object layoutIdObj = newRow[layoutIdIdx];
        Object amountObj = newRow[amountIdx];
        if (!(layoutIdObj instanceof Number) || !(amountObj instanceof Number)) {
            return;
        }

        long layoutId = ((Number) layoutIdObj).longValue();
        int amount = ((Number) amountObj).intValue();

        int updated;
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            ps.setInt(1, amount);
            ps.setInt(2, amount);
            ps.setInt(3, amount);
            ps.setInt(4, amount);
            ps.setInt(5, amount);
            ps.setInt(6, amount);
            ps.setLong(7, layoutId);
            updated = ps.executeUpdate();
        }

        if (updated == 0) {
            try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
                ps.setLong(1, layoutId);
                ps.setInt(2, amount);
                ps.setInt(3, amount);
                ps.executeUpdate();
            }
        }
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public void remove() {
        // no-op
    }
}
