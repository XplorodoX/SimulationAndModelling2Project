import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Test
    public void testGetDataAtTimeStampRange() throws Exception {
        try (Connection conn = AnyLogicDBUtil.openMemoryConnection()) {
            Map<String, String> columns = new LinkedHashMap<>();
            columns.put("zeitstempel", "TIMESTAMP");
            columns.put("value", "INTEGER");
            AnyLogicDBUtil.createTable(conn, "timeseries_test", columns, true);

            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[]{Timestamp.valueOf("2024-01-01 00:00:00"), 10});
            rows.add(new Object[]{Timestamp.valueOf("2024-01-02 00:00:00"), 20});
            rows.add(new Object[]{Timestamp.valueOf("2024-01-03 00:00:00"), 30});
            rows.add(new Object[]{Timestamp.valueOf("2024-01-04 00:00:00"), 40});
            rows.add(new Object[]{Timestamp.valueOf("2024-01-05 00:00:00"), 50});
            AnyLogicDBUtil.insertManualData(conn, "timeseries_test",
                    new String[]{"zeitstempel", "value"}, rows);

            Timestamp start = Timestamp.valueOf("2024-01-02 00:00:00");
            Timestamp end = Timestamp.valueOf("2024-01-04 00:00:00");
            List<Object[]> result = AnyLogicDBUtil.getDataAtTimeStampRange(
                    conn, "timeseries_test", start, end);

            assertEquals(3, result.size());
            assertEquals(Timestamp.valueOf("2024-01-02 00:00:00"), result.get(0)[0]);
            assertEquals(20, ((Number) result.get(0)[1]).intValue());
            assertEquals(40, ((Number) result.get(2)[1]).intValue());
        }
    }
}
