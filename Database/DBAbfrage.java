import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DBAbfrage {

    public static void main(String[] args) {
        // --- Database Connection Details ---
        String url = "jdbc:postgresql://localhost:5432/simdata";
        String user = "user";
        String password = "password";
        String tableName = "pv";

        Connection connection = null;

        try {
            // --- 1. Establish the Connection ---
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Successfully connected to the PostgreSQL database!");

            // --- 2. Use the Integrated Functions ---
            LocalDateTime startDateTime = LocalDateTime.of(2005, 1, 1, 0, 0, 0);
            LocalDateTime endDateTime = LocalDateTime.of(2005, 1, 1, 1, 0, 0);

            Timestamp endTime = Timestamp.valueOf(endDateTime);
            Timestamp startTime = Timestamp.valueOf(startDateTime);

            // Query all timestamp/kWh pairs in the given range
            List<Object[]> data = getDataAtTimeStampRange(connection,
                    tableName,
                    "Time",
                    startTime,
                    endTime);

            Double nowKwh = getActualAtTimeStampData(connection, tableName, "Time", endTime);

            System.out.println("Table: " + tableName);
            System.out.println("Time range: " + startTime + " to " + endTime);
            System.out.println("Now KWH: " + nowKwh);

            // --- 3. Print the results ---
            if (data.isEmpty()) {
                System.out.println("📢 No data found for the specified time range.");
            } else {
                double totalKwh = 0.0;
                for (Object[] row : data) {
                    Timestamp ts = (Timestamp) row[0];
                    Double kWh = (Double) row[1];
                    System.out.println(ts + " -> " + kWh);
                    if (kWh != null) {
                        totalKwh += kWh;
                    }
                }
                System.out.println("Total kWh in range: " + String.format("%.2f", totalKwh));
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
     * Gibt alle Zeitstempel-kWh-Paare für einen bestimmten Zeitraum zurück.
     * Jedes Element der zurückgegebenen Liste ist ein Array mit [Timestamp, kWh].
     * Gibt eine leere Liste zurück, wenn keine Daten gefunden werden.
     */
    public static List<Object[]> getDataAtTimeStampRange(Connection conn,
                                                         String tableName,
                                                         String timestampColumn,
                                                         Timestamp startTime,
                                                         Timestamp endTime) throws SQLException {
        String sanitizedTable = sanitizeTableName(tableName);
        String sanitizedColumn = sanitizeColumnName("kWh");
        String sanitizedTimeColumn = sanitizeColumnName(timestampColumn);

        // SQL-Abfrage für einzelne Datenpunkte statt Summe
        String sql = "SELECT \"" + sanitizedTimeColumn + "\", \"" + sanitizedColumn + "\" FROM \"" + sanitizedTable + "\"" +
                " WHERE \"" + sanitizedTimeColumn + "\" >= ? AND \"" + sanitizedTimeColumn + "\" <= ?" +
                " ORDER BY \"" + sanitizedTimeColumn + "\" ASC";

        List<Object[]> resultList = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, startTime);
            ps.setTimestamp(2, endTime);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp timestamp = rs.getTimestamp(1);
                    Double kWh = rs.getDouble(2);

                    // Prüfe ob kWh NULL ist
                    if (rs.wasNull()) {
                        kWh = null;
                    }

                    // Füge die Daten als Array zur Ergebnisliste hinzu
                    resultList.add(new Object[]{timestamp, kWh});
                }
            }
        }

        return resultList;
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