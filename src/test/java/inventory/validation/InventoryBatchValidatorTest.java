package inventory.validation;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryBatchValidatorTest {

    @Test
    void validatesQuantityAndExpiry() {
        assertEquals(2.5, InventoryBatchValidator.parseQuantity("2.5"), 0.0001);
        String tomorrow = LocalDate.now().plusDays(1).toString();
        assertEquals(tomorrow, InventoryBatchValidator.normalizeExpiry(tomorrow));
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> InventoryBatchValidator.parseQuantity("0"));
        String yesterday = LocalDate.now().minusDays(1).toString();
        assertThrows(IllegalArgumentException.class, () -> InventoryBatchValidator.normalizeExpiry(yesterday));
    }
}
