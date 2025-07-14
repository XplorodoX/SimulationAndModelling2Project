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

public double[] getForecastFromDatabase(double start, double end, int numberOfPeople) {
        List<Object[]> rows = DBRequest.getTimeSeriesData(
            TABLE_NAME,
            TIME_COLUMN,
            DATA_COLUMN,
            DateTimeConversionJava.doubleToCurrentLocalDateTime(start),
            DateTimeConversionJava.doubleToCurrentLocalDateTime(end)
        );

        double[] result = new double[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            if (row[1] instanceof Number) {
                double avg = ((Number) row[1]).doubleValue();
                result[i] = avg * numberOfPeople;
            }
        }
        return result;
    }
}
