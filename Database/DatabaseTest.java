import java.io.File;

public class DatabaseTest {
    public static void main(String[] args) throws Exception {
        Databank db = new Databank();
        db.setElectricityPrice(42.0);

        String url = args.length > 0 ? args[0] : "jdbc:hsqldb:mem:testdb";
        try (var conn = AnyLogicDBUtil.openConnection(url)) {
            AnyLogicDBUtil.createSchema(conn);
            AnyLogicDBUtil.insertDatabank(conn, "test", db);

            double price = AnyLogicDBUtil.loadElectricityPrice(conn, "test");
            if (price == db.getElectricityPrice()) {
                System.out.println("Test passed: price=" + price);
            } else {
                System.err.println("Test failed: expected " + db.getElectricityPrice() + " but got " + price);
            }

            // Import sample CSV file and verify row count
            File csv = new File("sample_data.csv");
            AnyLogicDBUtil.importTableFromFile(conn, "sample_table", csv);
            try (var st = conn.createStatement();
                 var rs = st.executeQuery("SELECT COUNT(*) FROM sample_table")) {
                if (rs.next() && rs.getInt(1) == 2) {
                    System.out.println("CSV import test passed");
                } else {
                    System.err.println("CSV import test failed");
                }
            }
        }
    }
}
