import java.io.File;
import java.sql.Connection;
import java.sql.Timestamp;

public class DatabaseController {
    private static final String SAMPLE_SIMPLE_CSV_PATH = "Database/sample_csvold.csv";

    public static void main(String[] args) {
        Connection conn = null;
        Boolean init = true;
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

            Timestamp startTime = Timestamp.valueOf("2005-01-04 12:30:00.000000");
            Timestamp endTime = Timestamp.valueOf("2005-03-04 11:45:00.000000");

            Double lol = AnyLogicDBUtil.getDataAtTimeStampRange(conn, "sample_csv", "Time", startTime, endTime);

            // Example retrieval of the last kWh value before a timestamp
            Timestamp queryTime = Timestamp.valueOf("2005-01-01 08:30:00");
            Double singleValue = AnyLogicDBUtil.getActualAtTimeStampData(conn, "sample_csv", "Time", queryTime);

            if (lol != null && lol != 0) {
                System.out.println("Sum of kWh between " + startTime + " and " + endTime + ": " + lol + " kWh");
            } else {
                System.out.println("No data found for the specified time range.");
            }

            if (singleValue != null) {
                System.out.println("kWh at " + queryTime + ": " + singleValue);
            } else {
                System.out.println("No data found at " + queryTime);
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
