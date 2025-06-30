import java.io.Serializable;
import java.sql.*;
import java.time.*;

public class HeatPump implements Serializable {
    String table_name = "heatpump";
    String timestamp_columns = "Time";

    public HeatPump() {
        // Constructor logic if needed
    }

    public double db_communication()
    {
        try (DBRequest.DBConnection dbConn = new DBRequest.DBConnection())
        {
            Connection conn = dbConn.getConnection();

            LocalDateTime startHH = LocalDateTime.of(2016, 1, 1, 1, 0);
            LocalDateTime endHH = LocalDateTime.of(2016, 1, 1, 2, 0);

            Object raw_kWh = DBRequest.getActualValue(conn, table_name, "utc_timestamp", "DE_KN_residential1_heat_pump", startHH);

            return ((Number) raw_kWh).doubleValue();
        }
        catch (ClassNotFoundException e)
        {
            System.err.println("🚨 PostgreSQL JDBC driver not found.");
            e.printStackTrace();
        }
        catch (SQLException e)
        {
            System.err.println("🚨 Database connection failed!");
            e.printStackTrace();
        }
        return Double.NaN;
    }

    public static void main(String[] args) {
        HeatPump heatPump = new HeatPump();
        double heatPumpValue = heatPump.db_communication();
        System.out.println("Heat Pump Value: " + heatPumpValue);
    }

}