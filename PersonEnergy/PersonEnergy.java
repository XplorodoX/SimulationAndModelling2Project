import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class PersonEnergy {
    private String csvFile;
    private double totalConsumption;
    private double totalProduction;

    // Residential grid imports
    private final int[] residentialImports = {34, 40, 46, 52, 58, 66};

    // Residential PV production
    private final int[] residentialPV = {36, 48, 54, 68};

    private final double intervalHours = 0.25;

    public PersonEnergy(String csvFile) {
        this.csvFile = csvFile;
    }

    public void loadAndCalculate() {
        totalConsumption = 0;
        totalProduction = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String header = br.readLine(); // Skip header
            System.out.println("Reading dataset...");

            String line;
            int lineNumber = 1; // Start after header
            while ((line = br.readLine()) != null) {
                lineNumber++;
                String[] data = line.split(",", -1);

                double consumption = 0;
                for (int index : residentialImports) {
                    if (index < data.length && !data[index].isEmpty()) {
                        consumption += Double.parseDouble(data[index]) * intervalHours;
                    } else if (index >= data.length) {
                        System.err.println("[DEBUG] Line " + lineNumber + ": missing consumption column " + index + " (columns=" + data.length + ")");
                    }
                }

                double production = 0;
                for (int index : residentialPV) {
                    if (index < data.length && !data[index].isEmpty()) {
                        production += Double.parseDouble(data[index]) * intervalHours;
                    } else if (index >= data.length) {
                        System.err.println("[DEBUG] Line " + lineNumber + ": missing production column " + index + " (columns=" + data.length + ")");
                    }
                }

                totalConsumption += consumption;
                totalProduction += production;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getTotalConsumption() {
        return totalConsumption;
    }

    public double getTotalProduction() {
        return totalProduction;
    }

    public double getNetConsumption() {
        return totalConsumption - totalProduction;
    }

    
}
