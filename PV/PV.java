public class PV
{
    private final double module_kWp;
    private final int module_count;

    // Parameters for simplified production model
    private final double base_kWh_per_kWp;
    private final double inverter_efficiency;
    private final double temperature_coefficient;

    public PV(double module_kWp,
              double roof_length,
              double roof_width,
              double pv_module_length,
              double pv_module_width,
              double base_kWh_per_kWp,
              double inverter_efficiency,
              double temperature_coefficient)
    {
        this.module_kWp = module_kWp;
        this.base_kWh_per_kWp = base_kWh_per_kWp;
        this.inverter_efficiency = inverter_efficiency;
        this.temperature_coefficient = temperature_coefficient;
        module_count = pvCount(roof_length, roof_width, pv_module_length, pv_module_width);
    }

    private int pvCount(double roof_length, double roof_width, double pv_module_length, double pv_module_width)
    {
        int count_length = (int) (roof_length / pv_module_length);
        int count_width = (int) (roof_width / pv_module_width);
        return count_length * count_width;
    }

    private double getAge_factor()
    {
        //TODO: Different Cleaning Intervals?

        double age = 0;  //TODO: Convert Sim time into the age of the panels in years
        double aging_factor = 0.008;
        return Math.pow(1 - aging_factor, age);
    }


    /**
     * Simplified energy production calculation used in unit tests.
     *
     * @return estimated energy production for one time step
     */
    public double calculateProduction()
    {
        double total_kWp = module_count * module_kWp;

        // Example temperature (30°C) and standard (25°C)
        double kWh_per_kWp = base_kWh_per_kWp * (1 + (30 - 25) * temperature_coefficient);

        return total_kWp * kWh_per_kWp * inverter_efficiency;
    }

    // Kept for backwards compatibility but no longer used in tests
    public double calculateCurrentProduction()
    {
        double base_kWh = 0; // TODO: Get current value from Database

        double total_kWp = module_count * module_kWp;

        double data_kWp = 1;

        double kWp_Factor = total_kWp / data_kWp;

        double result = base_kWh * kWp_Factor * getAge_factor();

        return result;
    }
    public double forecastFutureProduction(int start, int end )
    {
        //TODO: Get value from Database. Database looks up last x of the given time intervals and gives the (weighted) mean of this past simulation data?
        return 0;
    }
}
