import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class AnyLogicDBUtilTest {
    @Test
    public void testImportTableFromFile() throws Exception {
        // create temporary CSV file
        File csv = File.createTempFile("test", ".csv");
        try (PrintWriter pw = new PrintWriter(csv)) {
            pw.println("id,name");
            pw.println("1,Alice");
            pw.println("2,Bob");
        }

        try (Connection conn = AnyLogicDBUtil.openMemoryConnection()) {
            AnyLogicDBUtil.importTableFromFile(conn, "persons", csv, true);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM persons");
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }

        csv.delete();
    }
}
