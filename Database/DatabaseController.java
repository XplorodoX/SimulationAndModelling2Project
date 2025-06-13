import java.io.File;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.List;

public class DatabaseController {
    private static final String SAMPLE_SIMPLE_CSV_PATH = "Database/sample_csv.csv";

    public static void main(String[] args) {
        Connection conn = null;
        File csvFile = new File(SAMPLE_SIMPLE_CSV_PATH);

        try {
            conn = AnyLogicDBUtil.openProjektYDBConnection();
            // Example usage of AnyLogicDBUtil methods

            if (csvFile.exists()) {
                System.out.println("Importing file: " + csvFile.getAbsolutePath());
                AnyLogicDBUtil.importTableFromFile(conn, null, csvFile, true);

                AnyLogicDBUtil.listTables(conn);
                AnyLogicDBUtil.displayTable(conn, "sample_csv", 5);
            } else {
                System.out.println("File '" + csvFile.getAbsolutePath() + "' not found - Demo 4 skipped.");
            }

            Timestamp startTime = Timestamp.valueOf("2005-01-01 09:30:00");
            Timestamp endTime = Timestamp.valueOf("2005-01-01 15:40:00");

            // sample_csv.csv uses the column name "zeit" for timestamps
            AnyLogicDBUtil.getDataAtTimeStampRange(conn, "sample_csv", "zeit", startTime, endTime);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
