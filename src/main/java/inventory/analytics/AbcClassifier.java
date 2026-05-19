package inventory.analytics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AbcClassifier {

    private static final double EPSILON = 1e-9;

    private AbcClassifier() {
    }

    // Classifies based on cumulative annual usage value share.
    // A: top 80%, B: next 15%, C: remaining 5%.
    public static Map<String, String> classify(Map<String, Double> annualUsageValues) {
        Map<String, String> result = new HashMap<>();
        if (annualUsageValues == null || annualUsageValues.isEmpty()) {
            return result;
        }

        double total = 0.0;
        for (double value : annualUsageValues.values()) {
            if (value > 0) {
                total += value;
            }
        }
        if (total <= 0.0) {
            for (String key : annualUsageValues.keySet()) {
                result.put(key, "C");
            }
            return result;
        }

        List<Map.Entry<String, Double>> entries = new ArrayList<>(annualUsageValues.entrySet());
        entries.sort(Comparator.comparingDouble((Map.Entry<String, Double> e) -> e.getValue()).reversed());

        double cumulative = 0.0;
        for (Map.Entry<String, Double> entry : entries) {
            double v = Math.max(0.0, entry.getValue());
            cumulative += v / total;
            if (cumulative <= 0.80 + EPSILON) {
                result.put(entry.getKey(), "A");
            } else if (cumulative <= 0.95 + EPSILON) {
                result.put(entry.getKey(), "B");
            } else {
                result.put(entry.getKey(), "C");
            }
        }

        return result;
    }
}
