import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter; // For PrintWriter
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.*;

/**
 * Testclass to Test database functions.
 */
public class DatabaseTest {

    private static final String DATA_DIRECTORY_PATH = "Database/data_test";
    private static final String SAMPLE_CSV_PATH = "Database/sample_products_test.csv";


    public static void main(String[] args) {
        // Test the connection to the ProjektY database
        testProjektYConnection();

        // File import demonstrations (optional, adapt to your environment)
        // Ensure paths are correct and the HSQLDB server is running.
        try {
            // CSV/Excel import from directory
            // Requires a directory DATA_DIRECTORY_PATH with test files
            demonstrateFileImport();

            // Manual table creation and data import
            demonstrateManualDataEntry();

            // Single file with custom table name
            // Requires the file SAMPLE_CSV_PATH
            demonstrateSingleFileImport();

        } catch (Exception e) {
            System.err.println("An error occurred during the demonstrations:");
            e.printStackTrace();
        }
    }

    /**
     * Tests the connection to database.
     */
    private static void testProjektYConnection() {
        System.out.println("\n=== TEST: Connection to ProjektY Database ===");
        Connection conn = null;
        try {
            // Attempt to establish a connection to the ProjektY database
            conn = AnyLogicDBUtil.openProjektYDBConnection();
            System.out.println("Connection to ProjektY database established successfully.");

            // List tables as a test
            AnyLogicDBUtil.listTables(conn);

        } catch (Exception e) {
            System.err.println("ERROR connecting or testing ProjektY database:");
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("Connection to ProjektY database closed.");
                } catch (Exception e) {
                    System.err.println("Error closing ProjektY database connection:");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Demonstrates importing files from a directory.
     * Creates sample CSV files if they don't exist.
     * Imports them into the ProjektY database.
     * Lists tables and displays content of imported tables.
     * @throws Exception if any error occurs during the process.
     */
    private static void demonstrateFileImport() throws Exception {
        System.out.println("\n=== DEMO 1: CSV/Excel Import from Directory ===");
        Connection conn = null;
        try {
            // Establish connection to ProjektY database
            conn = AnyLogicDBUtil.openProjektYDBConnection();
            System.out.println("Demo 1: Connected to ProjektY DB.");

            File dataDir = new File(DATA_DIRECTORY_PATH);
            // Create directory and sample files if they don't exist
            if (!dataDir.exists()) {
                System.out.println("Data directory '" + dataDir.getAbsolutePath() + "' not found, attempting to create it.");
                dataDir.mkdirs(); // Creates the directory and all parent directories
            }
            createSampleCsvFileForDir(new File(dataDir, "dir_sample1.csv"), "ID,Name,Value", "1,Alpha,100\n2,Beta,200");
            createSampleCsvFileForDir(new File(dataDir, "dir_sample2.csv"), "Product;Price", "Apple;1.0\nPear;1.5"); // Test with semicolon

            if (dataDir.exists() && dataDir.isDirectory()) {
                System.out.println("Importing tables from directory: " + dataDir.getAbsolutePath());
                AnyLogicDBUtil.importTablesFromDirectory(conn, dataDir, true); // true = replace tables
                AnyLogicDBUtil.listTables(conn);
                AnyLogicDBUtil.displayTable(conn, "dir_sample1", 5);
                AnyLogicDBUtil.displayTable(conn, "dir_sample2", 5); // Table name will be sanitized
            } else {
                System.out.println("Directory '" + dataDir.getAbsolutePath() + "' does not exist or is not a directory - Demo 1 skipped/incomplete.");
            }
        } catch (Exception e) {
            System.err.println("Error in Demo 1 (File Import):");
            e.printStackTrace();
            throw e; // Rethrow exception to highlight the problem
        } finally {
            if (conn != null) conn.close();
            System.out.println("Demo 1: Connection closed.");
        }
    }

    /**
     * Helper method to create sample CSV files for the directory import example.
     * @param file The file object to create.
     * @param header The header string for the CSV.
     * @param data The data string for the CSV (newline separated for rows).
     * @throws FileNotFoundException if the file cannot be created.
     */
    private static void createSampleCsvFileForDir(File file, String header, String data) throws FileNotFoundException {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
            writer.println(header);
            writer.println(data);
            System.out.println("Sample file created: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error creating sample file " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }

    /**
     * Demonstrates manual table creation and data insertion.
     * Creates an 'employees_test' table and inserts sample data.
     * Displays the table content.
     * @throws Exception if any error occurs.
     */
    private static void demonstrateManualDataEntry() throws Exception {
        System.out.println("\n=== DEMO 2: Manual Table Creation and Data Import ===");
        Connection conn = null;
        try {
            // Establish connection to ProjektY database
            conn = AnyLogicDBUtil.openProjektYDBConnection();
            System.out.println("Demo 2: Connected to ProjektY DB.");

            // 1. Create table manually
            Map<String, String> columns = new LinkedHashMap<>(); // LinkedHashMap maintains insertion order
            columns.put("id", "INTEGER PRIMARY KEY");
            columns.put("name", "VARCHAR(100)");
            columns.put("email", "VARCHAR(255) UNIQUE"); // Unique email
            columns.put("age", "INTEGER");
            columns.put("salary", "DECIMAL(10,2)");
            columns.put("hire_date", "DATE");


            String tableName = "employees_test";
            AnyLogicDBUtil.createTable(conn, tableName, columns, true); // true = replace table

            // 2. Insert data manually
            String[] columnNames = {"id", "name", "email", "age", "salary", "hire_date"};
            List<Object[]> data = Arrays.asList(
                    new Object[]{1, "Max Mustermann", "max.m@example.com", 30, 50000.00, java.sql.Date.valueOf("2020-01-15")},
                    new Object[]{2, "Anna Schmidt", "anna.s@example.com", 28, 55000.00, java.sql.Date.valueOf("2021-06-01")},
                    new Object[]{3, "Peter Weber", "peter.w@example.com", 35, 60000.00, java.sql.Date.valueOf("2019-03-20")},
                    new Object[]{4, "Lisa Müller", "lisa.m@example.com", 26, 48000.00, null} // No hire date
            );

            AnyLogicDBUtil.insertManualData(conn, tableName, columnNames, data);

            // 3. Display table
            AnyLogicDBUtil.displayTable(conn, tableName, 10);
        } catch (Exception e) {
            System.err.println("Error in Demo 2 (Manual Data Entry):");
            e.printStackTrace();
            throw e;
        } finally {
            if (conn != null) conn.close();
            System.out.println("Demo 2: Connection closed.");
        }
    }

    /**
     * Demonstrates importing a single CSV file with a custom table name.
     * Creates a sample CSV file if it doesn't exist.
     * Imports it into 'product_catalog_test' table.
     * Displays the table.
     * @throws Exception if any error occurs.
     */
    private static void demonstrateSingleFileImport() throws Exception {
        System.out.println("\n=== Single File with Custom Table Name ===");
        Connection conn = null;
        try {
            // Establish connection to ProjektY database
            conn = AnyLogicDBUtil.openProjektYDBConnection();
            System.out.println("Connected to ProjektY DB.");

            File csvFile = new File(SAMPLE_CSV_PATH);
            createSampleCsvFileForSingleImport(csvFile);

            if (csvFile.exists()) {
                System.out.println("Importing single file: " + csvFile.getAbsolutePath());
                String customTableName = "product_catalog_test";
                AnyLogicDBUtil.importTableFromFile(conn, customTableName, csvFile, true);

                AnyLogicDBUtil.listTables(conn);
                AnyLogicDBUtil.displayTable(conn, customTableName, 5);
            } else {
                System.out.println("File '" + csvFile.getAbsolutePath() + "' not found - skipped.");
            }
        } catch (Exception e) {
            System.err.println("Error (Single File Import):");
            e.printStackTrace();
            throw e;
        } finally {
            if (conn != null) conn.close();
            System.out.println("Connection closed.");
        }
    }

    /**
     * Helper method to create the sample CSV file for the single file import example.
     * @param file The file object to create/update.
     * @throws FileNotFoundException if the file path is invalid.
     */
    private static void createSampleCsvFileForSingleImport(File file) throws FileNotFoundException {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
            writer.println("product_id,product_name,category,price,stock_count"); // Column name changed to stock_count
            writer.println("101,\"Laptop Dell XPS 15\",Electronics,1499.99,15");
            writer.println("102,\"Ergonomic Office Chair\",Furniture,299.50,8");
            writer.println("103,\"Smart Coffee Maker\",Appliances,99.95,25");
            writer.println("104,\"Wireless Optical Mouse\",Electronics,39.99,50");
            writer.println("105,\"LED Desk Lamp with USB\",Furniture,44.95,0"); // Stock is 0
            System.out.println("Sample file for single import created/updated: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error creating sample file for single import " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }
}
