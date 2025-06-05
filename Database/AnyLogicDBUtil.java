import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets; // Import für Charset

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
     * HINWEIS: Der Pfad zur Datenbankdatei ist hartcodiert und muss ggf. angepasst werden.
     */
    public static Connection openHSQLDBConnection() throws SQLException {
        // Der Pfad sollte für Ihre Umgebung korrekt sein oder konfigurierbar gemacht werden.
        String url = "jdbc:hsqldb:hsql://localhost:9001/projekty;file:/Users/merluee/Models/ProjektY//database/db";
        // Standardmäßig verwendet HSQLDB den Benutzer "SA" mit einem leeren Passwort.
        return DriverManager.getConnection(url, "SA", "");
    }

    /**
     * Importiert eine einzelne CSV/Excel Datei in eine Tabelle.
     *
     * @param conn Datenbankverbindung
     * @param tableName Name der zu erstellenden Tabelle (null = Dateiname verwenden)
     * @param file CSV oder Excel Datei (nur .xls wird derzeit unterstützt)
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
     * @param directory Verzeichnis mit CSV/Excel Dateien (nur .xls wird derzeit unterstützt)
     * @param replaceExistingTables true = existierende Tabellen ersetzen
     */
    public static void importTablesFromDirectory(Connection conn, File directory, boolean replaceExistingTables)
            throws SQLException, IOException {

        if (!directory.isDirectory()) {
            throw new IOException(directory + " ist kein Verzeichnis");
        }

        File[] files = directory.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            // Aktuell nur .csv und .xls, da .xlsx explizit ausgeschlossen wird.
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
                // Optional: e.printStackTrace(); für detailliertere Fehlermeldungen
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
            int[] results = ps.executeBatch(); // Use executeBatch for PreparedStatement
            System.out.println("Erfolgreich " + Arrays.stream(results).sum() + " Zeilen in Tabelle '" + tableName + "' eingefügt (Summe der betroffenen Zeilen pro Batch-Anweisung)");
        }
    }

    /**
     * Zeigt den Inhalt einer Tabelle an.
     */
    public static void displayTable(Connection conn, String tableName, int maxRows) throws SQLException {
        String sql = "SELECT * FROM " + sanitizeTableName(tableName);
        if (maxRows > 0) {
            // HSQLDB verwendet LIMIT, andere Datenbanken könnten TOP oder ROWNUM verwenden
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
            System.out.println("(" + rowCount + " Zeilen" + (maxRows > 0 && rowCount == maxRows ? " - Limit erreicht" : "") + ")");
        }
    }

    /**
     * Listet alle Tabellen in der Datenbank auf.
     */
    public static void listTables(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        // Parameter für getTables: catalog, schemaPattern, tableNamePattern, types[]
        try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
            System.out.println("\n=== Vorhandene Tabellen ===");
            boolean found = false;
            while (rs.next()) {
                System.out.println("- " + rs.getString("TABLE_NAME"));
                found = true;
            }
            if (!found) {
                System.out.println("Keine Tabellen gefunden.");
            }
        }
    }

    // Private Hilfsmethoden

    private static List<String[]> readFile(File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".csv")) {
            return readCsv(file);
        }
        // Derzeit wird .xlsx explizit nicht unterstützt.
        if (name.endsWith(".xls")) {
            return readExcelXls(file); // Umbenannt, um Klarheit zu schaffen
        }
        // Die ursprüngliche Implementierung hat hier einen Fehler für .xlsx ausgelöst.
        // Wenn .xlsx-Unterstützung gewünscht ist, müsste hier eine entsprechende Logik (mit XSSFWorkbook) implementiert werden.
        if (name.endsWith(".xlsx")) {
            throw new IOException("XLSX-Dateien (.xlsx) werden von dieser Methode derzeit nicht unterstützt. Bitte konvertieren Sie zu XLS oder CSV, oder erweitern Sie die Methode.");
        }
        throw new IOException("Nicht unterstützter Dateityp: " + file.getName() + ". Nur .csv und .xls werden unterstützt.");
    }

    private static List<String[]> readCsv(File file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        // Explizit UTF-8 verwenden, um Kompatibilitätsprobleme zu vermeiden
        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                rows.add(parseCsvLine(line));
            }
        }
        return rows;
    }

    private static String[] parseCsvLine(String line) {
        // Diese einfache Implementierung geht davon aus, dass Kommas die Trennzeichen sind
        // und Anführungszeichen Felder umschließen können, die Kommas enthalten.
        // Für komplexere CSV-Dateien (z.B. mit escaped quotes) wäre eine robustere Bibliothek wie OpenCSV sinnvoll.
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Behandle doppelte Anführungszeichen innerhalb von Anführungszeichen als literales Anführungszeichen
                    field.append('"');
                    i++; // Überspringe das nächste Anführungszeichen
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(field.toString().trim());
                field.setLength(0); // Reset für das nächste Feld
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString().trim()); // Letztes Feld hinzufügen

        return fields.toArray(new String[0]);
    }

    // Spezifische Methode für .xls Dateien (HSSF)
    private static List<String[]> readExcelXls(File file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (InputStream in = new FileInputStream(file);
             HSSFWorkbook wb = new HSSFWorkbook(in)) { // HSSFWorkbook für .xls

            // Annahme: Daten sind im ersten Sheet
            if (wb.getNumberOfSheets() == 0) {
                System.out.println("Warnung: Die Excel-Datei " + file.getName() + " enthält keine Sheets.");
                return rows; // Leere Liste zurückgeben
            }
            HSSFSheet sheet = wb.getSheetAt(0);

            int firstRowIdx = sheet.getFirstRowNum();
            int lastRowIdx = sheet.getLastRowNum();

            for (int r = firstRowIdx; r <= lastRowIdx; r++) {
                HSSFRow row = sheet.getRow(r);
                if (row == null) { // Leere Zeilen überspringen
                    continue;
                }

                // getLastCellNum() kann problematisch sein, wenn Zellen fehlen.
                // Es ist besser, bis zur maximalen Spaltenanzahl der Zeile zu iterieren oder
                // die Anzahl der Spalten aus dem Header zu verwenden, falls bekannt.
                // Hier verwenden wir getLastCellNum, was die Nummer der letzten Zelle + 1 ist.
                short firstCellIdx = row.getFirstCellNum();
                short lastCellIdx = row.getLastCellNum(); // Index der letzten Zelle + 1; kann -1 sein, wenn die Zeile leer ist.

                if (lastCellIdx < 0) continue; // Zeile ohne Zellen überspringen

                List<String> values = new ArrayList<>();
                for (int c = firstCellIdx; c < lastCellIdx; c++) {
                    HSSFCell cell = row.getCell(c); // Kann null sein, wenn Zelle leer ist
                    values.add(getCellValueAsString(cell));
                }

                // Leere Strings am Ende der Zeile entfernen, um Konsistenz zu wahren
                while (!values.isEmpty() && (values.get(values.size() - 1) == null || values.get(values.size() - 1).trim().isEmpty())) {
                    values.remove(values.size() - 1);
                }

                // Nur Zeilen mit Inhalt hinzufügen
                if (!values.isEmpty() || rows.isEmpty()) { // Header immer hinzufügen, auch wenn leer
                    rows.add(values.toArray(new String[0]));
                }
            }
        }
        return rows;
    }


    private static String getCellValueAsString(HSSFCell cell) {
        if (cell == null) {
            return ""; // Leere Zeichenfolge für leere Zellen
        }

        CellType cellType = cell.getCellType(); // Gibt CellType Enum in POI >= 4.0 zurück

        // Wenn die Zelle eine Formel ist, versuchen, den zwischengespeicherten Ergebnis-Typ zu verwenden
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }

        switch (cellType) {
            case STRING:
                return cell.getStringCellValue(); // Bevorzugte Methode für String-Zellen
            case NUMERIC:
                // Hier könnten Datumsformate behandelt werden, falls erforderlich
                // if (DateUtil.isCellDateFormatted(cell)) {
                //     return cell.getDateCellValue().toString(); // Oder formatiert als String
                // }
                double numValue = cell.getNumericCellValue();
                // Prüfen, ob es sich um eine Ganzzahl handelt
                if (numValue == Math.floor(numValue) && !Double.isInfinite(numValue)) {
                    return String.valueOf((long) numValue);
                } else {
                    return String.valueOf(numValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK: // Leere Zellen explizit behandeln
                return "";
            case ERROR: // Fehlerzellen behandeln
                return "ERROR_IN_CELL"; // Oder Byte.toString(cell.getErrorCellValue())
            default: // _NONE und andere unbekannte Typen
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
        if (headers == null || headers.length == 0) {
            System.err.println("Kann Tabelle nicht ohne Header erstellen: " + tableName);
            // Alternativ eine Exception werfen oder eine Standardspalte erstellen
            throw new SQLException("Header dürfen nicht leer sein, um eine Tabelle zu erstellen.");
        }
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(sanitizeTableName(tableName)).append(" (");

        for (int i = 0; i < headers.length; i++) {
            if (i > 0) sql.append(", ");
            // Standardmäßig wird VARCHAR(255) verwendet. Dies könnte anpassbar sein.
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
        if (headers == null || headers.length == 0) {
            System.err.println("Kann Daten nicht ohne Header-Informationen einfügen für Tabelle: " + tableName);
            return;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(sanitizeTableName(tableName)).append(" (");
        for(int i=0; i < headers.length; i++){
            if(i > 0) sql.append(", ");
            sql.append(sanitizeColumnName(headers[i]));
        }
        sql.append(") VALUES (");
        sql.append(String.join(",", Collections.nCopies(headers.length, "?")));
        sql.append(")");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (String[] row : dataRows) {
                // Sicherstellen, dass die Anzahl der Werte mit der Anzahl der Header übereinstimmt
                for (int i = 0; i < headers.length; i++) {
                    // Wenn eine Zeile weniger Werte hat als Header, wird null für die fehlenden Werte verwendet
                    String value = (i < row.length) ? row[i] : null;
                    ps.setString(i + 1, value);
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static String sanitizeTableName(String name) {
        if (name == null || name.trim().isEmpty()) {
            // Fallback, falls der Name leer ist, obwohl dies durch deriveTableNameFromFile vermieden werden sollte
            return "default_table_name";
        }
        String sanitized = name.trim()
                .replaceAll("\\s+", "_") // Leerzeichen durch Unterstriche ersetzen
                .replaceAll("[^A-Za-z0-9_]", ""); // Alle nicht-alphanumerischen Zeichen (außer Unterstrich) entfernen

        // Sicherstellen, dass der Name nicht mit einer Zahl beginnt (SQL-Konvention)
        if (sanitized.isEmpty() || Character.isDigit(sanitized.charAt(0))) {
            sanitized = "tbl_" + sanitized;
        }
        return sanitized.toLowerCase(); // Zu Kleinbuchstaben konvertieren für Konsistenz
    }

    private static String sanitizeColumnName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "unnamed_column";
        }
        String sanitized = name.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^A-Za-z0-9_]", "");

        // SQL-Keywords und andere problematische Namen könnten hier auch behandelt werden, falls nötig
        if (sanitized.isEmpty() || Character.isDigit(sanitized.charAt(0))) {
            sanitized = "col_" + sanitized;
        }
        return sanitized.toLowerCase();
    }
}
