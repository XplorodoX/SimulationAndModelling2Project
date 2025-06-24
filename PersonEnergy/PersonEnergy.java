import java.sql.Connection;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class PersonEnergy {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 1. Take user input
        System.out.print("Enter start time (YYYY-MM-DD HH:MM:SS): ");
        String startInput = scanner.nextLine();
        System.out.print("Enter end time (YYYY-MM-DD HH:MM:SS): ");
        String endInput = scanner.nextLine();
        System.out.print("Enter number of people in household: ");
        int people = scanner.nextInt();

        // 2. Parse timestamps
        LocalDateTime startTime = LocalDateTime.parse(startInput, formatter);
        LocalDateTime endTime = LocalDateTime.parse(endInput, formatter);

        // 3. Query DB using groupmate’s DBRequest class
        try (DBRequest.DBConnection dbConn = new DBRequest.DBConnection()) {
            Connection conn = dbConn.getConnection();

            List<Object[]> data = DBRequest.getTimeSeriesData(
                conn,
                "household_data",                       // table name
                "utc_timestamp",                        // timestamp column
                "average_per_person_consumption",       // data column
                startTime,
                endTime
            );

            double total = 0.0;
            for (Object[] row : data) {
                Timestamp ts = (Timestamp) row[0];
                Object val = row[1];

                if (val instanceof Number) {
                    double perPerson = ((Number) val).doubleValue();
                    double result = perPerson * people;
                    total += result;

                    // 4. Output or forward to house controller
                    System.out.println(ts + " -> " + result + " kWh");
                }
            }

            System.out.println("Total Predicted Consumption: " + String.format("%.2f", total) + " kWh");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
