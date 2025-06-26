import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class PCJava implements Serializable {

	public void messageIn(MessageType msg) {
	    if (msg instanceof DataResponseTriggerMessage) {
	        int numberOfPeople = 3;

	        Object prediction = getPredictedConsumptionNow();

	        if (prediction instanceof Number) {
	            double consumption = ((Number) prediction).doubleValue();
	            owner.port.send(new DataMessageFromPC(owner, owner.time(), consumption));
	        } else {
	            System.err.println("Vorhersage ist null oder kein Number-Objekt: " + prediction);
	            // Optional: Sende eine Meldung mit Defaultwert oder ignoriere
	            // owner.port.send(new DataMessageFromPC(owner, owner.time(), 0.0));
	        }
	    }
	}
	
	public PC owner;
    //private static final String DB_URL = "jdbc:postgresql://localhost:5432/simdata";
    //private static final String DB_USER = "user";      
    //private static final String DB_PASSWORD = "password";  
    // not needed, because theyre set in the Database

    private static final String TABLE_NAME = "household_data";
    private static final String TIME_COLUMN = "utc_timestamp";
    private static final String DATA_COLUMN = "average_per_person_consumption";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
    public PCJava(PC owner) {
    	this.owner = owner;
    }    
    
  public Object getPredictedConsumptionNow() 
    {
        try (DBRequest.DBConnection dbConn = new DBRequest.DBConnection()) {
            Connection conn = dbConn.getConnection(); 
            LocalDateTime timeStart = LocalDateTime.of(2016, 1, 1, 0, 45);
            LocalDateTime timeEnd = LocalDateTime.of(2016, 1, 1, 1, 45);
            
            LocalDateTime time = LocalDateTime.of(2023, 01, 28, 9, 0);
            LocalDateTime time2 = DateTimeConversionJava.getTimeNow(owner);
            LocalDateTime time3 = DateTimeConversionJava.doubleToCurrentLocalDateTime(owner.time());            
            System.out.println("PC: time2: " + time2);
            System.out.println("PC: time3: " + time3);
            
            List<Object[]> data = DBRequest.getTimeSeriesData(conn, TABLE_NAME, TIME_COLUMN, DATA_COLUMN, timeStart, timeEnd);

            System.out.println("📊 Gefundene Daten:");
            for (Object[] row : data) {
                System.out.print("→ [ ");
                for (Object value : row) {
                    System.out.print(value + " ");
                }
                System.out.println("]");
            }
        }
        catch (ClassNotFoundException e)
        {
        	System.err.println("🚨 PostgreSQL JDBC driver not found.");
        	e.printStackTrace();
        }
        catch (SQLException e) 
        {
            System.err.println("Database error: " + e.getMessage());
        }
        return null;
    }
    
    // Perform the process and return the result for the controller
    public List<Object[]> getPredictedConsumption(LocalDateTime startTime, LocalDateTime endTime, int numberOfPeople) {
        List<Object[]> results = new ArrayList<>();

        try (DBRequest.DBConnection dbConn = new DBRequest.DBConnection()) {
            Connection conn = dbConn.getConnection(); 

            List<Object[]> data = DBRequest.getTimeSeriesData(conn, TABLE_NAME, TIME_COLUMN, DATA_COLUMN, startTime, endTime);

            for (Object[] row : data) {
                Timestamp ts = (Timestamp) row[0];
                Object valueObj = row[1];
                if (valueObj instanceof Number) {
                    double avgPerPerson = ((Number) valueObj).doubleValue();
                    double predicted = avgPerPerson * numberOfPeople * 2.5;

                    // Test print (remove later)
                    System.out.println(ts + " → Average/person: " + avgPerPerson + " kWh → Predicted for " + numberOfPeople + " people: " + predicted + " kWh");

                    results.add(new Object[]{ts, predicted});
                }
            }            
        }         
        catch (ClassNotFoundException e)
        {
        	System.err.println("🚨 PostgreSQL JDBC driver not found.");
        	e.printStackTrace();
        }
        catch (SQLException e) 
        {
            System.err.println("Database error: " + e.getMessage());
        }

        return results;  // ← to be forwarded to the house controller
    }
    // Optional test main to check before integrating with controller
    public void main(String[] args) {
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
	@Override
	public String toString() {
		return super.toString();
	}

	/**
	 * This number is here for model snapshot storing purpose<br>
	 * It needs to be changed when this class gets changed
	 */ 
	private static final long serialVersionUID = 1L;

}
