public class PV
{
    private final double module_kWp;
    private final int module_count;

    public PV(double module_kWp,
              double roof_length,
              double roof_width,
              double pv_module_length,
              double pv_module_width)
    {
        this.module_kWp = module_kWp;

        module_count = pvCount(roof_length, roof_width, pv_module_length, pv_module_width);
    }

    private int pvCount(double roof_length, double roof_width, double pv_module_length, double pv_module_width)
    {
        double usable_roof_modifier = 0.75;

        int count_length = (int) (roof_length / pv_module_length);
        int count_width = (int) (roof_width / pv_module_width);
        return (int) (count_length * count_width * usable_roof_modifier);
    }

    private double getAgeFactor()
    {
        double age = 0;  //TODO: Convert Sim time into the age of the panels in years
        double aging_factor = 0.008;
        return Math.pow(1 - aging_factor, age);
    }

    public double calculateCurrentProduction()
    {
        double base_kWh = 0; // TODO: Get current value from Database

        double total_kWp = module_count * module_kWp;

        double data_kWp = 1;

        double kWp_Factor = total_kWp / data_kWp;

        return base_kWh * kWp_Factor * getAgeFactor();
    }
    public double forecastFutureProduction(int start, int end )
    {
        //TODO: Get production for last 3 timeslots from Database

        double p1 = 0;
        double p2 = 0;
        double p3 = 0;

        return (p1 + p2 + p3) /  3;
    }
}
