import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

//TODO: GetDataAtTimeStempX(Timestamp timeStamp, String tableName, String columnName)
//TODO: GetActualAtTimeStempData(String tableName, String columnName)

/**
 * Extended utility class for AnyLogic database operations.
 * Supports CSV/Excel import with dynamic table creation
 * and manual data entry.
 */
public class AnyLogicDBUtil {

    // URL for the target database
    // Ensure that the HSQLDB server for this database is running
    // if AnyLogic is to access it.
    private static final String PROJEKT_Y_DB_URL = "jdbc:hsqldb:hsql://localhost:9001/firstprojectdraft;file:/Users/merluee/IdeaProjects/SimulationAndModelling2Project3/AnylogicProject/database/db";
    private static final String DB_USER = "SA"; // Default HSQLDB user
    private static final String DB_PASSWORD = ""; // Default HSQLDB password

    /**
     * Opens a connection to AnyLogic's INTERNAL in-memory database (or a standalone in-memory DB).
     * This is NOT your file-based 'projekty' database.
     */
    public static Connection openMemoryConnection() throws SQLException {
        System.out.println("Verbinde mit In-Memory-Datenbank: jdbc:hsqldb:mem:default"); // Connecting to In-Memory-Database
        return DriverManager.getConnection("jdbc:hsqldb:mem:default");
    }

    /**
     * Opens a connection with the specified JDBC URL.
     */
    public static Connection openConnection(String url, String user, String password) throws SQLException {
        System.out.println("Verbinde mit URL: " + url); // Connecting to URL
        return DriverManager.getConnection(url, user, password);
    }
    /**
     * Overloaded method for openConnection without user/password (for DBs that do not require them or have default values).
     */
    public static Connection openConnection(String url) throws SQLException {
        System.out.println("Verbinde mit URL (ohne explizite User/Pass-Angabe): " + url); // Connecting to URL (without explicit User/Pass)
        // HSQLDB Server usually requires user/pass, but AnyLogic's internal DB not always explicitly.
        // For the external HSQLDB server connection, it's better to provide user/pass.
        // If your server DB requires user/pass, use openConnection(url, user, pass).
        // This attempts to connect without user/pass, which is OK for some setups, but not for others.
        // For connecting to the external HSQLDB server DB, explicitly providing user/pass is recommended.
        return DriverManager.getConnection(url);
    }


    /**
     * Connects to the external HSQLDB instance for ProjektY.
     * ENSURE THE HSQLDB SERVER IS RUNNING BEFORE CALLING THIS.
     */
    public static Connection openProjektYDBConnection() throws SQLException {
        System.out.println("Versuche, Verbindung zur ProjektY HSQLDB herzustellen: " + PROJEKT_Y_DB_URL); // Attempting to establish connection to ProjektY HSQLDB
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("HSQLDB JDBC Treiber nicht gefunden. Stellen Sie sicher, dass hsqldb.jar im Classpath ist.");
            throw new SQLException("HSQLDB JDBC Treiber nicht gefunden", e);
        }
        return DriverManager.getConnection(PROJEKT_Y_DB_URL, DB_USER, DB_PASSWORD);
    }


    /**
     * Imports a single CSV/Excel file into a table.
     *
     * @param conn Database connection
     * @param tableName Name of the table to be created (null = use filename)
     * @param file CSV or Excel file (only .xls and .csv are currently supported by readFile logic)
     * @param replaceTable true = replace existing table, false = append data
     */
    public static void importTableFromFile(Connection conn, String tableName, File file, boolean replaceTable)
            throws SQLException, IOException {

        if (!file.exists()) {
            System.err.println("FEHLER: Datei nicht gefunden für Import: " + file.getAbsolutePath()); // ERROR: File not found for import
            throw new FileNotFoundException("Datei nicht gefunden: " + file.getAbsolutePath()); // File not found
        }
        System.out.println("Lese Datei: " + file.getAbsolutePath()); // Reading file

        List<String[]> rows = readFile(file);
        if (rows.isEmpty()) {
            System.out.println("Warnung: Datei " + file.getName() + " ist leer oder konnte nicht gelesen werden."); // Warning: File is empty or could not be read.
            return;
        }

        // Derive table name if not specified
        if (tableName == null || tableName.trim().isEmpty()) {
            tableName = deriveTableNameFromFile(file);
        }
        System.out.println("Zieltabelle: " + tableName); // Target table


        String[] headers = rows.get(0);
        List<String[]> dataRows = rows.subList(1, rows.size());
        String[] columnTypes = guessColumnTypes(headers, dataRows);

        // Create or replace table
        if (replaceTable) {
            System.out.println("Ersetze Tabelle (falls vorhanden): " + tableName); // Replacing table (if exists)
            dropTableIfExists(conn, tableName);
        }
        createTableIfNotExists(conn, tableName, headers, columnTypes);

        // Insert data
        System.out.println("Füge Daten ein in Tabelle: " + tableName); // Inserting data into table
        insertData(conn, tableName, headers, columnTypes, dataRows);

        System.out.println("Erfolgreich importiert: " + file.getName() + " → Tabelle '" + tableName + "' (" + (rows.size() - 1) + " Zeilen)"); // Successfully imported ... rows
    }

    /**
     * Overloaded method with default behavior (do not replace).
     */
    public static void importTableFromFile(Connection conn, String tableName, File file)
            throws SQLException, IOException {
        importTableFromFile(conn, tableName, file, false);
    }

    /**
     * Imports all CSV/Excel files from a directory.
     *
     * @param conn Database connection
     * @param directory Directory with CSV/Excel files (only .xls and .csv are currently supported by listFiles filter)
     * @param replaceExistingTables true = replace existing tables
     */
    public static void importTablesFromDirectory(Connection conn, File directory, boolean replaceExistingTables)
            throws SQLException, IOException {

        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("FEHLER: Verzeichnis nicht gefunden oder kein Verzeichnis: " + directory.getAbsolutePath()); // ERROR: Directory not found or not a directory
            throw new IOException(directory.getAbsolutePath() + " ist kein Verzeichnis oder existiert nicht."); // is not a directory or does not exist.
        }
        System.out.println("Importiere aus Verzeichnis: " + directory.getAbsolutePath()); // Importing from directory


        File[] files = directory.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            // Currently only .csv and .xls, as .xlsx is explicitly excluded by readFile.
            return lower.endsWith(".csv") || lower.endsWith(".xls");
        });

        if (files == null || files.length == 0) {
            System.out.println("Keine CSV/Excel Dateien im Verzeichnis " + directory.getPath() + " gefunden"); // No CSV/Excel files found in directory
            return;
        }

        System.out.println("Importiere " + files.length + " Dateien aus " + directory.getPath()); // Importing ... files from ...

        for (File file : files) {
            try {
                System.out.println("Verarbeite Datei: " + file.getName()); // Processing file
                String tableName = deriveTableNameFromFile(file);
                importTableFromFile(conn, tableName, file, replaceExistingTables);
            } catch (Exception e) {
                System.err.println("Fehler beim Importieren von " + file.getName() + ": " + e.getMessage()); // Error importing from
                e.printStackTrace(); // More detailed error message for debugging
            }
        }
    }

    /**
     * Creates a table manually with the specified columns.
     *
     * @param conn Database connection
     * @param tableName Name of the table
     * @param columns Map with column name → data type (e.g., "VARCHAR(255)", "INTEGER", "DECIMAL(10,2)")
     * @param replaceIfExists true = replace existing table
     */
    public static void createTable(Connection conn, String tableName, Map<String, String> columns, boolean replaceIfExists)
            throws SQLException {

        String sanitizedTableName = sanitizeTableName(tableName);
        System.out.println("Erstelle Tabelle (falls nicht vorhanden/ersetzen): " + sanitizedTableName); // Creating table (if not exists/replace)

        if (replaceIfExists) {
            dropTableIfExists(conn, sanitizedTableName);
        }

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(sanitizedTableName).append(" (");

        boolean first = true;
        for (Map.Entry<String, String> column : columns.entrySet()) {
            if (!first) sql.append(", ");
            sql.append(sanitizeColumnName(column.getKey())).append(" ").append(column.getValue());
            first = false;
        }
        sql.append(")");
        System.out.println("SQL zum Erstellen der Tabelle: " + sql.toString()); // SQL for creating the table

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql.toString());
            System.out.println("Tabelle '" + sanitizedTableName + "' erstellt/überprüft mit " + columns.size() + " Spalten"); // Table '...' created/verified with ... columns
        }
    }

    /**
     * Manually inserts data into a table.
     *
     * @param conn Database connection
     * @param tableName Name of the table
     * @param columnNames Column names in the correct order
     * @param rows List of data rows
     */
    public static void insertManualData(Connection conn, String tableName, String[] columnNames, List<Object[]> rows)
            throws SQLException {

        if (rows.isEmpty()) {
            System.out.println("Keine Daten zum Einfügen"); // No data to insert
            return;
        }
        String sanitizedTableName = sanitizeTableName(tableName);
        System.out.println("Füge manuelle Daten ein in: " + sanitizedTableName); // Inserting manual data into:

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(sanitizedTableName).append(" (");

        for (int i = 0; i < columnNames.length; i++) {
            if (i > 0) sql.append(", ");
            sql.append(sanitizeColumnName(columnNames[i]));
        }

        sql.append(") VALUES (");
        sql.append(String.join(",", Collections.nCopies(columnNames.length, "?")));
        sql.append(")");
        System.out.println("SQL zum Einfügen von Daten: " + sql.toString()); // SQL for inserting data

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int rowCount = 0;
            for (Object[] row : rows) {
                for (int i = 0; i < columnNames.length; i++) {
                    Object value = i < row.length ? row[i] : null;
                    ps.setObject(i + 1, value);
                }
                ps.addBatch();
                rowCount++;
            }
            int[] results = ps.executeBatch();
            long totalInserted = 0;
            for(int res : results) {
                if (res > 0) totalInserted += res;
                else if (res == Statement.SUCCESS_NO_INFO) totalInserted++; // Count as success if no info is available
            }
            System.out.println("Erfolgreich " + totalInserted + " von " + rowCount + " Zeilen-Batches in Tabelle '" + sanitizedTableName + "' verarbeitet."); // Successfully processed ... of ... row batches into table '...'
        }
    }

    /**
     * Displays the content of a table.
     */
    public static void displayTable(Connection conn, String tableName, int maxRows) throws SQLException {
        String sanitizedTableName = sanitizeTableName(tableName);
        String sql = "SELECT * FROM " + sanitizedTableName;
        if (maxRows > 0) {
            sql += " LIMIT " + maxRows;
        }
        System.out.println("Zeige Tabelle: " + sanitizedTableName + " (SQL: " + sql + ")"); // Displaying table: ... (SQL: ...)

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            System.out.println("\n=== Tabelle: " + sanitizedTableName + " ==="); // Table:
            for (int i = 1; i <= columnCount; i++) {
                System.out.printf("%-20s", meta.getColumnName(i));
            }
            System.out.println();
            System.out.println("-".repeat(columnCount * 20));

            int rowCount = 0;
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    System.out.printf("%-20s", value != null ? value : "NULL");
                }
                System.out.println();
                rowCount++;
            }
            System.out.println("(" + rowCount + " Zeilen angezeigt" + (maxRows > 0 && rowCount >= maxRows ? " - Limit erreicht" : "") + ")"); // (... rows displayed ... - Limit reached)
            if (rowCount == 0) {
                System.out.println("Keine Daten in der Tabelle gefunden."); // No data found in the table.
            }
        }
    }

    /**
     * Lists all tables in the database.
     */
    public static void listTables(Connection conn) throws SQLException {
        System.out.println("Liste Tabellen..."); // Listing tables...
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
            System.out.println("\n=== Vorhandene Tabellen ==="); // Existing tables
            boolean found = false;
            while (rs.next()) {
                System.out.println("- " + rs.getString("TABLE_NAME"));
                found = true;
            }
            if (!found) {
                System.out.println("Keine Tabellen gefunden."); // No tables found.
            }
        }
    }

    public static Object[] getDataAtTimeStamp(Connection conn, double modelTime, String tableName, String columnName) throws SQLException {
        String sql = "SELECT * FROM " + sanitizeTableName(tableName) +
                " WHERE zeitstempel <= ? ORDER BY zeitstempel DESC LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, modelTime);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Daten aus dem ResultSet extrahieren
                    return extractRowData(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all rows from the specified table between the given start and
     * end timestamps.
     *
     * @param conn       Active database connection
     * @param tableName  Name of the table
     * @param startTime  Start of the interval (inclusive)
     * @param endTime    End of the interval (inclusive)
     * @return           List of row objects found in the interval
     */
    public static List<Object[]> getDataAtTimeStampRange(Connection conn,
                                                         String tableName,
                                                         Timestamp startTime,
                                                         Timestamp endTime) throws SQLException {
        String sql = "SELECT * FROM " + sanitizeTableName(tableName) +
                " WHERE zeitstempel >= ? AND zeitstempel <= ? ORDER BY zeitstempel";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, startTime);
            ps.setTimestamp(2, endTime);
            try (ResultSet rs = ps.executeQuery()) {
                List<Object[]> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(extractRowData(rs));
                }
                return results;
            }
        }
    }

    /**
     * Returns the most recent row up to the current system timestamp.
     * This can be used when a regularly updated time variable triggers the
     * data retrieval.
     *
     * @param conn      Active database connection
     * @param tableName Name of the table
     * @return          Row data closest to the current time or {@code null}
     */
    public static Object[] getActualAtTimeStampData(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT * FROM " + sanitizeTableName(tableName) +
                " WHERE zeitstempel <= CURRENT_TIMESTAMP ORDER BY zeitstempel DESC LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return extractRowData(rs);
            }
        }
        return null;
    }

    //************************************************************************
    // Private helper methods

    /**
     * Helper that extracts all column values of the current row from the given
     * {@link ResultSet} into an {@code Object[]} array.
     */
    private static Object[] extractRowData(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        Object[] data = new Object[columnCount];
        for (int i = 1; i <= columnCount; i++) {
            data[i - 1] = rs.getObject(i);
        }
        return data;
    }

    private static List<String[]> readFile(File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".csv")) {
            return readCsv(file);
        }
        if (name.endsWith(".xls")) {
            return readExcelXls(file);
        }
        if (name.endsWith(".xlsx")) {
            System.err.println("XLSX-Dateien (.xlsx) werden von dieser Methode derzeit nicht unterstützt. Bitte konvertieren Sie zu XLS oder CSV, oder erweitern Sie die Methode."); // XLSX files (.xlsx) are currently not supported by this method. Please convert to XLS or CSV, or extend the method.
            throw new IOException("XLSX-Dateien (.xlsx) werden von dieser Methode derzeit nicht unterstützt."); // XLSX files (.xlsx) are currently not supported by this method.
        }
        throw new IOException("Nicht unterstützter Dateityp: " + file.getName() + ". Nur .csv und .xls werden unterstützt."); // Unsupported file type: ... Only .csv and .xls are supported.
    }

    private static List<String[]> readCsv(File file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) { // Skip empty lines
                    rows.add(parseCsvLine(line));
                }
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
                if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
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

    private static List<String[]> readExcelXls(File file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (InputStream in = new FileInputStream(file);
             HSSFWorkbook wb = new HSSFWorkbook(in)) {

            if (wb.getNumberOfSheets() == 0) {
                System.out.println("Warnung: Die Excel-Datei " + file.getName() + " enthält keine Sheets."); // Warning: The Excel file ... contains no sheets.
                return rows;
            }
            HSSFSheet sheet = wb.getSheetAt(0);

            int firstRowIdx = sheet.getFirstRowNum();
            int lastRowIdx = sheet.getLastRowNum();

            for (int r = firstRowIdx; r <= lastRowIdx; r++) {
                HSSFRow row = sheet.getRow(r);
                if (row == null) {
                    // Add an empty row if the header is expected and the first row is null
                    if (r == firstRowIdx && rows.isEmpty()){
                        rows.add(new String[0]); // Empty header
                    }
                    continue;
                }

                short firstCellIdx = row.getFirstCellNum();
                short lastCellIdx = row.getLastCellNum();

                if (lastCellIdx < 0 && r == firstRowIdx && rows.isEmpty()){
                    rows.add(new String[0]); // Empty header for a row without cells
                    continue;
                }
                if (lastCellIdx < 0) continue;


                List<String> values = new ArrayList<>();
                int maxCols = 0; // Determine the maximum number of columns from the header
                if (!rows.isEmpty() && rows.get(0) != null) {
                    maxCols = rows.get(0).length;
                } else if (r == firstRowIdx) { // For the header row
                    for (int c = firstCellIdx; c < lastCellIdx; c++) {
                        values.add(getCellValueAsString(row.getCell(c)));
                    }
                    if (!values.stream().allMatch(String::isEmpty)) { // Only add if header is not completely empty
                        rows.add(values.toArray(new String[0]));
                    } else if (rows.isEmpty()) { // If header is completely empty, add empty array for header
                        rows.add(new String[0]);
                    }
                    continue; // Header row processed
                }


                // For data rows, iterate up to the maximum number of columns of the header or the current row
                int currentMaxCell = Math.max(maxCols, lastCellIdx);
                values.clear(); // Ensure the list is empty for each new row

                for (int c = 0; c < currentMaxCell; c++) { // Start at 0 for consistency
                    if (c >= firstCellIdx && c < lastCellIdx) {
                        values.add(getCellValueAsString(row.getCell(c)));
                    } else {
                        values.add(""); // Add empty strings for missing cells
                    }
                }

                // Only add if the row has content or it is the first (header) row
                if (!values.stream().allMatch(String::isEmpty) || (rows.isEmpty() && r == firstRowIdx) ) {
                    rows.add(values.toArray(new String[0]));
                }
            }
        }
        return rows;
    }


    private static String getCellValueAsString(HSSFCell cell) {
        if (cell == null) {
            return "";
        }
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }

        switch (cellType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue) && !Double.isInfinite(numValue)) {
                    return String.valueOf((long) numValue);
                } else {
                    return String.valueOf(numValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return "";
            case ERROR:
                return "ERROR_IN_CELL";
            default:
                return "";
        }
    }

    // Attempts to parse a value as java.sql.Time using a set of common patterns
    private static Time tryParseTime(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String[] patterns = {
                "HH:mm",
                "HH:mm:ss",
                "dd.MM.HH:mm",
                "dd.MM.HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd HH:mm:ss"
        };
        for (String p : patterns) {
            try {
                DateTimeFormatter fmt;
                // patterns without year (e.g., dd.MM.HH:mm) need a default year
                if (!p.contains("y")) {
                    fmt = new DateTimeFormatterBuilder()
                            .appendPattern(p)
                            .parseDefaulting(ChronoField.YEAR, 2000)
                            .toFormatter();
                } else {
                    fmt = DateTimeFormatter.ofPattern(p);
                }

                if (p.contains("d") || p.contains("M") || p.contains("y")) {
                    LocalDateTime dt = LocalDateTime.parse(value, fmt);
                    return Time.valueOf(dt.toLocalTime());
                } else {
                    LocalTime t = LocalTime.parse(value, fmt);
                    return Time.valueOf(t);
                }
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    // Attempts to parse a value as java.sql.Timestamp using common patterns
    private static Timestamp tryParseTimestamp(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "dd.MM.yyyy HH:mm",
                "dd.MM.yyyy HH:mm:ss",
                "dd.MM.HH:mm",
                "dd.MM.HH:mm:ss"
        };
        for (String p : patterns) {
            try {
                DateTimeFormatter fmt;
                // If the pattern lacks a year, provide a default year for parsing
                if (!p.contains("y")) {
                    fmt = new DateTimeFormatterBuilder()
                            .appendPattern(p)
                            .parseDefaulting(ChronoField.YEAR, 2000)
                            .toFormatter();
                } else {
                    fmt = DateTimeFormatter.ofPattern(p);
                }
                LocalDateTime dt = LocalDateTime.parse(value, fmt);
                return Timestamp.valueOf(dt);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    // Determines SQL column types based on header names and sample values
    private static String[] guessColumnTypes(String[] headers, List<String[]> dataRows) {
        String[] types = new String[headers.length];
        for (int col = 0; col < headers.length; col++) {
            String header = headers[col] != null ? headers[col].toLowerCase() : "";
            boolean timeCandidate = header.contains("zeit") || header.contains("time");
            boolean timestampCandidate = header.contains("timestamp") || header.contains("zeitstempel");

            boolean allInts = true;
            boolean allNumbers = true;

            for (String[] row : dataRows) {
                if (col >= row.length) continue;
                String value = row[col];
                if (value == null || value.trim().isEmpty()) continue;

                if (timestampCandidate && tryParseTimestamp(value) != null) {
                    continue;
                }
                if (timeCandidate && tryParseTime(value) != null) {
                    continue;
                }

                try {
                    Integer.parseInt(value.trim());
                } catch (NumberFormatException e) {
                    allInts = false;
                    try {
                        Double.parseDouble(value.trim());
                    } catch (NumberFormatException ex) {
                        allNumbers = false;
                    }
                }
            }

            if (timestampCandidate) {
                types[col] = "TIMESTAMP";
            } else if (timeCandidate) {
                types[col] = "TIME";
            } else if (allInts) {
                types[col] = "INTEGER";
            } else if (allNumbers) {
                types[col] = "DOUBLE";
            } else {
                types[col] = "VARCHAR(255)";
            }
        }
        return types;
    }

    private static String deriveTableNameFromFile(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            name = name.substring(0, dotIndex);
        }
        return sanitizeTableName(name); // Sanitize directly here
    }

    private static void createTableIfNotExists(Connection conn, String tableName, String[] headers, String[] columnTypes) throws SQLException {
        // The table name should already be sanitized if it comes from deriveTableNameFromFile,
        // but re-sanitizing doesn't hurt if it comes from elsewhere.
        String sanitizedTableName = sanitizeTableName(tableName);

        if (headers == null || headers.length == 0) {
            System.err.println("Warnung: Header sind leer für Tabelle '" + sanitizedTableName + "'. Erstelle Tabelle ohne Spalten, was zu Fehlern führen kann.");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(sanitizedTableName).append(" (");

        if (headers != null && headers.length > 0) {
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i];
                String type = (columnTypes != null && columnTypes.length > i) ? columnTypes[i] : "VARCHAR(255)";
                if (header == null || header.trim().isEmpty()) {
                    // Skip empty headers or replace them with a placeholder
                    // A placeholder is used here to avoid SQL errors
                    System.out.println("Warnung: Leerer Header an Position " + i + " für Tabelle " + sanitizedTableName + ". Verwende Platzhalter 'col_" + i + "'."); // Warning: Empty header at position ... for table ... Using placeholder 'col_...'.
                    header = "col_" + i;
                }
                if (i > 0) sql.append(", ");
                sql.append(sanitizeColumnName(header)).append(" ").append(type);
            }
        } else {
            return; // No columns, so don't create a table
        }
        sql.append(")");
        System.out.println("SQL zum Erstellen/Überprüfen der Tabelle: " + sql.toString()); // SQL for creating/checking the table

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql.toString());
        }
    }

    private static void dropTableIfExists(Connection conn, String tableName) throws SQLException {
        // The table name should already be sanitized.
        String sql = "DROP TABLE IF EXISTS " + sanitizeTableName(tableName);
        System.out.println("SQL zum Löschen der Tabelle: " + sql); // SQL for deleting the table
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private static void insertData(Connection conn, String tableName, String[] headers, String[] columnTypes, List<String[]> dataRows) throws SQLException {
        if (dataRows.isEmpty()) {
            System.out.println("Keine Datenzeilen zum Einfügen in Tabelle " + tableName); // No data rows to insert into table
            return;
        }
        if (headers == null || headers.length == 0) {
            System.err.println("FEHLER: Kann Daten nicht ohne Header-Informationen einfügen für Tabelle: " + tableName); // ERROR: Cannot insert data without header information for table:
            // It's not safe to map data without header information.
            throw new SQLException("Header sind erforderlich, um Daten einzufügen."); // Headers are required to insert data.
        }
        String sanitizedTableName = sanitizeTableName(tableName);

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(sanitizedTableName).append(" (");
        for(int i=0; i < headers.length; i++){
            String header = headers[i];
            if (header == null || header.trim().isEmpty()) { // Ensure column names are valid
                System.out.println("Warnung: Leerer Header an Position " + i + " beim Daten einfügen in " + sanitizedTableName + ". Verwende Platzhalter 'col_" + i + "'."); // Warning: Empty header at position ... when inserting data into ... Using placeholder 'col_...'.
                header = "col_" + i;
            }
            if(i > 0) sql.append(", ");
            sql.append(sanitizeColumnName(header));
        }
        sql.append(") VALUES (");
        // Number of placeholders '?' must match the number of (valid) headers
        sql.append(String.join(",", Collections.nCopies(headers.length, "?")));
        sql.append(")");
        System.out.println("SQL zum Einfügen von Daten-Batches: " + sql.toString()); // SQL for inserting data batches

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int validRowsProcessed = 0;
            for (String[] row : dataRows) {
                if (row.length != headers.length) {
                    System.out.println("Warnung: Zeile hat " + row.length + " Werte, aber es gibt " + headers.length + " Header. Überspringe Zeile: " + Arrays.toString(row)); // Warning: Row has ... values, but there are ... headers. Skipping row:
                    // Optional: pad row or throw error
                    // continue; // Skip this row
                }
                for (int i = 0; i < headers.length; i++) {
                    String value = (i < row.length) ? row[i] : null; // Null for missing values at the end of the row
                    String type = (columnTypes != null && columnTypes.length > i) ? columnTypes[i] : "VARCHAR(255)";
                    if (value == null || value.trim().isEmpty()) {
                        ps.setNull(i + 1, Types.VARCHAR);
                        continue;
                    }
                    switch (type) {
                        case "INTEGER":
                            try {
                                ps.setInt(i + 1, Integer.parseInt(value.trim()));
                            } catch (NumberFormatException e) {
                                ps.setNull(i + 1, Types.INTEGER);
                            }
                            break;
                        case "DOUBLE":
                            try {
                                ps.setDouble(i + 1, Double.parseDouble(value.trim()));
                            } catch (NumberFormatException e) {
                                ps.setNull(i + 1, Types.DOUBLE);
                            }
                            break;
                        case "TIME":
                            Time t = tryParseTime(value);
                            if (t != null) ps.setTime(i + 1, t); else ps.setNull(i + 1, Types.TIME);
                            break;
                        case "TIMESTAMP":
                            Timestamp ts = tryParseTimestamp(value);
                            if (ts != null) ps.setTimestamp(i + 1, ts); else ps.setNull(i + 1, Types.TIMESTAMP);
                            break;
                        default:
                            ps.setString(i + 1, value);
                    }
                }
                ps.addBatch();
                validRowsProcessed++;
            }
            if (validRowsProcessed > 0) {
                ps.executeBatch();
                System.out.println(validRowsProcessed + " Datenzeilen-Batches verarbeitet für Tabelle " + sanitizedTableName); // ... data row batches processed for table
            } else {
                System.out.println("Keine gültigen Datenzeilen zum Einfügen in " + sanitizedTableName + " gefunden nach Filterung."); // No valid data rows found for insertion into ... after filtering.
            }
        }
    }

    private static String sanitizeIdentifier(String name, String prefix) {
        if (name == null || name.trim().isEmpty()) {
            // Generate a unique name if the original name is empty
            return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
        String sanitized = name.trim()
                .replaceAll("[^A-Za-z0-9_\\s-]", "") // Initially allow hyphens and spaces
                .replaceAll("[\\s-]+", "_") // Replace spaces and hyphens with single underscores
                .replaceAll("[^A-Za-z0-9_]", ""); // Remove all remaining invalid characters

        Set<String> keywords = new HashSet<>(Arrays.asList("TABLE", "SELECT", "INSERT", "UPDATE", "DELETE", "WHERE", "FROM", "GROUP", "ORDER", "INDEX", "KEY", "PRIMARY", "FOREIGN", "USER", "VALUES", "COLUMN"));
        if (keywords.contains(sanitized.toUpperCase())) {
            sanitized = prefix + "_" + sanitized;
        }

        if (sanitized.isEmpty() || Character.isDigit(sanitized.charAt(0))) {
            sanitized = prefix + "_" + sanitized;
        }
        // Maximum length (HSQLDB specific, but generally a good idea)
        if (sanitized.length() > 128) {
            sanitized = sanitized.substring(0, 128);
        }
        return sanitized.toLowerCase();
    }

    private static String sanitizeTableName(String name) {
        return sanitizeIdentifier(name, "tbl");
    }

    private static String sanitizeColumnName(String name) {
        return sanitizeIdentifier(name, "col");
    }
}