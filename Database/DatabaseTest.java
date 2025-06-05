import java.io.File;
import java.sql.Connection;
import java.util.*;

/**
 * Demo-Klasse die die erweiterten Datenbankfunktionen demonstriert.
 */
public class DatabaseTest {

    public static void main(String[] args) {
        try {
            // Beispiel 1: CSV/Excel Import aus Verzeichnis
            demonstrateFileImport();

            // Beispiel 2: Manuelle Tabellenerstellung und Datenimport
            demonstrateManualDataEntry();

            // Beispiel 3: Einzelne Datei mit benutzerdefiniertem Tabellennamen
            demonstrateSingleFileImport();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void demonstrateFileImport() throws Exception {
        System.out.println("\n=== DEMO 1: CSV/Excel Import aus Verzeichnis ===");

        try (Connection conn = AnyLogicDBUtil.openConnection()) {
            // Alle CSV/Excel Dateien aus einem Verzeichnis importieren
            File dataDir = new File("data"); // Angenommen es gibt ein 'data' Verzeichnis

            if (dataDir.exists() && dataDir.isDirectory()) {
                AnyLogicDBUtil.importTablesFromDirectory(conn, dataDir, true);
                AnyLogicDBUtil.listTables(conn);
            } else {
                System.out.println("Verzeichnis 'data' nicht gefunden - Demo übersprungen");
            }
        }
    }

    private static void demonstrateManualDataEntry() throws Exception {
        System.out.println("\n=== DEMO 2: Manuelle Tabellenerstellung und Datenimport ===");

        try (Connection conn = AnyLogicDBUtil.openConnection()) {
            // 1. Tabelle manuell erstellen
            Map<String, String> columns = new LinkedHashMap<>();
            columns.put("id", "INTEGER PRIMARY KEY");
            columns.put("name", "VARCHAR(100)");
            columns.put("email", "VARCHAR(255)");
            columns.put("age", "INTEGER");
            columns.put("salary", "DECIMAL(10,2)");

            AnyLogicDBUtil.createTable(conn, "employees", columns, true);

            // 2. Daten manuell einfügen
            String[] columnNames = {"id", "name", "email", "age", "salary"};
            List<Object[]> data = Arrays.asList(
                    new Object[]{1, "Max Mustermann", "max@example.com", 30, 50000.00},
                    new Object[]{2, "Anna Schmidt", "anna@example.com", 28, 55000.00},
                    new Object[]{3, "Peter Weber", "peter@example.com", 35, 60000.00},
                    new Object[]{4, "Lisa Müller", "lisa@example.com", 26, 48000.00}
            );

            AnyLogicDBUtil.insertManualData(conn, "employees", columnNames, data);

            // 3. Tabelle anzeigen
            AnyLogicDBUtil.displayTable(conn, "employees", 10);
        }
    }

    private static void demonstrateSingleFileImport() throws Exception {
        System.out.println("\n=== DEMO 3: Einzelne Datei mit benutzerdefiniertem Tabellennamen ===");

        try (Connection conn = AnyLogicDBUtil.openConnection()) {
            // Sample CSV erstellen für Demo
            createSampleCsvFile();

            File csvFile = new File("sample_products.csv");
            if (csvFile.exists()) {
                // Import mit benutzerdefiniertem Tabellennamen
                AnyLogicDBUtil.importTableFromFile(conn, "product_catalog", csvFile, true);

                // Tabelle anzeigen
                AnyLogicDBUtil.displayTable(conn, "product_catalog", 5);

                // Cleanup
                csvFile.delete();
            }
        }
    }

    private static void createSampleCsvFile() throws Exception {
        try (java.io.PrintWriter writer = new java.io.PrintWriter("sample_products.csv")) {
            writer.println("product_id,product_name,category,price,stock");
            writer.println("1,\"Laptop Dell XPS\",Electronics,1299.99,15");
            writer.println("2,\"Office Chair\",Furniture,249.50,8");
            writer.println("3,\"Coffee Maker\",Appliances,89.95,25");
            writer.println("4,\"Wireless Mouse\",Electronics,29.99,50");
            writer.println("5,\"Desk Lamp\",Furniture,34.95,12");
        }
    }
}