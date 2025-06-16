import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class DBAbfrage {

    public static void main(String[] args) {
        // --- Database Connection Details ---
        String url = "jdbc:postgresql://localhost:5432/simdata";
        String user = "user";
        String password = "password";
        String tableName = "sample_table";

        Connection connection = null;

        try {
            // --- 1. Establish the Connection ---
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Successfully connected to the PostgreSQL database!");

            // --- 2. Use the Integrated Functions ---
            LocalDateTime endDateTime = LocalDateTime.of(2023, 12, 31, 23, 30, 0);
            LocalDateTime startDateTime = LocalDateTime.of(2023, 12, 31, 22, 45, 0);
            Timestamp endTime = Timestamp.valueOf(endDateTime);
            Timestamp startTime = Timestamp.valueOf(startDateTime);

            // We now expect a 'Double' object, which can be null.
            Double totalKwh = getDataAtTimeStampRange(connection, tableName, startTime, endTime);

            Double nowKwh = getActualAtTimeStampData(connection, tableName, "Time", endTime);

            System.out.println("Table: " + tableName);
            System.out.println("Time range: " + startTime + " to " + endTime);

            System.out.println("Now KWH: " + nowKwh);

            // --- 3. Check the result and give feedback ---
            if (totalKwh != null) {
                System.out.println("Total kWh consumed: " + String.format("%.2f", totalKwh));
            } else {
                // This message is shown if the function returned null.
                System.out.println("📢 No data found for the specified time range.");
            }


        } catch (ClassNotFoundException e) {
            System.err.println("🚨 PostgreSQL JDBC Driver not found. Include it in your library path.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("🚨 Connection Failed! Check output console.");
            e.printStackTrace();
        } finally {
            // --- 4. Close the Connection ---
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    System.out.println("\n🔌 Connection closed.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Changed to return Double to allow for a null return value.
     */
    public static Double getDataAtTimeStampRange(Connection conn,
                                                 String tableName,
                                                 Timestamp startTime,
                                                 Timestamp endTime) throws SQLException {
        return getDataAtTimeStampRange(conn, tableName, "Time", startTime, endTime);
    }

    /**
     * Changed to return Double. Returns null if no data is found.
     */
    public static Double getDataAtTimeStampRange(Connection conn,
                                                 String tableName,
                                                 String timestampColumn,
                                                 Timestamp startTime,
                                                 Timestamp endTime) throws SQLException {
        String sanitizedTable = sanitizeTableName(tableName);
        String sanitizedColumn = sanitizeColumnName("kWh");
        String sanitizedTimeColumn = sanitizeColumnName(timestampColumn);
        String sql = "SELECT SUM(\"" + sanitizedColumn + "\") FROM \"" + sanitizedTable + "\"" +
                " WHERE \"" + sanitizedTimeColumn + "\" >= ? AND \"" + sanitizedTimeColumn + "\" <= ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, startTime);
            ps.setTimestamp(2, endTime);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double total = rs.getDouble(1);
                    // The SUM aggregate returns NULL if there are no rows.
                    // rs.getDouble() returns 0.0 for NULL, so we must also check rs.wasNull().
                    if (rs.wasNull()) {
                        return null; // No rows were found to sum, so return null.
                    }
                    return total; // Return the calculated sum.
                }
            }
        }
        return null; // Should not be reached, but good practice.
    }

    // --- getActualAtTimeStampData and sanitize methods remain the same ---
    public static Double getActualAtTimeStampData(Connection conn,
                                                  String tableName,
                                                  String timestampColumn,
                                                  Timestamp time) throws SQLException {
        String sanitizedTable = sanitizeTableName(tableName);
        String sanitizedColumn = sanitizeColumnName("kWh");
        String sanitizedTimeColumn = sanitizeColumnName(timestampColumn);
        String sql = "SELECT \"" + sanitizedColumn + "\" FROM \"" + sanitizedTable + "\"" +
                " WHERE \"" + sanitizedTimeColumn + "\" <= ? ORDER BY \"" + sanitizedTimeColumn + "\" DESC LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, time);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return null;
    }

    private static String sanitizeTableName(String tableName) {
        return tableName.replaceAll("[^a-zA-Z0-9_]", "");
    }

    private static String sanitizeColumnName(String columnName) {
        return columnName.replaceAll("[^a-zA-Z0-9_]", "");
    }
}