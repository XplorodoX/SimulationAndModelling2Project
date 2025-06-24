import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PersonEnergy {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/simdata";
    private static final String DB_USER = "user";      
    private static final String DB_PASSWORD = "password";  

    private static final String TABLE_NAME = "household_data";
    private static final String TIME_COLUMN = "utc_timestamp";
    private static final String DATA_COLUMN = "average_per_person_consumption";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Perform the process and return the result for the controller
    public static List<Object[]> getPredictedConsumption(LocalDateTime startTime, LocalDateTime endTime, int numberOfPeople) {
        List<Object[]> results = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            List<Object[]> data = DBRequest.getTimeSeriesData(conn, TABLE_NAME, TIME_COLUMN, DATA_COLUMN, startTime, endTime);

            for (Object[] row : data) {
                Timestamp ts = (Timestamp) row[0];
                Object valueObj = row[1];
                if (valueObj instanceof Number) {
                    double avgPerPerson = ((Number) valueObj).doubleValue();
                    double predicted = avgPerPerson * numberOfPeople;

                    // Test print (remove later)
                    System.out.println(ts + " → Average/person: " + avgPerPerson + " kWh → Predicted for " + numberOfPeople + " people: " + predicted + " kWh");

                    results.add(new Object[]{ts, predicted});
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }

        return results;  // ← to be forwarded to the house controller
    }

    // Optional test main to check before integrating with controller
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter start datetime (YYYY-MM-DD HH:MM:SS): ");
        String startInput = scanner.nextLine();

        System.out.print("Enter end datetime (YYYY-MM-DD HH:MM:SS): ");
        String endInput = scanner.nextLine();

        System.out.print("Enter number of people in the household: ");
        int numberOfPeople = scanner.nextInt();

        LocalDateTime startTime = LocalDateTime.parse(startInput, FORMATTER);
        LocalDateTime endTime = LocalDateTime.parse(endInput, FORMATTER);

        List<Object[]> predictions = getPredictedConsumption(startTime, endTime, numberOfPeople);

        // forward 'predictions' to the house controller, should i just rerturn? whaaaaaaaaaaaaa
    }
}
