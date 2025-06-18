/**
 * Example program that accesses a PostgreSQL database.
 * Demonstrates simple queries for time series data as provided by the
 * Python import script.
 *
 * This class can be executed standalone.
 */

import java.sql.*;
import java.time.*;
import java.util.*;

public class DBAbfrage {

    /**
     * Small helper class for creating and automatically closing a JDBC connection.
     * Default values for URL, user name and password can be adjusted via parameters.
     */
    // Simplified database connection using a builder pattern
    public static class DBConnection implements AutoCloseable {
        private Connection connection;
        private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/simdata";
        private static final String DEFAULT_USER = "user";
        private static final String DEFAULT_PASSWORD = "password";

        public DBConnection() throws SQLException, ClassNotFoundException {
            this(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
        }

        public DBConnection(String url, String user, String password) throws SQLException, ClassNotFoundException {
            Class.forName("org.postgresql.Driver");
            this.connection = DriverManager.getConnection(url, user, password);
        }

        public Connection getConnection() {
            return connection;
        }

        @Override
        public void close() throws SQLException {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
    }

    // Simplified query method for time series data
    public static List<Object[]> getTimeSeriesData(Connection conn, String tableName,
                                                   String timeColumn, List<String> dataColumns,
                                                   LocalDateTime start, LocalDateTime end) throws SQLException {
        // Convert using the UTC time zone
        Timestamp startTs = timestampFromLocalDateTime(start);
        Timestamp endTs = timestampFromLocalDateTime(end);

        return getTimeSeriesData(conn, tableName, timeColumn, dataColumns, startTs, endTs);
    }

    // Overloaded method using timestamps
    public static List<Object[]> getTimeSeriesData(Connection conn, String tableName,
                                                   String timeColumn, List<String> dataColumns,
                                                   Timestamp start, Timestamp end) throws SQLException {
        String sanitizedTable = sanitizeIdentifier(tableName);
        String sanitizedTimeColumn = sanitizeIdentifier(timeColumn);

        StringBuilder columnBuilder = new StringBuilder();
        columnBuilder.append('"').append(sanitizedTimeColumn).append('"');

        List<String> sanitizedColumns = new ArrayList<>();
        for (String col : dataColumns) {
            String sanitized = sanitizeIdentifier(col);
            sanitizedColumns.add(sanitized);
            columnBuilder.append(", \"").append(sanitized).append("\"");
        }

        String sql = "SELECT " + columnBuilder + " FROM \"" + sanitizedTable + "\"" +
                " WHERE \"" + sanitizedTimeColumn + "\" >= ? AND \"" + sanitizedTimeColumn + "\" <= ?" +
                " ORDER BY \"" + sanitizedTimeColumn + "\" ASC";

        List<Object[]> results = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, start);
            ps.setTimestamp(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[1 + sanitizedColumns.size()];
                    row[0] = rs.getTimestamp(1);
                    for (int i = 0; i < sanitizedColumns.size(); i++) {
                        row[i + 1] = rs.getObject(i + 2);
                    }
                    results.add(row);
                }
            }
        }

        return results;
    }

    // Helper method for a single column
    public static List<Object[]> getTimeSeriesData(Connection conn, String tableName,
                                                   String timeColumn, String dataColumn,
                                                   LocalDateTime start, LocalDateTime end) throws SQLException {
        return getTimeSeriesData(conn, tableName, timeColumn, List.of(dataColumn), start, end);
    }

    // Helper for retrieving the last value before a given timestamp
    public static Object getLatestValueBefore(Connection conn, String tableName,
                                              String timeColumn, String dataColumn,
                                              LocalDateTime time) throws SQLException {
        Timestamp ts = timestampFromLocalDateTime(time);
        String sanitizedTable = sanitizeIdentifier(tableName);
        String sanitizedTimeColumn = sanitizeIdentifier(timeColumn);
        String sanitizedDataColumn = sanitizeIdentifier(dataColumn);

        String sql = "SELECT \"" + sanitizedDataColumn + "\" FROM \"" + sanitizedTable + "\"" +
                " WHERE \"" + sanitizedTimeColumn + "\" <= ? ORDER BY \"" + sanitizedTimeColumn + "\" DESC LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, ts);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject(1);
                }
            }
        }
        return null;
    }

    // Helper for timestamp conversion (uses UTC)
    private static Timestamp timestampFromLocalDateTime(LocalDateTime ldt) {
        // Convert to UTC for the database
        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC"));
        return Timestamp.from(zdt.toInstant());
    }

    // Helper for SQL injection protection
    private static String sanitizeIdentifier(String identifier) {
        return identifier.replaceAll("[^a-zA-Z0-9_]", "");
    }

    // Helper for formatting the output
    public static void printTimeSeriesData(List<Object[]> data, String title, String valueLabel) {
        System.out.println("\n" + title);

        if (data.isEmpty()) {
            System.out.println("📢 No data found.");
            return;
        }

        double total = 0.0;
        for (Object[] row : data) {
            Timestamp ts = (Timestamp)row[0];
            Object value = row[1];
            System.out.println(ts + " -> " + value);

            if (value instanceof Number) {
                total += ((Number)value).doubleValue();
            }
        }

        System.out.println("Total " + valueLabel + ": " + String.format("%.2f", total));
    }

    public static void main(String[] args) {
        try (DBConnection dbConn = new DBConnection()) {
            Connection conn = dbConn.getConnection();
            System.out.println("✅ Successfully connected to the PostgreSQL database!");

            // Query PV data
            LocalDateTime startPV = LocalDateTime.of(2016, 1, 1, 0, 0);
            LocalDateTime endPV = LocalDateTime.of(2016, 12, 1, 1, 0);

            List<Object[]> pvData = getTimeSeriesData(conn, "pv", "Time", "kWh", startPV, endPV);
            Object latestKwh = getLatestValueBefore(conn, "pv", "Time", "kWh", endPV);

            System.out.println("Table: pv");
            System.out.println("Period: " + startPV + " to " + endPV);
            System.out.println("Current kWh value: " + latestKwh);
            printTimeSeriesData(pvData, "PV data:", "kWh");

            // Query household data
            LocalDateTime startHH = LocalDateTime.of(2016, 1, 1, 1, 0);
            LocalDateTime endHH = LocalDateTime.of(2016, 1, 1, 2, 0);

            List<Object[]> hhData = getTimeSeriesData(conn, "household_data", "utc_timestamp",
                    "average_per_person_consumption", startHH, endHH);
            printTimeSeriesData(hhData, "Household data:", "Average consumption per person");

        } catch (ClassNotFoundException e) {
            System.err.println("🚨 PostgreSQL JDBC driver not found.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("🚨 Database connection failed!");
            e.printStackTrace();
        }
    }
}