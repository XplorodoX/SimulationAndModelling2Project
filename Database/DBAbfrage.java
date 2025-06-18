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
        String tableName = "pv"; // table for PV data
        String tableName2 = "household_data"; // table for household consumption

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
            // Datum und Uhrzeit in: 2005-04-01 16:45:00
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

            // --- 2. Use the Integrated Functions ---
            LocalDateTime startDateTime2 = LocalDateTime.of(2016, 1, 1, 0, 0, 0);
            LocalDateTime endDateTime2 = LocalDateTime.of(2016, 1, 1, 1, 0, 0);

            Timestamp endTime2 = Timestamp.valueOf(endDateTime2);
            Timestamp startTime2 = Timestamp.valueOf(startDateTime2);

            // Query all timestamp/kWh pairs in the given range
            //Datum und Uhrzeit in UTC: 2016-01-01T00:45:00Z
            List<Object[]> data2 = getDataAtTimeStampRange(connection,
                    tableName2,
                    "utc_timestamp",
                    List.of("average_per_person_consumption"),
                    startTime2,
                    endTime2);

            System.out.println("\nTable: " + tableName2);
            System.out.println("Time range: " + startTime2 + " to " + endTime2);
            if (data2.isEmpty()) {
                System.out.println("📢 No data found for the specified time range.");
            } else {
                double totalAvg = 0.0;
                for (Object[] row : data2) {
                    Timestamp ts = (Timestamp) row[0];
                    Double val = (Double) row[1];
                    System.out.println(ts + " -> " + val);
                    if (val != null) {
                        totalAvg += val;
                    }
                }
                System.out.println("Total average consumption in range: " + String.format("%.2f", totalAvg));
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
     * Gibt alle Zeitstempel-Spalten-Paare für einen bestimmten Zeitraum zurück.
     * Die zurückgegebene Liste enthält für jede Zeile ein Object[], wobei das erste
     * Element der Zeitstempel und die weiteren Elemente den angeforderten Spalten
     * entsprechen. Gibt eine leere Liste zurück, wenn keine Daten gefunden werden.
     */
    public static List<Object[]> getDataAtTimeStampRange(Connection conn,
                                                         String tableName,
                                                         String timestampColumn,
                                                         List<String> columns,
                                                         Timestamp startTime,
                                                         Timestamp endTime) throws SQLException {
        String sanitizedTable = sanitizeTableName(tableName);
        String sanitizedTimeColumn = sanitizeColumnName(timestampColumn);

        List<String> sanitizedColumns = new ArrayList<>();
        for (String col : columns) {
            sanitizedColumns.add(sanitizeColumnName(col));
        }

        StringBuilder columnBuilder = new StringBuilder();
        columnBuilder.append('"').append(sanitizedTimeColumn).append('"');
        for (String col : sanitizedColumns) {
            columnBuilder.append(", \"").append(col).append("\"");
        }

        String sql = "SELECT " + columnBuilder + " FROM \"" + sanitizedTable + "\"" +
                " WHERE \"" + sanitizedTimeColumn + "\" >= ? AND \"" + sanitizedTimeColumn + "\" <= ?" +
                " ORDER BY \"" + sanitizedTimeColumn + "\" ASC";

        List<Object[]> resultList = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, startTime);
            ps.setTimestamp(2, endTime);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[sanitizedColumns.size() + 1];
                    row[0] = rs.getTimestamp(1);
                    for (int i = 0; i < sanitizedColumns.size(); i++) {
                        row[i + 1] = rs.getObject(i + 2);
                    }
                    resultList.add(row);
                }
            }
        }

        return resultList;
    }

    /**
     * Beibehaltende Rückwärtskompatibilität: ruft nur die "kWh"-Spalte ab.
     */
    public static List<Object[]> getDataAtTimeStampRange(Connection conn,
                                                         String tableName,
                                                         String timestampColumn,
                                                         Timestamp startTime,
                                                         Timestamp endTime) throws SQLException {
        List<String> cols = new ArrayList<>();
        cols.add("kWh");
        return getDataAtTimeStampRange(conn, tableName, timestampColumn, cols, startTime, endTime);
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