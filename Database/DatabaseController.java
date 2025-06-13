import java.io.File;
import java.sql.Connection;
import java.sql.Timestamp;

public class DatabaseController {
    private static final String SAMPLE_SIMPLE_CSV_PATH = "Database/sample_csv.csv";

    public static void main(String[] args) {
        Connection conn = null;
        Boolean init = false;
        File csvFile = new File(SAMPLE_SIMPLE_CSV_PATH);

        try {
            conn = AnyLogicDBUtil.openProjektYDBConnection();
            // Example usage of AnyLogicDBUtil methods

            if (csvFile.exists() && init == true) {
                System.out.println("Importing file: " + csvFile.getAbsolutePath());
                AnyLogicDBUtil.importTableFromFile(conn, null, csvFile, true);

                //AnyLogicDBUtil.listTables(conn);
                //AnyLogicDBUtil.displayTable(conn, "sample_csv", 5);
            } else {
                System.out.println("File not found or import not set to true");
            }

            Timestamp startTime = Timestamp.valueOf("2005-01-01 16:30:00");
            Timestamp endTime = Timestamp.valueOf("2005-01-01 16:50:00");

            Double lol = AnyLogicDBUtil.getDataAtTimeStampRange(conn, "sample_csv", "zeit", startTime, endTime);

            // Example retrieval of the last kWh value before a timestamp
            Timestamp queryTime = Timestamp.valueOf("2005-01-01 16:40:00");
            Double singleValue = AnyLogicDBUtil.getActualAtTimeStampData(conn, "sample_csv", "zeit", queryTime);

            if (lol != null && lol != 0) {
                System.out.println("Sum of kWh between " + startTime + " and " + endTime + ": " + lol + " kWh");
            } else {
                System.out.println("No data found for the specified time range.");
            }

            if (singleValue != null) {
                System.out.println("kWh at or before " + queryTime + ": " + singleValue);
            } else {
                System.out.println("No data found at or before " + queryTime);
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
