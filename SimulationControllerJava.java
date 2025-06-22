import java.util.ArrayList;
import java.util.List;

public class SimulationControllerJava {
    //needs owner in AnyLogic
    private double budget;
    private double livingArea;
    private int personCount;

    private final int numTradingStrategies = 1;

    private int maxPVCount; //Unnecessary if only roof area is used
    //PV model information
    private double pvPrice;
    private double modulePVkWp;
    private double roofAngle; //For extensions
    private double roofLength;
    private double roofWidth;
    private double pvModuleLength;
    private double pvModuleWidth;

    private int maxBatteryCount;
    //Battery model information
    private double batteryPrice;
    private double batteryCapacity;
    private double batteryChargeRate;
    private double batteryDischargeRate;
    private double batteryEfficiency;
    private double batteryDegradation;

    private boolean hasHeatPump;
    //HeatPump model information;
    private double heatPumpCop; // Do you have to provide it, or is it calculated?
    private double heatPumpRatedPower;
    private double heatPumpMinPower;
    private double heatPumpMaxPower;
    private double heatPumpMinTempHeating;
    private double heatPumpMaxTempHeating;
    private double heatPumpMinTempCooling;
    private double heatPumpMaxTempCooling;
    private boolean heatPumpIsActive;
    private double heatPumpPowerConsumption;
    private double heatPumpCurrentTemperature; // Not necessary for initialization
    private double heatPumpTargetTemperature; // Probably not necessary for initialization?
    private double heatPumpHeatingFactor;
    private double heatPumpCoolingFactor;

    private boolean hasEV;
    //More information needed

    private int trueMaxPVCount;
    private int trueMaxBatteryCount;

    //Default constructor
    public SimulationControllerJava(){
        budget = 10000;
        livingArea = 47.5;
        personCount = 1;

        maxPVCount = 10;
        pvPrice = 3000; // ask Chris
        modulePVkWp = 0.44;
        roofAngle = 30;
        roofLength = 10;
        roofWidth = 10;
        pvModuleLength = 1.134;
        pvModuleWidth = 1.762;

        maxBatteryCount = 10;
        batteryPrice = 3000;
        batteryCapacity = 5;
        batteryChargeRate = 0.5;
        batteryDischargeRate = 0.5;
        batteryEfficiency = 0.98;
        batteryDegradation = 0.01;

        hasHeatPump = true;
        // ask Flo for stats
        // set HeatPump stats here
        if(hasHeatPump){
            heatPumpPowerConsumption = 0; // set power consumption 0 or send different message to houseController
        }

        // set EV stats here

        trueMaxPVCount = Math.min(maxPVCount, pvCount(roofLength, roofWidth, pvModuleLength, pvModuleWidth));
        trueMaxPVCount = Math.min(trueMaxPVCount, (int) Math.floor(budget / pvPrice)); //Because it can only take 2 arguments

        trueMaxBatteryCount = Math.min(maxBatteryCount, (int) Math.floor(budget / pvPrice));



    }
    public void initialize(int strategy, int pvCount, int batteryCount){
        // send message to PV
        // send message to Battery
        // send message to PC
        // send message to HeatPump
        // send message to EV
        // send message to HouseController
        // reset date?
        return;
    }

    public List<int[]> calculatePossibleCombinations(){
        List<int[]> combinations = new ArrayList<>();
        for (int s = 0; s < numTradingStrategies; s++) {
            for (int p = 0; p < trueMaxPVCount; p++) {
                for (int b = 0; b < trueMaxBatteryCount; b++) {
                    if (p * pvPrice + b * batteryPrice <= budget){
                        combinations.add(new int[]{s, p, b});
                    }
                }
            }
        }
        return combinations;
    }

    // Taken from Chris' code
    private int pvCount(double roof_length, double roof_width, double pv_module_length, double pv_module_width)
    {
        double usable_roof_modifier = 0.75;

        int count_length = (int) (roof_length / pv_module_length);
        int count_width = (int) (roof_width / pv_module_width);
        return (int) (count_length * count_width * usable_roof_modifier);
    }
}
