import java.io.Serializable;
import java.time.*;

/**
 * HeatPumpJava
 */
public class HeatPumpJava implements Serializable
{
    HeatPumpJava owner;
    String table_name = "heatpump";
    String timestamp_column = "timestamp";  // Korrigiert und konsistent
    String data_column = "value";            // Hinzugefügt für Konsistenz

    public HeatPumpJava(HeatPumpJava owner)
    {
        this.owner = owner;

        DBRequest.initializeConnectionPool();

        LocalDateTime simStart = LocalDateTime.of(2016, 1, 1, 0, 0);
        LocalDateTime simEnd = LocalDateTime.of(2016, 12, 31, 23, 59);

        DBRequest.preloadTimeSeriesData(table_name, timestamp_column, data_column, simStart, simEnd);
    }

    public void messageIn(MessageType msg)
    {
        if (msg instanceof DataResponseTriggerMessage)
        {
            owner.port.send(new DataMessageFromHP(owner, owner.time(), db_communication()));
        }else if (msg instanceof DataResponseTriggerMessageFuture)
        {
            //TODO set start and end
            int start = 0;
            int end = 0;
            double[] futureProduction = forecastFutureProduction(start, end);
            owner.port.send(new DataMessageFromHP(owner, owner.time(), futureProduction));
    }
        //TODO New type of msg for future production
        //else if (msg instanceof DataResponseTriggerMessageFutureProduction)
        //{
        //TODO set start and end
        // int start = 0;
        // int end = 0;
        //forecastFutureProduction(start,end);
        //TODO maybe different msg type as a response
        //owner.port.send(new DataMessageFromPV(owner, owner.time(), forecastFutureProduction(start,end);));
        //}
    }

    public double db_communication(double lookUpTime)
    {

        LocalDateTime time = DateTimeConversionJava.getTimeNow(owner);

        double raw_consumption = DBRequest.getValueAtTime(table_name, timestamp_column, data_column, time);

        return raw_consumption;
    }

    public double calculateCurrentProduction(double lookUpTime)
    {
        double base_kWh = db_communication(lookUpTime);

        return base_kWh;
    }

    public double[] forecastFutureProduction(double start, double end )
    {
        int steps = (int) ((end - start) / 900) + 1;
        double[] result = new double[steps];

        for(int x = 0; x < steps; x++)
        {
            double timestamp = start + x * 900.0;
            result[x] = calculateCurrentProduction(timestamp);
        }

        return result;
    }

    @Override
    public String toString()
    {
        return super.toString();
    }

    /**
     * This number is here for model snapshot storing purpose<br>
     * It needs to be changed when this class gets changed
     */
    private static final long serialVersionUID = 1L;
}