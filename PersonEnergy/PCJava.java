import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.Serializable;

public class PCJava implements Serializable {
    private static final long serialVersionUID = 1L;

    private PC owner;
    private int numberOfPeople;

    private static final String TABLE_NAME = "household_data";
    private static final String TIME_COLUMN = "utc_timestamp";
    private static final String DATA_COLUMN = "average_per_person_consumption";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Constructor that sets owner and number of people
    public PCJava(PC owner, int numberOfPeople) {
        this.owner = owner;
        this.numberOfPeople = numberOfPeople;
    }

    // Returns predicted consumption for current time step using getActualValue
    public double getPredictedConsumptionNow() {
        try (DBRequest.DBConnection dbConn = new DBRequest.DBConnection()) {
            Connection conn = dbConn.getConnection();

            // Use current simulation time from AnyLogic
            LocalDateTime now = DateTimeConversionJava.getTimeNow(owner);

            // Get the most recent known value before or at the current time
            Object valueObj = DBRequest.getActualValue(
                conn, TABLE_NAME, TIME_COLUMN, DATA_COLUMN, now
            );

            if (valueObj instanceof Number) {
                double avgPerPerson = ((Number) valueObj).doubleValue();
                return avgPerPerson * numberOfPeople;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0.0; // fallback if no value or error
    }

    // Returns predicted consumption for a given time range
    public List<Object[]> getPredictedConsumption(LocalDateTime startTime, LocalDateTime endTime) {
        List<Object[]> results = new ArrayList<>();

        try (DBRequest.DBConnection dbConn = new DBRequest.DBConnection()) {
            Connection conn = dbConn.getConnection();

            List<Object[]> data = DBRequest.getTimeSeriesData(
                conn, TABLE_NAME, TIME_COLUMN, DATA_COLUMN,
                startTime, endTime
            );

            for (Object[] row : data) {
                Timestamp ts = (Timestamp) row[0];
                Object valueObj = row[1];
                if (valueObj instanceof Number) {
                    double avgPerPerson = ((Number) valueObj).doubleValue();
                    double predicted = avgPerPerson * numberOfPeople;
                    results.add(new Object[]{ts, predicted});
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    // Optional utility to test from console
    public void testWithScanner() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter start datetime (YYYY-MM-DD HH:MM:SS): ");
        String startInput = scanner.nextLine();

        System.out.print("Enter end datetime (YYYY-MM-DD HH:MM:SS): ");
        String endInput = scanner.nextLine();

        LocalDateTime startTime = LocalDateTime.parse(startInput, FORMATTER);
        LocalDateTime endTime = LocalDateTime.parse(endInput, FORMATTER);

        List<Object[]> predictions = getPredictedConsumption(startTime, endTime);

        System.out.println("Results for " + numberOfPeople + " people:");
        for (Object[] row : predictions) {
            System.out.println(row[0] + " → Predicted: " + row[1] + " kWh");
        }
    }
}
