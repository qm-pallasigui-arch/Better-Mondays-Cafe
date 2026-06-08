package inventory.analytics;

import inventory.InventoryItem;
import java.util.HashMap;
import java.util.Map;

public class InventoryPolicyService {

    // Simple defaults suitable for this project until historical demand/cost/lead-time tables are added.
    private static final double DEFAULT_ORDERING_COST = 120.0;
    private static final double DEFAULT_HOLDING_COST_PER_UNIT = 8.0;
    private static final double DEFAULT_LEAD_TIME_DAYS = 3.0;

    public double computeRecommendedEoq(InventoryItem item) {
        if (item == null) {
            return 0.0;
        }
        // Approximate annual demand from current stock profile.
        double annualDemand = Math.max(1.0, item.getQuantity() * 12.0);
        return EoqCalculator.calculate(annualDemand, DEFAULT_ORDERING_COST, DEFAULT_HOLDING_COST_PER_UNIT);
    }

    public double computeReorderPoint(InventoryItem item) {
        if (item == null) {
            return 0.0;
        }
        // Approximate average daily demand from current stock profile, mirroring
        // the annual-demand proxy used for EOQ (quantity * 12 turns / year / 365 days).
        double averageDailyDemand = Math.max(0.0, (item.getQuantity() * 12.0) / 365.0);
        // Use the item's own alert level as the safety stock buffer — it already
        // represents the quantity at which the item is considered "running low".
        double safetyStock = Math.max(0.0, item.getAlertLevel());
        return RopCalculator.calculate(averageDailyDemand, DEFAULT_LEAD_TIME_DAYS, safetyStock);
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
