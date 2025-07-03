/**
 * Optimierte Version der DBRequest-Klasse für AnyLogic-Simulationen
 *
 * Optimierungen:
 * - Connection Pooling für wiederverwendbare Verbindungen
 * - In-Memory Cache für häufig abgerufene Daten
 * - Batch-Loading von Zeitreihendaten
 * - Prepared Statement Caching
 * - Asynchrone Datenvorladung
 */

import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Opti_DBRequest {

    // Connection Pool (Singleton)
    private static HikariDataSource dataSource;

    // Cache für Zeitreihendaten
    private static final Map<String, List<TimeSeriesPoint>> dataCache = new ConcurrentHashMap<>();
    private static final Map<String, PreparedStatement> statementCache = new ConcurrentHashMap<>();

    // Cache-Konfiguration
    private static final long CACHE_VALIDITY_MINUTES = 60; // Cache 1 Stunde gültig
    private static final Map<String, LocalDateTime> cacheTimestamps = new ConcurrentHashMap<>();

    /**
     * Datenstruktur für Zeitreihenpunkte
     */
    public static class TimeSeriesPoint {
        public final LocalDateTime timestamp;
        public final double value;

        public TimeSeriesPoint(LocalDateTime timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }

    /**
     * Initialisiert den Connection Pool - einmalig beim Start aufrufen
     */
    public static synchronized void initializeConnectionPool() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:postgresql://localhost:5432/simdata");
            config.setUsername("user");
            config.setPassword("password");

            // Pool-Optimierungen
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            // Performance-Optimierungen
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            dataSource = new HikariDataSource(config);
        }
    }

    /**
     * Lädt alle Daten für einen bestimmten Zeitraum vor und cached sie
     * Sollte einmalig zu Beginn der Simulation aufgerufen werden
     */
    public static void preloadTimeSeriesData(String tableName, String timeColumn,
                                             String dataColumn, LocalDateTime start,
                                             LocalDateTime end) {
        String cacheKey = generateCacheKey(tableName, timeColumn, dataColumn);

        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT \"" + sanitizeIdentifier(timeColumn) + "\", \"" +
                    sanitizeIdentifier(dataColumn) + "\" FROM \"" +
                    sanitizeIdentifier(tableName) + "\" WHERE \"" +
                    sanitizeIdentifier(timeColumn) + "\" >= ? AND \"" +
                    sanitizeIdentifier(timeColumn) + "\" <= ? ORDER BY \"" +
                    sanitizeIdentifier(timeColumn) + "\" ASC";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, timestampFromLocalDateTime(start));
                ps.setTimestamp(2, timestampFromLocalDateTime(end));

                List<TimeSeriesPoint> points = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDateTime timestamp = rs.getTimestamp(1).toLocalDateTime();
                        double value = rs.getDouble(2);
                        points.add(new TimeSeriesPoint(timestamp, value));
                    }
                }

                dataCache.put(cacheKey, points);
                cacheTimestamps.put(cacheKey, LocalDateTime.now());

                System.out.println("✅ Preloaded " + points.size() + " data points for " + cacheKey);
            }
        } catch (SQLException e) {
            System.err.println("🚨 Error preloading data: " + e.getMessage());
        }
    }

    /**
     * Schnelle Abfrage von Zeitreihendaten aus dem Cache
     * Verwendet für 15-Minuten-Intervalle in der Simulation
     */
    public static double getValueAtTime(String tableName, String timeColumn,
                                        String dataColumn, LocalDateTime targetTime) {
        String cacheKey = generateCacheKey(tableName, timeColumn, dataColumn);

        // Prüfe ob Daten im Cache vorhanden und aktuell sind
        if (!isCacheValid(cacheKey)) {
            System.err.println("⚠️ Cache miss or expired for " + cacheKey);
            return getValueFromDatabase(tableName, timeColumn, dataColumn, targetTime);
        }

        List<TimeSeriesPoint> points = dataCache.get(cacheKey);
        if (points == null || points.isEmpty()) {
            return 0.0;
        }

        // Binäre Suche für optimale Performance
        return findValueAtTime(points, targetTime);
    }

    /**
     * Optimierte Suche des nächstliegenden Wertes zu einem Zeitpunkt
     */
    private static double findValueAtTime(List<TimeSeriesPoint> points, LocalDateTime targetTime) {
        if (points.isEmpty()) return 0.0;

        // Binäre Suche
        int left = 0;
        int right = points.size() - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            TimeSeriesPoint point = points.get(mid);

            if (point.timestamp.equals(targetTime)) {
                return point.value;
            } else if (point.timestamp.isBefore(targetTime)) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        // Nächstliegenden Wert zurückgeben
        if (right < 0) return points.get(0).value;
        if (left >= points.size()) return points.get(points.size() - 1).value;

        // Wähle den zeitlich näheren Wert
        TimeSeriesPoint leftPoint = points.get(right);
        TimeSeriesPoint rightPoint = points.get(left);

        Duration leftDistance = Duration.between(leftPoint.timestamp, targetTime);
        Duration rightDistance = Duration.between(targetTime, rightPoint.timestamp);

        return leftDistance.compareTo(rightDistance) <= 0 ? leftPoint.value : rightPoint.value;
    }

    /**
     * Fallback: Direkter Datenbankzugriff wenn Cache nicht verfügbar
     */
    private static double getValueFromDatabase(String tableName, String timeColumn,
                                               String dataColumn, LocalDateTime targetTime) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT \"" + sanitizeIdentifier(dataColumn) +
                    "\" FROM \"" + sanitizeIdentifier(tableName) +
                    "\" WHERE \"" + sanitizeIdentifier(timeColumn) +
                    "\" <= ? ORDER BY \"" + sanitizeIdentifier(timeColumn) +
                    "\" DESC LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, timestampFromLocalDateTime(targetTime));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("🚨 Database query error: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Batch-Update für mehrere Zeitpunkte gleichzeitig
     * Optimal für AnyLogic wenn mehrere Agenten gleichzeitig Daten benötigen
     */
    public static Map<LocalDateTime, Double> getValuesForTimeRange(String tableName,
                                                                   String timeColumn,
                                                                   String dataColumn,
                                                                   LocalDateTime start,
                                                                   LocalDateTime end,
                                                                   Duration interval) {
        Map<LocalDateTime, Double> results = new HashMap<>();
        String cacheKey = generateCacheKey(tableName, timeColumn, dataColumn);

        if (!isCacheValid(cacheKey)) {
            System.err.println("⚠️ Cache not available, using database query");
            return getBatchFromDatabase(tableName, timeColumn, dataColumn, start, end);
        }

        List<TimeSeriesPoint> points = dataCache.get(cacheKey);
        LocalDateTime current = start;

        while (!current.isAfter(end)) {
            double value = findValueAtTime(points, current);
            results.put(current, value);
            current = current.plus(interval);
        }

        return results;
    }

    /**
     * Asynchrone Vorabladung von Daten für bessere Performance
     */
    public static CompletableFuture<Void> preloadDataAsync(String tableName, String timeColumn,
                                                           String dataColumn, LocalDateTime start,
                                                           LocalDateTime end) {
        return CompletableFuture.runAsync(() -> {
            preloadTimeSeriesData(tableName, timeColumn, dataColumn, start, end);
        });
    }

    // Hilfsmethoden
    private static String generateCacheKey(String tableName, String timeColumn, String dataColumn) {
        return tableName + ":" + timeColumn + ":" + dataColumn;
    }

    private static boolean isCacheValid(String cacheKey) {
        LocalDateTime cacheTime = cacheTimestamps.get(cacheKey);
        return cacheTime != null &&
                dataCache.containsKey(cacheKey) &&
                Duration.between(cacheTime, LocalDateTime.now()).toMinutes() < CACHE_VALIDITY_MINUTES;
    }

    private static Map<LocalDateTime, Double> getBatchFromDatabase(String tableName,
                                                                   String timeColumn,
                                                                   String dataColumn,
                                                                   LocalDateTime start,
                                                                   LocalDateTime end) {
        Map<LocalDateTime, Double> results = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT \"" + sanitizeIdentifier(timeColumn) + "\", \"" +
                    sanitizeIdentifier(dataColumn) + "\" FROM \"" +
                    sanitizeIdentifier(tableName) + "\" WHERE \"" +
                    sanitizeIdentifier(timeColumn) + "\" >= ? AND \"" +
                    sanitizeIdentifier(timeColumn) + "\" <= ? ORDER BY \"" +
                    sanitizeIdentifier(timeColumn) + "\" ASC";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, timestampFromLocalDateTime(start));
                ps.setTimestamp(2, timestampFromLocalDateTime(end));

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDateTime timestamp = rs.getTimestamp(1).toLocalDateTime();
                        double value = rs.getDouble(2);
                        results.put(timestamp, value);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("🚨 Batch query error: " + e.getMessage());
        }

        return results;
    }

    private static Timestamp timestampFromLocalDateTime(LocalDateTime ldt) {
        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC"));
        return Timestamp.from(zdt.toInstant());
    }

    private static String sanitizeIdentifier(String identifier) {
        return identifier.replaceAll("[^a-zA-Z0-9_]", "");
    }

    /**
     * Schließt den Connection Pool - am Ende der Simulation aufrufen
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        dataCache.clear();
        cacheTimestamps.clear();
        statementCache.clear();
    }

    public static void main(String[] args) {
        // 1. Connection Pool initialisieren
        initializeConnectionPool();

        // 2. Daten für Simulationszeitraum vorladen
        LocalDateTime simStart = LocalDateTime.of(2016, 1, 1, 0, 0);
        LocalDateTime simEnd = LocalDateTime.of(2016, 12, 31, 23, 59);

        System.out.println("🔄 Preloading PV data...");
        preloadTimeSeriesData("pv", "Time", "kWh", simStart, simEnd);

        System.out.println("🔄 Preloading household data...");
        preloadTimeSeriesData("household_data", "utc_timestamp", "average_per_person_consumption", simStart, simEnd);

        // 3. Simulation - schnelle Abfragen alle 15 Minuten
        LocalDateTime current = simStart;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 100; i++) { // 100 Abfragen testen
            double pvValue = getValueAtTime("pv", "Time", "kWh", current);
            double householdValue = getValueAtTime("household_data", "utc_timestamp", "average_per_person_consumption", current);

            if (i % 20 == 0) {
                System.out.println("Time: " + current + ", PV: " + pvValue + ", Household: " + householdValue);
            }
            current = current.plusMinutes(15);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("⚡ 100 queries completed in " + (endTime - startTime) + "ms");

        // 4. Cleanup
        shutdown();
    }
}