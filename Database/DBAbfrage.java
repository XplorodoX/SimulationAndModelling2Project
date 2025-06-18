/**
 * Beispielprogramm zum Zugriff auf eine PostgreSQL-Datenbank.
 * Demonstriert einfache Abfragen für Zeitreihendaten, wie sie durch
 * das Python-Importskript bereitgestellt werden.
 *
 * Diese Klasse kann eigenständig ausgeführt werden.
 */

import java.sql.*;
import java.time.*;
import java.util.*;

public class DBAbfrage {

    /**
     * Kleine Hilfsklasse zum Aufbau und automatischen Schliessen einer JDBC-Verbindung.
     * Standardwerte fuer URL, Benutzername und Passwort koennen ueber Parameter
     * angepasst werden.
     */
    // Vereinfachte Datenbank-Verbindung mit Builder-Pattern
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

    // Vereinfachte Abfrage-Methode für Zeitreihen-Daten
    public static List<Object[]> getTimeSeriesData(Connection conn, String tableName,
                                                   String timeColumn, List<String> dataColumns,
                                                   LocalDateTime start, LocalDateTime end) throws SQLException {
        // Mit UTC-Zeitzone konvertieren
        Timestamp startTs = timestampFromLocalDateTime(start);
        Timestamp endTs = timestampFromLocalDateTime(end);

        return getTimeSeriesData(conn, tableName, timeColumn, dataColumns, startTs, endTs);
    }

    // Überladene Methode mit Timestamps
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

    // Hilfsmethode für eine einzelne Spalte
    public static List<Object[]> getTimeSeriesData(Connection conn, String tableName,
                                                   String timeColumn, String dataColumn,
                                                   LocalDateTime start, LocalDateTime end) throws SQLException {
        return getTimeSeriesData(conn, tableName, timeColumn, List.of(dataColumn), start, end);
    }

    // Hilfsmethode für den letzten Wert vor einem Zeitpunkt
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

    // Hilfsmethode für Zeitstempel-Konvertierung (berücksichtigt UTC)
    private static Timestamp timestampFromLocalDateTime(LocalDateTime ldt) {
        // Konvertiere zu UTC für die Datenbank
        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC"));
        return Timestamp.from(zdt.toInstant());
    }

    // Hilfsmethode für SQL-Injection-Schutz
    private static String sanitizeIdentifier(String identifier) {
        return identifier.replaceAll("[^a-zA-Z0-9_]", "");
    }

    // Hilfsmethode für die Formatierung der Ausgabe
    public static void printTimeSeriesData(List<Object[]> data, String title, String valueLabel) {
        System.out.println("\n" + title);

        if (data.isEmpty()) {
            System.out.println("📢 Keine Daten gefunden.");
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

        System.out.println("Gesamt " + valueLabel + ": " + String.format("%.2f", total));
    }

    public static void main(String[] args) {
        try (DBConnection dbConn = new DBConnection()) {
            Connection conn = dbConn.getConnection();
            System.out.println("✅ Erfolgreich mit der PostgreSQL-Datenbank verbunden!");

            // PV-Daten abfragen
            LocalDateTime startPV = LocalDateTime.of(2016, 1, 1, 0, 0);
            LocalDateTime endPV = LocalDateTime.of(2016, 12, 1, 1, 0);

            List<Object[]> pvData = getTimeSeriesData(conn, "pv", "Time", "kWh", startPV, endPV);
            Object latestKwh = getLatestValueBefore(conn, "pv", "Time", "kWh", endPV);

            System.out.println("Tabelle: pv");
            System.out.println("Zeitraum: " + startPV + " bis " + endPV);
            System.out.println("Aktueller kWh-Wert: " + latestKwh);
            printTimeSeriesData(pvData, "PV-Daten:", "kWh");

            // Haushaltsdaten abfragen
            LocalDateTime startHH = LocalDateTime.of(2016, 1, 1, 1, 0);
            LocalDateTime endHH = LocalDateTime.of(2016, 1, 1, 2, 0);

            List<Object[]> hhData = getTimeSeriesData(conn, "household_data", "utc_timestamp",
                    "average_per_person_consumption", startHH, endHH);
            printTimeSeriesData(hhData, "Haushaltsdaten:", "Durchschnittsverbrauch pro Person");

        } catch (ClassNotFoundException e) {
            System.err.println("🚨 PostgreSQL JDBC-Treiber nicht gefunden.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("🚨 Datenbankverbindung fehlgeschlagen!");
            e.printStackTrace();
        }
    }
}