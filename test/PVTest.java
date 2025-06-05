import org.junit.Test;
import static org.junit.Assert.*;

public class PVTest {
    @Test
    public void testCalculateProduction() {
        PV pv = new PV(
                1.0, // module_kWp
                10.0, // roof_length
                10.0, // roof_width
                1.0, // pv_module_length
                1.0, // pv_module_width
                1000.0, // base_kWh_per_kWp
                0.9, // inverter efficiency
                0.005 // temperature coefficient
        );
        double production = pv.calculateProduction();
        // expected calculation:
        // module_count = roof_area / pv_area = 100
        // kWh_per_kWp = base * (1 + (30-25)*0.005) = 1000 * 1.025 = 1025
        // result = 100 * 1 * 1025 * 0.9 = 92250
        assertEquals(92250.0, production, 0.0001);
    }
}
