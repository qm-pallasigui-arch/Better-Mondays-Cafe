package inventory.analytics;

import inventory.InventoryItem;
import java.util.HashMap;
import java.util.Map;

public class InventoryPolicyService {

    // Simple defaults suitable for this project until historical demand/cost tables are added.
    private static final double DEFAULT_ORDERING_COST = 120.0;
    private static final double DEFAULT_HOLDING_COST_PER_UNIT = 8.0;

    public double computeRecommendedEoq(InventoryItem item) {
        if (item == null) {
            return 0.0;
        }
        // Approximate annual demand from current stock profile.
        double annualDemand = Math.max(1.0, item.getQuantity() * 12.0);
        return EoqCalculator.calculate(annualDemand, DEFAULT_ORDERING_COST, DEFAULT_HOLDING_COST_PER_UNIT);
    }

    public Map<String, String> classifyAbc(Map<String, InventoryItem> items) {
        Map<String, Double> usageValues = new HashMap<>();
        for (Map.Entry<String, InventoryItem> e : items.entrySet()) {
            InventoryItem item = e.getValue();
            // Without per-item cost yet, use quantity as usage proxy.
            usageValues.put(e.getKey(), Math.max(0.0, item.getQuantity()));
        }
        return AbcClassifier.classify(usageValues);
    }
}
