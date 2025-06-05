import java.io.File;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Command line tool for advanced database operations.
 */
class CsvImporter {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0].toLowerCase();

        try {
            switch (command) {
                case "import-file":
                    handleFileImport(args);
                    break;
                case "import-dir":
                    handleDirectoryImport(args);
                    break;
                case "create-table":
                    handleCreateTable(args);
                    break;
                case "insert-data":
                    handleInsertData(args);
                    break;
                case "show-table":
                    handleShowTable(args);
                    break;
                case "list-tables":
                    handleListTables(args);
                    break;
                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
            }
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleFileImport(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: import-file <file> [tableName] [jdbcUrl] [replaceTrueFalse]");
            return;
        }

        File file = new File(args[1]);
        String tableName = args.length > 2 ? args[2] : null;
        String jdbcUrl = args.length > 3 ? args[3] : null; // explicit URL
        boolean replace = args.length > 4 && "true".equalsIgnoreCase(args[4]);

        // Logic for establishing connection:
        // 1. If jdbcUrl is specified, use it.
        // 2. Otherwise, use the default connection (ProjektY).
        try (Connection conn = jdbcUrl != null && !jdbcUrl.trim().isEmpty() ?
                AnyLogicDBUtil.openConnection(jdbcUrl) : // May require user/pass if your DB needs it
                AnyLogicDBUtil.openProjektYDBConnection()) { // Default to ProjektY

            AnyLogicDBUtil.importTableFromFile(conn, tableName, file, replace);
            System.out.println("File import completed successfully.");
        }
    }

    private static void handleDirectoryImport(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: import-dir <directory> [jdbcUrl] [replaceTrueFalse]");
            return;
        }

        File dir = new File(args[1]);
        String jdbcUrl = args.length > 2 ? args[2] : null;
        boolean replace = args.length > 3 && "true".equalsIgnoreCase(args[3]);

        try (Connection conn = jdbcUrl != null && !jdbcUrl.trim().isEmpty() ?
                AnyLogicDBUtil.openConnection(jdbcUrl) :
                AnyLogicDBUtil.openProjektYDBConnection()) {

            AnyLogicDBUtil.importTablesFromDirectory(conn, dir, replace);
            System.out.println("Directory import completed successfully.");
        }
    }

    private static void handleShowTable(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: show-table <tableName> [maxRows] [jdbcUrl]");
            return;
        }

        String tableName = args[1];
        int maxRows = args.length > 2 ? Integer.parseInt(args[2]) : 100; // Default 100 rows
        String jdbcUrl = args.length > 3 ? args[3] : null;

        try (Connection conn = jdbcUrl != null && !jdbcUrl.trim().isEmpty() ?
                AnyLogicDBUtil.openConnection(jdbcUrl) :
                AnyLogicDBUtil.openProjektYDBConnection()) {

            AnyLogicDBUtil.displayTable(conn, tableName, maxRows);
        }
    }

    private static void handleListTables(String[] args) throws Exception {
        // The first parameter after 'list-tables' is optionally the jdbcUrl
        String jdbcUrl = args.length > 1 ? args[1] : null;

        try (Connection conn = jdbcUrl != null && !jdbcUrl.trim().isEmpty() ?
                AnyLogicDBUtil.openConnection(jdbcUrl) :
                AnyLogicDBUtil.openProjektYDBConnection()) {

            AnyLogicDBUtil.listTables(conn);
        }
    }

    // The following methods are examples and not yet fully implemented
    // for command line use. They would require more argument parsing.
    private static void handleCreateTable(String[] args) throws Exception {
        // Example: java CsvImporter create-table myTable "id INTEGER, name VARCHAR(100)" [jdbcUrl]
        if (args.length < 3) {
            System.err.println("Usage: create-table <tableName> \"column1 TYPE, column2 TYPE\" [jdbcUrl] [replaceTrueFalse]");
            System.err.println("Note: Enclose column definitions in quotes.");
            System.err.println("This command is rudimentary. Use Java API for complex schemas.");
            return;
        }
        String tableName = args[1];
        String columnDefs = args[2]; // e.g., "id INTEGER PRIMARY KEY, name VARCHAR(100)"
        String jdbcUrl = args.length > 3 ? args[3] : null;
        boolean replace = args.length > 4 && "true".equalsIgnoreCase(args[4]);

        Map<String, String> columns = new LinkedHashMap<>();
        try {
            String[] pairs = columnDefs.split(",");
            for (String pair : pairs) {
                String[] parts = pair.trim().split("\\s+", 2);
                if (parts.length == 2) {
                    columns.put(parts[0].trim(), parts[1].trim());
                } else {
                    throw new IllegalArgumentException("Invalid column definition: " + pair);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing column definitions: " + e.getMessage());
            printUsage();
            return;
        }

        if (columns.isEmpty()) {
            System.err.println("No valid columns defined for create-table.");
            return;
        }

        try (Connection conn = jdbcUrl != null && !jdbcUrl.trim().isEmpty() ?
                AnyLogicDBUtil.openConnection(jdbcUrl) :
                AnyLogicDBUtil.openProjektYDBConnection()) {
            AnyLogicDBUtil.createTable(conn, tableName, columns, replace);
            System.out.println("Table '" + tableName + "' successfully processed/created.");
        }
    }

    private static void handleInsertData(String[] args) throws Exception {
        // Example: java CsvImporter insert-data myTable "id,name" "1,'Max';2,'Anna'" [jdbcUrl]
        // This is a very simplified example. A robust implementation would be more complex.
        if (args.length < 4) {
            System.err.println("Usage: insert-data <tableName> \"column1,column2\" \"value1.1,value1.2;value2.1,value2.2\" [jdbcUrl]");
            System.err.println("Note: Enclose columns and value sets in quotes. Separate values with commas, datasets with semicolons.");
            System.err.println("This command is rudimentary. Use Java API for complex inserts.");
            return;
        }
        String tableName = args[1];
        String[] columnNames = args[2].split(",");
        for (int i = 0; i < columnNames.length; i++) columnNames[i] = columnNames[i].trim();

        String[] dataRowsStr = args[3].split(";");
        List<Object[]> data = new java.util.ArrayList<>();
        for (String rowStr : dataRowsStr) {
            data.add(rowStr.split(",")); // Simple assumption: all values are strings
        }
        String jdbcUrl = args.length > 4 ? args[4] : null;

        if (data.isEmpty()) {
            System.err.println("No data to insert.");
            return;
        }

        try (Connection conn = jdbcUrl != null && !jdbcUrl.trim().isEmpty() ?
                AnyLogicDBUtil.openConnection(jdbcUrl) :
                AnyLogicDBUtil.openProjektYDBConnection()) {
            AnyLogicDBUtil.insertManualData(conn, tableName, columnNames, data);
            System.out.println("Data successfully inserted into table '" + tableName + "'.");
        }
    }

    private static void printUsage() {
        System.out.println("Advanced CSV/Excel Database Importer & Manager");
        System.out.println("Usage: java CsvImporter <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  import-file <file> [tableName] [jdbcUrl] [replaceTrueFalse]");
        System.out.println("    Imports a single CSV/Excel (.xls) file.");
        System.out.println("    Example: java CsvImporter import-file data.csv my_table");
        System.out.println();
        System.out.println("  import-dir <directory> [jdbcUrl] [replaceTrueFalse]");
        System.out.println("    Imports all CSV/Excel (.xls) files from a directory.");
        System.out.println();
        System.out.println("  create-table <tableName> \"column1 TYPE, column2 TYPE\" [jdbcUrl] [replaceTrueFalse]");
        System.out.println("    Creates a new table. Enclose column definitions in \"\".");
        System.out.println("    Example: java CsvImporter create-table users \"id INTEGER, name VARCHAR(50)\"");
        System.out.println();
        System.out.println("  insert-data <tableName> \"column1,column2\" \"value1.1,value1.2;value2.1,value2.2\" [jdbcUrl]");
        System.out.println("    Inserts data manually. Enclose columns and values in \"\". Separate datasets with ;.");
        System.out.println("    Example: java CsvImporter insert-data users \"id,name\" \"1,'Alice';2,'Bob'\"");
        System.out.println();
        System.out.println("  show-table <tableName> [maxRows] [jdbcUrl]");
        System.out.println("    Displays the content of a table (Default: 100 rows).");
        System.out.println();
        System.out.println("  list-tables [jdbcUrl]");
        System.out.println("    Lists all tables in the database.");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  tableName: Name of the target table (Default: filename without extension).");
        System.out.println("  jdbcUrl: JDBC URL of the target database.");
        System.out.println("           (Default: Connects to ProjektY DB: " + ")"); // Assuming AnyLogicDBUtil.getProjektYDB_JDBC_URL() or similar exists
        System.out.println("  replaceTrueFalse: 'true' or 'false' - Replaces existing table (Default: false).");
        System.out.println("  maxRows: Maximum number of rows to display for 'show-table'.");
    }
}
