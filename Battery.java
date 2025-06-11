public class Battery {

    // --- Batterieparameter ---
    private double capacity;            // Capacity [kWh]
    private double stateOfCharge;       // Current charge [kWh]
    private double maxChargePower;      // Maximum charge in one timestep [kWh]
    private double maxDischargePower;   // Maximum discharge in one timestep [kWh]
    private double roundTripEfficiency; // Charge-Discharge efficiency (applied twice)
    private double degradationPerCycle; // Capacity loss per cycle [%/1.0]

    private double cycles;              // Charging cycles

    public Battery(double capacity, double maxChargePower, double maxDischargePower, double roundTripEfficiency, double degradationPerCycle) {
        this.capacity = capacity;
        this.stateOfCharge = 0.0;
        this.maxChargePower = maxChargePower;
        this.maxDischargePower = maxDischargePower;
        this.roundTripEfficiency = roundTripEfficiency;
        this.degradationPerCycle = degradationPerCycle;
        this.cycles = 0;
    }

    // Charges battery and returns the actual charge
    public double charge(double requestedEnergy) {
        double maxPossibleCharge = Math.min(maxChargePower, (capacity - stateOfCharge));
        double reducedEnergy = requestedEnergy * roundTripEfficiency;
        double actualCharge = Math.min(reducedEnergy, maxPossibleCharge);
        double loss = reducedEnergy - actualCharge; // For usage in Anylogic

        stateOfCharge += actualCharge;

        cycles += actualCharge / capacity;

        return actualCharge;
    }

    // Discharges battery and returns actual obtained energy
    // trueDischarge tries to consider roundTripEfficiency beforehand
    public double discharge(double requestedEnergy, boolean trueDischarge) {
        double newRequestedEnergy = requestedEnergy;
        if(trueDischarge) {
            newRequestedEnergy /= roundTripEfficiency;
        }
        double maxPossibleDischarge = Math.min(maxDischargePower, stateOfCharge);
        double actualDischarge = Math.min(newRequestedEnergy, maxPossibleDischarge);

        double missing = newRequestedEnergy - actualDischarge;

        stateOfCharge -= actualDischarge;

        cycles += actualDischarge / capacity;

        if(trueDischarge){
            if(missing==0){
                return requestedEnergy; // should be numerically more stable
            }
        }
        return actualDischarge * roundTripEfficiency;
    }

    // Simulate Degradation
    public void applyDegradation() {
        double degradedCapacity = capacity * (1 - degradationPerCycle * cycles);
        capacity = Math.max(degradedCapacity, 0);
        stateOfCharge = Math.min(stateOfCharge, capacity);
    }

    // Getter
    public double getSOC() {
        return stateOfCharge;
    }

    public double getCapacity() {
        return capacity;
    }

    public double getCycles() {
        return cycles;
    }

    public double getSOCPercentage() {
        return (stateOfCharge / capacity) * 100.0;
    }
}
