package inventory.analytics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EoqCalculatorTest {

    @Test
    void calculatesExpectedEoq() {
        double eoq = EoqCalculator.calculate(1200, 100, 5);
        assertEquals(219.089, eoq, 0.01);
    }

    @Test
    void returnsZeroForInvalidInputs() {
        assertEquals(0.0, EoqCalculator.calculate(0, 100, 5));
        assertEquals(0.0, EoqCalculator.calculate(100, -1, 5));
        assertEquals(0.0, EoqCalculator.calculate(100, 10, 0));
    }
}
