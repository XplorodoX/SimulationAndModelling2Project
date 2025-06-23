/**
 * Example program that accesses a PostgreSQL database.
 * This class demonstrates how to retrieve and analyze time series data stored in PostgreSQL
 * by the Python import script.
 *
 * Key features:
 * - Connection handling using try-with-resources
 * - Parameterized queries to prevent SQL injection
 * - Time series data retrieval with timestamp filtering
 * - Helper methods for common query patterns
 *
 * This class can be executed standalone.
 */

import java.sql.*;
import java.time.*;
import java.util.*;

public class DBRequest {

    /**
     * Helper class that creates and automatically closes a JDBC connection.
     * Implements AutoCloseable for use with try-with-resources.
     * Default connection parameters can be overridden via constructor parameters.
     */
    public static class DBConnection implements AutoCloseable {
        private Connection connection;
        private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/simdata";
        private static final String DEFAULT_USER = "user";
        private static final String DEFAULT_PASSWORD = "password";

        /**
         * Creates a database connection with default parameters.
         *
         * @throws SQLException If a database access error occurs
         * @throws ClassNotFoundException If the PostgreSQL driver is not found
         */
        public DBConnection() throws SQLException, ClassNotFoundException {
            this(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
        }

        /**
         * Creates a database connection with custom parameters.
         *
         * @param url Database JDBC URL
         * @param user Database username
         * @param password Database password
         * @throws SQLException If a database access error occurs
         * @throws ClassNotFoundException If the PostgreSQL driver is not found
         */
        public DBConnection(String url, String user, String password) throws SQLException, ClassNotFoundException {
            Class.forName("org.postgresql.Driver");
            this.connection = DriverManager.getConnection(url, user, password);
        }

        /**
         * Returns the active database connection.
         *
         * @return The JDBC connection object
         */
        public Connection getConnection() {
            return connection;
        }

        /**
         * Closes the database connection if it's open.
         * Automatically called when used with try-with-resources.
         *
         * @throws SQLException If a database access error occurs
         */
        @Override
        public void close() throws SQLException {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
    }

    /**
     * Retrieves time series data from the specified table within a time range.
     * Converts LocalDateTime parameters to UTC timestamps for database queries.
     *
     * @param conn Database connection
     * @param tableName Name of the table to query
     * @param timeColumn Name of the timestamp column
     * @param dataColumns List of data columns to retrieve
     * @param start Start of the time range (inclusive)
     * @param end End of the time range (inclusive)
     * @return List of Object arrays containing timestamp and data values
     * @throws SQLException If a database access error occurs
     */
    public static List<Object[]> getTimeSeriesData(Connection conn, String tableName,
                                                   String timeColumn, List<String> dataColumns,
                                                   LocalDateTime start, LocalDateTime end) throws SQLException {
        // Convert using the UTC time zone
        Timestamp startTs = timestampFromLocalDateTime(start);
        Timestamp endTs = timestampFromLocalDateTime(end);

        return getTimeSeriesData(conn, tableName, timeColumn, dataColumns, startTs, endTs);
    }

    /**
     * Overloaded method for retrieving time series data using Timestamp objects.
     * Builds a parameterized SQL query with proper identifier sanitization.
     *
     * @param conn Database connection
     * @param tableName Name of the table to query
     * @param timeColumn Name of the timestamp column
     * @param dataColumns List of data columns to retrieve
     * @param start Start timestamp (inclusive)
     * @param end End timestamp (inclusive)
     * @return List of Object arrays containing timestamp and data values
     * @throws SQLException If a database access error occurs
     */
    public static List<Object[]> getTimeSeriesData(Connection conn, String tableName,
                                                   String timeColumn, List<String> dataColumns,
                                                   Timestamp start, Timestamp end) throws SQLException {
        // Sanitize identifiers to prevent SQL injection
        String sanitizedTable = sanitizeIdentifier(tableName);
        String sanitizedTimeColumn = sanitizeIdentifier(timeColumn);

        // Build column list for SELECT statement
        StringBuilder columnBuilder = new StringBuilder();
        columnBuilder.append('"').append(sanitizedTimeColumn).append('"');

        List<String> sanitizedColumns = new ArrayList<>();
        for (String col : dataColumns) {
            String sanitized = sanitizeIdentifier(col);
            sanitizedColumns.add(sanitized);
            columnBuilder.append(", \"").append(sanitized).append("\"");
        }

        // Create parameterized query with time range filter
        String sql = "SELECT " + columnBuilder + " FROM \"" + sanitizedTable + "\"" +
                " WHERE \"" + sanitizedTimeColumn + "\" >= ? AND \"" + sanitizedTimeColumn + "\" <= ?" +
                " ORDER BY \"" + sanitizedTimeColumn + "\" ASC";

        List<Object[]> results = new ArrayList<>();

        // Execute query and collect results
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

    /**
     * Convenience method for retrieving time series data with a single data column.
     *
     * @param conn Database connection
     * @param tableName Name of the table to query
     * @param timeColumn Name of the timestamp column
     * @param dataColumn Single data column to retrieve
     * @param start Start of the time range (inclusive)
     * @param end End of the time range (inclusive)
     * @return List of Object arrays containing timestamp and data value
     * @throws SQLException If a database access error occurs
     */
    public static List<Object[]> getTimeSeriesData(Connection conn, String tableName,
                                                   String timeColumn, String dataColumn,
                                                   LocalDateTime start, LocalDateTime end) throws SQLException {
        return getTimeSeriesData(conn, tableName, timeColumn, List.of(dataColumn), start, end);
    }

    /**
     * Retrieves the most recent value of a specific column before a given timestamp.
     * Useful for getting the "current" value at a specific point in time.
     *
     * @param conn Database connection
     * @param tableName Name of the table to query
     * @param timeColumn Name of the timestamp column
     * @param dataColumn Data column to retrieve
     * @param time Reference timestamp
     * @return The most recent value before the given timestamp, or null if none exists
     * @throws SQLException If a database access error occurs
     */
    public static Object getActulValue(Connection conn, String tableName,
                                              String timeColumn, String dataColumn,
                                              LocalDateTime time) throws SQLException {
        Timestamp ts = timestampFromLocalDateTime(time);
        String sanitizedTable = sanitizeIdentifier(tableName);
        String sanitizedTimeColumn = sanitizeIdentifier(timeColumn);
        String sanitizedDataColumn = sanitizeIdentifier(dataColumn);

        // Query for the most recent value before the specified time
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

    /**
     * Converts a LocalDateTime to a UTC Timestamp for database storage.
     * Ensures consistent time zone handling across the application.
     *
     * @param ldt LocalDateTime to convert
     * @return SQL Timestamp in UTC
     */
    private static Timestamp timestampFromLocalDateTime(LocalDateTime ldt) {
        // Convert to UTC for the database
        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC"));
        return Timestamp.from(zdt.toInstant());
    }

    /**
     * Sanitizes a database identifier to prevent SQL injection.
     * Removes any non-alphanumeric/underscore characters.
     *
     * @param identifier Database identifier to sanitize
     * @return Sanitized identifier
     */
    private static String sanitizeIdentifier(String identifier) {
        return identifier.replaceAll("[^a-zA-Z0-9_]", "");
    }

    /**
     * Utility method to print time series data in a readable format.
     * Also calculates totals for numeric values.
     *
     * @param data List of data rows to print
     * @param title Title to display before the data
     * @param valueLabel Label for the value total
     */
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

            // Calculate running total for numeric values
            if (value instanceof Number) {
                total += ((Number)value).doubleValue();
            }
        }

        System.out.println("Total " + valueLabel + ": " + String.format("%.2f", total));
    }

    /**
     * Main method demonstrating database queries and time series analysis.
     * Shows examples of querying PV and household consumption data.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        try (DBConnection dbConn = new DBConnection()) {
            Connection conn = dbConn.getConnection();
            System.out.println("✅ Successfully connected to the PostgreSQL database!");

            // Example 1: Query photovoltaic (PV) energy generation data for a year
            LocalDateTime startPV = LocalDateTime.of(2016, 1, 1, 0, 0);
            LocalDateTime endPV = LocalDateTime.of(2016, 12, 1, 1, 0);

            List<Object[]> pvData = getTimeSeriesData(conn, "pv", "Time", "kWh", startPV, endPV);
            Object latestKwh = getActulValue(conn, "pv", "Time", "kWh", endPV);

            System.out.println("Table: pv");
            System.out.println("Period: " + startPV + " to " + endPV);
            System.out.println("Current kWh value: " + latestKwh);
            printTimeSeriesData(pvData, "PV data:", "kWh");

            // Example 2: Query household consumption data for an hour
            LocalDateTime startHH = LocalDateTime.of(2016, 1, 1, 1, 0);
            LocalDateTime endHH = LocalDateTime.of(2016, 1, 1, 2, 0);

            List<Object[]> hhData = getTimeSeriesData(conn, "household_data", "utc_timestamp",
                    "average_per_person_consumption", startHH, endHH);
            printTimeSeriesData(hhData, "Household data:", "Average consumption per person");

            // Example 2: Query household consumption data for an hour
            LocalDateTime startHHS = LocalDateTime.of(2015, 1, 1, 1, 0);
            LocalDateTime endHHS = LocalDateTime.of(2016, 1, 1, 2, 0);

            List<Object[]> shData = getTimeSeriesData(conn, "price", "Time",
                    "price_kWh", startHHS, endHHS);
            printTimeSeriesData(shData, "Price data:", "Average price per person");


        } catch (ClassNotFoundException e) {
            System.err.println("🚨 PostgreSQL JDBC driver not found.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("🚨 Database connection failed!");
            e.printStackTrace();
        }
    }
}