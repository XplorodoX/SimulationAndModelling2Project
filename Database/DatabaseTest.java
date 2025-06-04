import java.io.File;
import java.sql.Connection;

public class DatabaseTest {
    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "jdbc:hsqldb:mem:test";
        // Running from the Database directory in CI, so the path is relative
        // to the working directory
        File csv = new File("sample_data.csv");
        try (Connection conn = AnyLogicDBUtil.openConnection(url)) {
            AnyLogicDBUtil.importTableFromFile(conn, "sample_table", csv);
            try (var st = conn.createStatement();
                 var rs = st.executeQuery("SELECT COUNT(*) FROM sample_table")) {
                int count = rs.next() ? rs.getInt(1) : -1;
                if (count == 2) {
                    System.out.println("CSV import test passed");
                } else {
                    System.err.println("CSV import test failed: count=" + count);
                }
            }
        }
    }
}
