/**
 * PVHandler
 */	

import java.time.*;

public class PVJava implements Serializable
{
	String table_name = "pv";
	String timestamp_columns = "Time";
	//The kWp our csv source uses 
	double data_kWp = 1;
	 
	public void messageIn(MessageType msg)
	{
		if (msg instanceof DataResponseTriggerMessage)
		{
			owner.port.send(new DataMessageFromPV(owner, owner.time(), calculateCurrentProduction()));
			//System.out.println(calculateCurrentProduction());
		}
		//TODO New type of msg for future production
		//else if (msg instanceof DataResponseTriggerMessageFutureProduction)
		{
			//TODO set start and end 
			int start = 0;
			int end = 0;
			forecastFutureProduction(start,end);
			//TODO maybe different msg type as a response 
			//owner.port.send(new DataMessageFromPV(owner, owner.time(), forecastFutureProduction(start,end);));
		}
	}
	
	
	private final double module_kWp;
    private final int module_count;
	public PV owner;

    public PVJava(PV owner, 
    		  double module_kWp,
              double roof_length,
              double roof_width,
              double pv_module_length,
              double pv_module_width)
    {
    	this.owner = owner;
        this.module_kWp = module_kWp;

        module_count = pvCount(roof_length, roof_width, pv_module_length, pv_module_width);
    }
    
    //TODO: Change location from pvCount / get solar pannel count from constructor
    private int pvCount(double roof_length, double roof_width, double pv_module_length, double pv_module_width)
    {
        double usable_roof_modifier = 0.75;

        int count_length = (int) (roof_length / pv_module_length);
        int count_width = (int) (roof_width / pv_module_width);
        return (int) (count_length * count_width * usable_roof_modifier);
    }

    private double getAgeFactor()
    {
    	//TODO Validate correct age calculation
    	double age = owner.time() / 31536000;
    	double aging_factor = 0.008;
    	System.out.println(age);
    	return Math.pow(1 - aging_factor, age); 
    }
 
    public double calculateCurrentProduction()
    {
        double base_kWh = db_communication();

        double total_kWp = module_count * module_kWp;

        double kWp_Factor = total_kWp / data_kWp;

        return base_kWh * kWp_Factor * getAgeFactor();
    }
    public double forecastFutureProduction(int start, int end )
    {
        //TODO: Implement forecast

        return Double.NaN;
    }
    
    public double db_communication() 
    {
        try (DBRequest.DBConnection dbConn = new DBRequest.DBConnection()) 
        {
            Connection conn = dbConn.getConnection(); 
            //TODO: Change to use owner.time() in correct format
            LocalDateTime time = LocalDateTime.of(2016, 12, 1, 9, 0);
            Object raw_kWh = DBRequest.getActualValue(conn, table_name, "Time", "kWh", time);
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
