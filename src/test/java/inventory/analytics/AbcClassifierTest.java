package inventory.analytics;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbcClassifierTest {

    @Test
    void classifiesByCumulativeUsage() {
        Map<String, Double> usage = new LinkedHashMap<>();
        usage.put("A1", 80.0);
        usage.put("B1", 15.0);
        usage.put("C1", 5.0);

        Map<String, String> result = AbcClassifier.classify(usage);

        assertEquals("A", result.get("A1"));
        assertEquals("B", result.get("B1"));
        assertEquals("C", result.get("C1"));
    }

    @Test
    void defaultsToCWhenNoPositiveTotal() {
        Map<String, Double> usage = new LinkedHashMap<>();
        usage.put("X", 0.0);
        usage.put("Y", -2.0);

        Map<String, String> result = AbcClassifier.classify(usage);
        assertEquals("C", result.get("X"));
        assertEquals("C", result.get("Y"));
    }
}
