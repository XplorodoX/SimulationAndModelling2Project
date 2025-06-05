import java.io.File;
import java.sql.Connection;

/**
 * Kommandozeilen-Tool für erweiterte Datenbankoperationen.
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
                    System.err.println("Unbekannter Befehl: " + command);
                    printUsage();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleFileImport(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: import-file <file> [tableName] [jdbcUrl] [replace]");
            return;
        }

        File file = new File(args[1]);
        String tableName = args.length > 2 ? args[2] : null;
        String jdbcUrl = args.length > 3 ? args[3] : null;
        boolean replace = args.length > 4 && "true".equalsIgnoreCase(args[4]);

        try (Connection conn = jdbcUrl != null ?
                AnyLogicDBUtil.openConnection(jdbcUrl) :
                AnyLogicDBUtil.openConnection()) {

            AnyLogicDBUtil.importTableFromFile(conn, tableName, file, replace);
        }
    }

    private static void handleDirectoryImport(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: import-dir <directory> [jdbcUrl] [replace]");
            return;
        }

        File dir = new File(args[1]);
        String jdbcUrl = args.length > 2 ? args[2] : null;
        boolean replace = args.length > 3 && "true".equalsIgnoreCase(args[3]);

        try (Connection conn = jdbcUrl != null ?
                AnyLogicDBUtil.openConnection(jdbcUrl) :
                AnyLogicDBUtil.openConnection()) {

            AnyLogicDBUtil.importTablesFromDirectory(conn, dir, replace);
        }
    }

    private static void handleShowTable(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: show-table <tableName> [maxRows] [jdbcUrl]");
            return;
        }

        String tableName = args[1];
        int maxRows = args.length > 2 ? Integer.parseInt(args[2]) : 100;
        String jdbcUrl = args.length > 3 ? args[3] : null;

        try (Connection conn = jdbcUrl != null ?
                AnyLogicDBUtil.openConnection(jdbcUrl) :
                AnyLogicDBUtil.openConnection()) {

            AnyLogicDBUtil.displayTable(conn, tableName, maxRows);
        }
    }

    private static void handleListTables(String[] args) throws Exception {
        String jdbcUrl = args.length > 1 ? args[1] : null;

        try (Connection conn = jdbcUrl != null ?
                AnyLogicDBUtil.openConnection(jdbcUrl) :
                AnyLogicDBUtil.openConnection()) {

            AnyLogicDBUtil.listTables(conn);
        }
    }

    private static void handleCreateTable(String[] args) throws Exception {
        System.err.println("create-table Befehl noch nicht implementiert - verwenden Sie die Java API");
    }

    private static void handleInsertData(String[] args) throws Exception {
        System.err.println("insert-data Befehl noch nicht implementiert - verwenden Sie die Java API");
    }

    private static void printUsage() {
        System.out.println("Enhanced CSV/Excel Database Importer");
        System.out.println("Usage: java EnhancedCsvImporter <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  import-file <file> [tableName] [jdbcUrl] [replace]");
        System.out.println("    Importiert eine einzelne CSV/Excel Datei");
        System.out.println();
        System.out.println("  import-dir <directory> [jdbcUrl] [replace]");
        System.out.println("    Importiert alle CSV/Excel Dateien aus einem Verzeichnis");
        System.out.println();
        System.out.println("  show-table <tableName> [maxRows] [jdbcUrl]");
        System.out.println("    Zeigt den Inhalt einer Tabelle an");
        System.out.println();
        System.out.println("  list-tables [jdbcUrl]");
        System.out.println("    Listet alle Tabellen in der Datenbank auf");
        System.out.println();
        System.out.println("Optionen:");
        System.out.println("  tableName: Name der zu erstellenden Tabelle (Standard: Dateiname)");
        System.out.println("  jdbcUrl: JDBC URL der Datenbank (Standard: In-Memory DB)");
        System.out.println("  replace: true/false - Existierende Tabelle ersetzen (Standard: false)");
    }
}