public class Databank {
    private WeatherData weatherForecast;
    private double electricityPrice;
    private double[] pvProductionCurve;
    private double[] heatPumpConsumptionCurve;
    private double[] generalConsumptionCurve;
    private double[] batteryLoadingCurve;
    private double[] batteryDischargeCurve;

    public Databank() {
    }

    public WeatherData getWeatherForecast() {
        return weatherForecast;
    }

    public void setWeatherForecast(WeatherData weatherForecast) {
        this.weatherForecast = weatherForecast;
    }

    public double getElectricityPrice() {
        return electricityPrice;
    }

    public void setElectricityPrice(double electricityPrice) {
        this.electricityPrice = electricityPrice;
    }

    public double[] getPvProductionCurve() {
        return pvProductionCurve;
    }

    public void setPvProductionCurve(double[] pvProductionCurve) {
        this.pvProductionCurve = pvProductionCurve;
    }


    public double[] getHeatPumpConsumptionCurve() {
        return heatPumpConsumptionCurve;
    }

    public void setHeatPumpConsumptionCurve(double[] heatPumpConsumptionCurve) {
        this.heatPumpConsumptionCurve = heatPumpConsumptionCurve;
    }

    public double[] getGeneralConsumptionCurve() {
        return generalConsumptionCurve;
    }

    public void setGeneralConsumptionCurve(double[] generalConsumptionCurve) {
        this.generalConsumptionCurve = generalConsumptionCurve;
    }

    public double[] getBatteryLoadingCurve() {
        return batteryLoadingCurve;
    }

    public void setBatteryLoadingCurve(double[] batteryLoadingCurve) {
        this.batteryLoadingCurve = batteryLoadingCurve;
    }

    public double[] getBatteryDischargeCurve() {
        return batteryDischargeCurve;
    }

    public void setBatteryDischargeCurve(double[] batteryDischargeCurve) {
        this.batteryDischargeCurve = batteryDischargeCurve;
    }

    public double getValue(String type, int index) {
        return switch (type) {
            case "PV" -> pvProductionCurve[index];
            case "HeatPump" -> heatPumpConsumptionCurve[index];
            case "General" -> generalConsumptionCurve[index];
            case "BatteryLoad" -> batteryLoadingCurve[index];
            case "BatteryDischarge" -> batteryDischargeCurve[index];
            default -> 0;
        };
    }
}
