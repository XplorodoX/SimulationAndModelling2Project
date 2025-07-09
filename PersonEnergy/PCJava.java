import java.time.*;
import java.util.*;
import java.io.Serializable;

public class PCJava implements Serializable {
    private static final long serialVersionUID = 1L;

    private PC owner;

    private static final String TABLE_NAME = "household_data";
    private static final String TIME_COLUMN = "utc_timestamp";
    private static final String DATA_COLUMN = "average_per_person_consumption";

    public PCJava(PC owner) {
        this.owner = owner;
        DBRequest.initializeConnectionPool(); // ensure pool is available
    }

    public void messageIn(MessageType msg) {
        if (msg instanceof DataResponceTriggerMessageForForecast forecastMsg) {
            double start = forecastMsg.timestampStart;
            double end = forecastMsg.timestampEnd;
            int numberOfPeople = forecastMsg.numberOfPeople;

            double[] forecast = getForecastUsingHistoricalData(start, end, numberOfPeople);
            owner.port.send(new DataMessageFromPCForecast(owner, forecast));
        }
    }

    public double[] getForecastUsingHistoricalData(double start, double end, int numberOfPeople) {
        int steps = (int) ((end - start) / 900);  // 15-min intervals
        double[] result = new double[steps];

        for (int i = 0; i < steps; i++) {
            double simTime = start + i * 900.0;  // simulation timestamp
            LocalDateTime actualTime = DateTimeConversionJava.doubleToCurrentLocalDateTime(simTime);
            LocalDateTime mappedTo2016 = convertTo2016Equivalent(actualTime);

            double avgPerPerson = DBRequest.getValueAtTime(
                TABLE_NAME, TIME_COLUMN, DATA_COLUMN, mappedTo2016
            );

            result[i] = avgPerPerson * numberOfPeople;
        }

        return result;
    }

    private LocalDateTime convertTo2016Equivalent(LocalDateTime actualTime) {
        // Handles overflow (e.g., Feb 29 in non-leap year)
        int day = Math.min(actualTime.getDayOfMonth(),
            Year.of(2016).atMonth(actualTime.getMonth()).lengthOfMonth());

        return LocalDateTime.of(2016, actualTime.getMonth(), day, actualTime.getHour(), actualTime.getMinute());
    }
}
