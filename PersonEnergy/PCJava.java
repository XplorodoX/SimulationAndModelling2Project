import java.io.Serializable;
import java.time.*;

public class PCJava implements Serializable {

    private static final long serialVersionUID = 1L;

    private PC owner;
    private static final String TABLE_NAME = "household_data";
    private static final String TIME_COLUMN = "utc_timestamp";
    private static final String DATA_COLUMN = "average_per_person_consumption";

    public PCJava(PC owner) {
        this.owner = owner;
        DBRequest.initializeConnectionPool(); // Ensure connection is available
    }

    public void messageIn(MessageType msg) {
        if (msg instanceof DataResponceTriggerMessageForForecast forecastMsg) {
            double start = forecastMsg.timestampStart;
            double end = forecastMsg.timestampEnd;
            int numberOfPeople = owner.numberOfPeople; // Read from field

            double[] forecast = getForecastUsingHistoricalData(start, end, numberOfPeople);
            owner.port.send(new DataMessageFromPCForecast(owner, forecast));
        }

        // Optional: If needed, handle current consumption:
        else if (msg instanceof DataResponseTriggerMessage) {
            LocalDateTime now = DateTimeConversionJava.getTimeNow(owner);
            LocalDateTime mapped = convertTo2016Equivalent(now);
            double avgPerPerson = DBRequest.getValueAtTime(TABLE_NAME, TIME_COLUMN, DATA_COLUMN, mapped);
            double value = avgPerPerson * owner.numberOfPeople;

            owner.port.send(new DataMessageFromPC(owner, value));
        }
    }

    // Forecast future consumption using mapped historical data (e.g. Jan 5 2048 → Jan 5 2016)
    public double[] getForecastUsingHistoricalData(double start, double end, int numberOfPeople) {
        int steps = (int) ((end - start) / 900);  // 900 seconds = 15 min
        double[] result = new double[steps];

        for (int i = 0; i < steps; i++) {
            double simTime = start + i * 900.0;
            LocalDateTime actualTime = DateTimeConversionJava.doubleToCurrentLocalDateTime(simTime);
            LocalDateTime mappedTime = convertTo2016Equivalent(actualTime);

            double avgPerPerson = DBRequest.getValueAtTime(TABLE_NAME, TIME_COLUMN, DATA_COLUMN, mappedTime);
            result[i] = avgPerPerson * numberOfPeople;
        }

        return result;
    }

    // If year is not 2016, map date to the same MM-DD hh:mm in 2016.
    private LocalDateTime convertTo2016Equivalent(LocalDateTime time) {
        int day = Math.min(
            time.getDayOfMonth(),
            Year.of(2016).atMonth(time.getMonth()).lengthOfMonth()
        );
        return LocalDateTime.of(2016, time.getMonth(), day, time.getHour(), time.getMinute());
    }
}
