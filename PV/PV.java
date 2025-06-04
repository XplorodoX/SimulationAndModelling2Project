public class PV
{
    double module_kWp;
    double module_count;
    double base_kWh_per_kWp;
    double inverter_Conversion_Efficiency; //in - %/°C
    double temperature_coefficient;

    public PV(double module_kWp, double roof_length, double roof_width, double pv_module_length, double pv_module_width, double base_kWh_per_kWp, double inverter_Conversion_Efficiency, double temperature_coefficient)
    {
        this.module_kWp = module_kWp;
        this.base_kWh_per_kWp = base_kWh_per_kWp;
        this.inverter_Conversion_Efficiency = inverter_Conversion_Efficiency;
        this.temperature_coefficient = temperature_coefficient;

        //TODO: More advanced module count calculation
        module_count = pvCount(roof_length, roof_width, pv_module_length, pv_module_width);
    }

    private int pvCount(double roof_length, double roof_width, double pv_module_length, double pv_module_width)
    {
        double roof_area = roof_length * roof_width;
        double pv_area = pv_module_length * pv_module_width;

        return (int) (roof_area / pv_area);
    }

    public double calculateProduction()
    {
        double kWh_per_kWp = updateConversion();
        return module_count * module_kWp * kWh_per_kWp * inverter_Conversion_Efficiency;
    }
    private double updateConversion()
    {
        //TODO: Check results especially if the modifiers are correct
        // or if they influence eachother and thus lower the result too much. e.g. weather / temperature already being factored into Month

        double kWh_per_kWp = base_kWh_per_kWp;
        //TODO: Weather Influence

        //TODO: Month Influence: https://regional-photovoltaik.de/planung-installation/pv-ertrag-tabelle-aktuelle-daten/

        //TODO: Hour Influence

        //Temperature Influence:

        int temperature = 30; //TODO: Get Temperature from Database
        double modifier = (temperature - 25) * temperature_coefficient;
        kWh_per_kWp = kWh_per_kWp * (1 + modifier);

        //TODO: Angel Influence? perhaps not needed due to the assumption of a perfect angle during installation
        //TODO: Age Factor?

        return kWh_per_kWp;
    }
}