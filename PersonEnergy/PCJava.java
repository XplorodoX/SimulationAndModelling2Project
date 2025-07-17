import java.io.Serializable;
import java.time.*;

public class PCJava implements Serializable {

    private static final long serialVersionUID = 1L;

    private PC owner;
    private int numPeople;
    private static final String TABLE_NAME = "household_data";
    private static final String TIME_COLUMN = "utc_timestamp";
    private static final String DATA_COLUMN = "average_per_person_consumption";

    public PCJava(PC owner, int numPeople) {
        this.owner = owner;
        this.numPeople = numPeople;
        DBRequest.initializeConnectionPool(); // Ensure DB connection is available

        LocalDateTime simStart = LocalDateTime.of(2016, 1, 1, 0, 0);
        LocalDateTime simEnd = LocalDateTime.of(2016, 12, 31, 23, 59);
        DBRequest.preloadTimeSeriesData(TABLE_NAME, TIME_COLUMN, DATA_COLUMN, simStart, simEnd);
    }

    public void messageIn(MessageType msg) {
        int multiplier = 5;

        if (msg instanceof DataResponceTriggerMessageForForecast forecastMsg) {
            double start = forecastMsg.timestampStart;
            double end = forecastMsg.timestampEnd;

            double[] forecast = getForecastUsingHistoricalData(start, end, multiplier * numPeople);
            owner.port.send(new DataMessageFromPCForecast(owner, start, end, forecast));
        }

        else if (msg instanceof DataResponseTriggerMessage) {
            LocalDateTime now = DateTimeConversionJava.doubleToCurrentLocalDateTime(owner.time());
            LocalDateTime mapped = DateTimeConversionJava.getDataFrom2016(now);

            double avgPerPerson = DBRequest.getValueAtTime(TABLE_NAME, TIME_COLUMN, DATA_COLUMN, mapped);
            double value = avgPerPerson * numPeople * multiplier;

            owner.port.send(new DataMessageFromPC(owner, value));
        }
    }

    public double[] getForecastUsingHistoricalData(double start, double end, int numberOfPeople) {
        int steps = (int) ((end - start) / 900) + 1;  // 15-minute steps
        double[] result = new double[steps];

        for (int i = 0; i < steps; i++) {
            double simTime = start + i * 900.0;
            LocalDateTime actualTime = DateTimeConversionJava.doubleToCurrentLocalDateTime(simTime);
            LocalDateTime mappedTime = DateTimeConversionJava.getDataFrom2016(actualTime);

            double avgPerPerson = DBRequest.getValueAtTime(TABLE_NAME, TIME_COLUMN, DATA_COLUMN, mappedTime);
            result[i] = avgPerPerson * numberOfPeople;
        }

        return result;
    }
}
