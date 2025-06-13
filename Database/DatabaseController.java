import java.io.File;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.List;

public class DatabaseController {
    private static final String SAMPLE_SIMPLE_CSV_PATH = "Database/sample_csv.csv";

    public static void main(String[] args) {
        Connection conn = null;
        Boolean importet = false;
        File csvFile = new File(SAMPLE_SIMPLE_CSV_PATH);

        try {
            conn = AnyLogicDBUtil.openProjektYDBConnection();
            // Example usage of AnyLogicDBUtil methods

            if (csvFile.exists() && importet == true) {
                System.out.println("Importing file: " + csvFile.getAbsolutePath());
                AnyLogicDBUtil.importTableFromFile(conn, null, csvFile, true);

                //AnyLogicDBUtil.listTables(conn);
                //AnyLogicDBUtil.displayTable(conn, "sample_csv", 5);
            } else {
                System.out.println("File not found or import not set to true");
            }

            Timestamp startTime = Timestamp.valueOf("2005-01-01 08:30:00");
            Timestamp endTime = Timestamp.valueOf("2005-01-01 16:50:00");

            Double lol = AnyLogicDBUtil.getDataAtTimeStampRange(conn, "sample_csv", "zeit", startTime, endTime);

            if (lol != null && lol != 0) {
                System.out.println("Sum of kWh between " + startTime + " and " + endTime + ": " + lol);
            } else {
                System.out.println("No data found for the specified time range.");
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
