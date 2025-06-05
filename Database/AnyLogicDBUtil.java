import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.*;
import java.sql.*;
import java.util.*;

/**
 * Erweiterte Utility-Klasse für AnyLogic Datenbankoperationen.
 * Unterstützt CSV/Excel Import mit dynamischer Tabellenerstellung
 * und manuelle Dateneingabe.
 */
public class AnyLogicDBUtil {

    /**
     * Öffnet eine Verbindung zur internen Datenbank.
     */
    public static Connection openConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:hsqldb:mem:default");
    }

    /**
     * Öffnet eine Verbindung mit der angegebenen JDBC URL.
     */
    public static Connection openConnection(String url) throws SQLException {
        return DriverManager.getConnection(url);
    }

    /**
     * Verbindet sich zur HSQLDB Instanz mit Standard-Konfiguration.
     */
    public static Connection openHSQLDBConnection() throws SQLException {
        String url = "jdbc:hsqldb:hsql://localhost:9001/projectx;file:/Users/merluee/Models/ProjectX//database/db";
        return DriverManager.getConnection(url, "SA", "");
    }

    /**
     * Importiert eine einzelne CSV/Excel Datei in eine Tabelle.
     *
     * @param conn Datenbankverbindung
     * @param tableName Name der zu erstellenden Tabelle (null = Dateiname verwenden)
     * @param file CSV oder Excel Datei
     * @param replaceTable true = existierende Tabelle ersetzen, false = Daten anhängen
     */
    public static void importTableFromFile(Connection conn, String tableName, File file, boolean replaceTable)
            throws SQLException, IOException {

        List<String[]> rows = readFile(file);
        if (rows.isEmpty()) {
            System.out.println("Warnung: Datei " + file.getName() + " ist leer");
            return;
        }

        // Tabellenname ableiten wenn nicht angegeben
        if (tableName == null || tableName.trim().isEmpty()) {
            tableName = deriveTableNameFromFile(file);
        }

        String[] headers = rows.get(0);

        // Tabelle erstellen oder ersetzen
        if (replaceTable) {
            dropTableIfExists(conn, tableName);
        }
        createTableIfNotExists(conn, tableName, headers);

        // Daten einfügen
        insertData(conn, tableName, headers, rows.subList(1, rows.size()));

        System.out.println("Erfolgreich importiert: " + file.getName() + " → Tabelle '" + tableName + "' (" + (rows.size() - 1) + " Zeilen)");
    }

    /**
     * Überladene Methode mit Standard-Verhalten (nicht ersetzen).
     */
    public static void importTableFromFile(Connection conn, String tableName, File file)
            throws SQLException, IOException {
        importTableFromFile(conn, tableName, file, false);
    }

    /**
     * Importiert alle CSV/Excel Dateien aus einem Verzeichnis.
     *
     * @param conn Datenbankverbindung
     * @param directory Verzeichnis mit CSV/Excel Dateien
     * @param replaceExistingTables true = existierende Tabellen ersetzen
     */
    public static void importTablesFromDirectory(Connection conn, File directory, boolean replaceExistingTables)
            throws SQLException, IOException {

        if (!directory.isDirectory()) {
            throw new IOException(directory + " ist kein Verzeichnis");
        }

        File[] files = directory.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".csv") || lower.endsWith(".xls");
        });

        if (files == null || files.length == 0) {
            System.out.println("Keine CSV/Excel Dateien im Verzeichnis " + directory.getPath() + " gefunden");
            return;
        }

        System.out.println("Importiere " + files.length + " Dateien aus " + directory.getPath());

        for (File file : files) {
            try {
                String tableName = deriveTableNameFromFile(file);
                importTableFromFile(conn, tableName, file, replaceExistingTables);
            } catch (Exception e) {
                System.err.println("Fehler beim Importieren von " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Erstellt eine Tabelle manuell mit den angegebenen Spalten.
     *
     * @param conn Datenbankverbindung
     * @param tableName Name der Tabelle
     * @param columns Map mit Spaltenname → Datentyp (z.B. "VARCHAR(255)", "INTEGER", "DECIMAL(10,2)")
     * @param replaceIfExists true = existierende Tabelle ersetzen
     */
    public static void createTable(Connection conn, String tableName, Map<String, String> columns, boolean replaceIfExists)
            throws SQLException {

        if (replaceIfExists) {
            dropTableIfExists(conn, tableName);
        }

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(sanitizeTableName(tableName)).append(" (");

        boolean first = true;
        for (Map.Entry<String, String> column : columns.entrySet()) {
            if (!first) sql.append(", ");
            sql.append(sanitizeColumnName(column.getKey())).append(" ").append(column.getValue());
            first = false;
        }
        sql.append(")");

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql.toString());
            System.out.println("Tabelle '" + tableName + "' erstellt mit " + columns.size() + " Spalten");
        }
    }

    /**
     * Fügt manuell Daten in eine Tabelle ein.
     *
     * @param conn Datenbankverbindung
     * @param tableName Name der Tabelle
     * @param columnNames Spaltennamen in der richtigen Reihenfolge
     * @param rows Liste der Datenzeilen
     */
    public static void insertManualData(Connection conn, String tableName, String[] columnNames, List<Object[]> rows)
            throws SQLException {

        if (rows.isEmpty()) {
            System.out.println("Keine Daten zum Einfügen");
            return;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(sanitizeTableName(tableName)).append(" (");

        for (int i = 0; i < columnNames.length; i++) {
            if (i > 0) sql.append(", ");
            sql.append(sanitizeColumnName(columnNames[i]));
        }

        sql.append(") VALUES (");
        sql.append(String.join(",", Collections.nCopies(columnNames.length, "?")));
        sql.append(")");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (Object[] row : rows) {
                for (int i = 0; i < columnNames.length; i++) {
                    Object value = i < row.length ? row[i] : null;
                    ps.setObject(i + 1, value);
                }
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            System.out.println("Erfolgreich " + results.length + " Zeilen in Tabelle '" + tableName + "' eingefügt");
        }
    }

    /**
     * Zeigt den Inhalt einer Tabelle an.
     */
    public static void displayTable(Connection conn, String tableName, int maxRows) throws SQLException {
        String sql = "SELECT * FROM " + sanitizeTableName(tableName);
        if (maxRows > 0) {
            sql += " LIMIT " + maxRows;
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            // Header ausgeben
            System.out.println("\n=== Tabelle: " + tableName + " ===");
            for (int i = 1; i <= columnCount; i++) {
                System.out.printf("%-20s", meta.getColumnName(i));
            }
            System.out.println();
            System.out.println("-".repeat(columnCount * 20));

            // Daten ausgeben
            int rowCount = 0;
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    System.out.printf("%-20s", value != null ? value : "NULL");
                }
                System.out.println();
                rowCount++;
            }
            System.out.println("(" + rowCount + " Zeilen)");
        }
    }

    /**
     * Listet alle Tabellen in der Datenbank auf.
     */
    public static void listTables(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
            System.out.println("\n=== Vorhandene Tabellen ===");
            while (rs.next()) {
                System.out.println("- " + rs.getString("TABLE_NAME"));
            }
        }
    }

    // Private Hilfsmethoden

    private static List<String[]> readFile(File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".csv")) {
            return readCsv(file);
        }
        if (name.endsWith(".xls")) {
            return readExcel(file, false);
        }
        if (name.endsWith(".xlsx")) {
            throw new IOException("XLSX-Dateien werden ohne erweiterte POI-Bibliothek nicht unterstützt. Bitte konvertieren Sie zu XLS oder CSV.");
        }
        throw new IOException("Nicht unterstützter Dateityp: " + file);
    }

    private static List<String[]> readCsv(File file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Einfache CSV-Parsing (kann bei komplexeren CSVs erweitert werden)
                rows.add(parseCsvLine(line));
            }
        }
        return rows;
    }

    private static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(field.toString().trim());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString().trim());

        return fields.toArray(new String[0]);
    }

    private static List<String[]> readExcel(File file, boolean isXlsx) throws IOException {
        if (isXlsx) {
            throw new IOException("XLSX-Dateien werden ohne erweiterte POI-Bibliothek nicht unterstützt. Bitte konvertieren Sie zu XLS oder CSV.");
        }

        List<String[]> rows = new ArrayList<>();
        try (InputStream in = new FileInputStream(file)) {
            HSSFWorkbook wb = new HSSFWorkbook(in);
            HSSFSheet sheet = wb.getSheetAt(0);
            int firstRow = sheet.getFirstRowNum();
            int lastRow = sheet.getLastRowNum();

            for (int r = firstRow; r <= lastRow; r++) {
                HSSFRow row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                int firstCell = row.getFirstCellNum();
                int lastCell = row.getLastCellNum();
                List<String> values = new ArrayList<>();

                for (int c = firstCell; c < lastCell; c++) {
                    HSSFCell cell = row.getCell(c);
                    values.add(getCellValueAsString(cell));
                }

                // Leere Zeilen am Ende entfernen
                while (!values.isEmpty() && (values.get(values.size() - 1) == null || values.get(values.size() - 1).trim().isEmpty())) {
                    values.remove(values.size() - 1);
                }
                if (!values.isEmpty()) {
                    rows.add(values.toArray(new String[0]));
                }
            }
        }
        return rows;
    }

    private static String getCellValueAsString(HSSFCell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case HSSFCell.CELL_TYPE_STRING:
                return cell.getStringCellValue();
            case HSSFCell.CELL_TYPE_NUMERIC:
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue)) {
                    return String.valueOf((long) numValue);
                } else {
                    return String.valueOf(numValue);
                }
            case HSSFCell.CELL_TYPE_BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case HSSFCell.CELL_TYPE_FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private static String deriveTableNameFromFile(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            name = name.substring(0, dotIndex);
        }
        return sanitizeTableName(name);
    }

    private static void createTableIfNotExists(Connection conn, String tableName, String[] headers) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(sanitizeTableName(tableName)).append(" (");

        for (int i = 0; i < headers.length; i++) {
            if (i > 0) sql.append(", ");
            sql.append(sanitizeColumnName(headers[i])).append(" VARCHAR(255)");
        }
        sql.append(")");

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql.toString());
        }
    }

    private static void dropTableIfExists(Connection conn, String tableName) throws SQLException {
        String sql = "DROP TABLE IF EXISTS " + sanitizeTableName(tableName);
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private static void insertData(Connection conn, String tableName, String[] headers, List<String[]> dataRows) throws SQLException {
        if (dataRows.isEmpty()) return;

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(sanitizeTableName(tableName)).append(" VALUES (");
        sql.append(String.join(",", Collections.nCopies(headers.length, "?")));
        sql.append(")");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (String[] row : dataRows) {
                for (int i = 0; i < headers.length; i++) {
                    String value = i < row.length ? row[i] : null;
                    ps.setString(i + 1, value);
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static String sanitizeTableName(String name) {
        return name.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^A-Za-z0-9_]", "")
                .toLowerCase();
    }

    private static String sanitizeColumnName(String name) {
        String sanitized = name.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^A-Za-z0-9_]", "");

        if (sanitized.isEmpty() || Character.isDigit(sanitized.charAt(0))) {
            sanitized = "col_" + sanitized;
        }
        return sanitized.toLowerCase();
    }
}