public class HeatPump {
    private double cop;
    private double ratedPower;
    private double minPower;
    private double maxPower;
    private double minTempHeating;
    private double maxTempHeating;
    private double minTempCooling;
    private double maxTempCooling;
    private boolean isActive;
    private double powerConsumption;
    private double currentTemperature;
    private double targetTemperature;
    private double heatingFactor;
    private double coolingFactor;

    // Default constructor
    public void Heatpump() {
        this.cop = 0.0;
        this.ratedPower = 0.0;
        this.minPower = 0.0;
        this.maxPower = 0.0;
        this.minTempHeating = 0.0;
        this.maxTempHeating = 0.0;
        this.minTempCooling = 0.0;
        this.maxTempCooling = 0.0;
        this.isActive = false;
        this.powerConsumption = 0.0;
        this.currentTemperature = 20.0; // Default or initial temperature
        this.targetTemperature = 20.0;   // Default target temperature
        this.heatingFactor = 0.1;      // Default heating factor
        this.coolingFactor = 0.1;      // Default cooling factor
    }

    // Constructor with parameters
    public void Heatpump(double cop, double ratedPower, double minPower, double maxPower,
                         double minTempHeating, double maxTempHeating,
                         double minTempCooling, double maxTempCooling) {
        this.cop = cop;
        this.ratedPower = ratedPower;
        this.minPower = minPower;
        this.maxPower = maxPower;
        this.minTempHeating = minTempHeating;
        this.maxTempHeating = maxTempHeating;
        this.minTempCooling = minTempCooling;
        this.maxTempCooling = maxTempCooling;
        this.isActive = false;
        this.powerConsumption = 0.0;
        this.currentTemperature = 20.0; // Default or initial temperature
        this.targetTemperature = 20.0;   // Default target temperature
        this.heatingFactor = 0.1;      // Default heating factor
        this.coolingFactor = 0.1;      // Default cooling factor
    }

    // Getters
    public double getPowerConsumption() {
        return powerConsumption;
    }

    public double getRatedPower() {
        return ratedPower;
    }

    public double getMinPower() {
        return minPower;
    }

    public double getMaxPower() {
        return maxPower;
    }

    public double getMinTempHeating() {
        return minTempHeating;
    }

    public double getMaxTempHeating() {
        return maxTempHeating;
    }

    public double getMinTempCooling() {
        return minTempCooling;
    }

    public double getMaxTempCooling() {
        return maxTempCooling;
    }

    public boolean getIsActive() {
        return isActive;
    }

    // Setters
    public void setTargetTemperature(double targetTemperature) {
        this.targetTemperature = targetTemperature;
    }

    public void setHeatingFactor(double heatingFactor) {
        this.heatingFactor = heatingFactor;
    }

    public void setCoolingFactor(double coolingFactor) {
        this.coolingFactor = coolingFactor;
    }

    // Methods
    public double getCOP(double ambientTemperature, double targetTemperature) {
        // A simplified COP calculation for demonstration.
        // In a real heat pump, COP depends on many factors (temperatures, compressor efficiency, etc.)
        // This assumes a linear relationship or a lookup table would be used in a more complex model.
        if (targetTemperature > ambientTemperature) { // Heating
            if (ambientTemperature < minTempHeating || targetTemperature > maxTempHeating) {
                return 0.0; // Outside operating range
            }
            return this.cop + (targetTemperature - ambientTemperature) * 0.05; // Example adjustment
        } else if (targetTemperature < ambientTemperature) { // Cooling
            if (ambientTemperature > maxTempCooling || targetTemperature < minTempCooling) {
                return 0.0; // Outside operating range
            }
            return this.cop + (ambientTemperature - targetTemperature) * 0.05; // Example adjustment
        } else {
            return this.cop; // No temperature difference, base COP
        }
    }

    public double calculatePowerConsumption(double currentTemperature, double targetTemperature) {
        this.currentTemperature = currentTemperature; // Update current temperature for internal state

        if (!isActive) {
            this.powerConsumption = 0.0;
            return 0.0;
        }

        double tempDifference = targetTemperature - currentTemperature;
        double requiredPower = 0.0;

        if (tempDifference > 0) { // Heating needed
            if (currentTemperature < minTempHeating || targetTemperature > maxTempHeating) {
                System.out.println("Heatpump: Target temperature for heating is outside operating range.");
                this.powerConsumption = 0.0;
                return 0.0;
            }
            requiredPower = Math.abs(tempDifference) * heatingFactor;
        } else if (tempDifference < 0) { // Cooling needed
            if (currentTemperature > maxTempCooling || targetTemperature < minTempCooling) {
                System.out.println("Heatpump: Target temperature for cooling is outside operating range.");
                this.powerConsumption = 0.0;
                return 0.0;
            }
            requiredPower = Math.abs(tempDifference) * coolingFactor;
        } else {
            // Target temperature reached, no power consumption
            this.powerConsumption = 0.0;
            return 0.0;
        }

        // Apply rated power and min/max power constraints
        if (requiredPower > ratedPower) {
            requiredPower = ratedPower;
        }
        if (requiredPower < minPower && requiredPower > 0) { // If some power is needed but less than min, use min
            requiredPower = minPower;
        }
        if (requiredPower > maxPower) {
            requiredPower = maxPower;
        }

        this.powerConsumption = requiredPower;
        return requiredPower;
    }

    public void activate() {
        this.isActive = true;
        System.out.println("Heatpump activated.");
    }

    public void deactivate() {
        this.isActive = false;
        this.powerConsumption = 0.0; // Reset power consumption when deactivated
        System.out.println("Heatpump deactivated.");
    }
}