package notifications;

import controller.InventoryRowView;
import inventory.InventoryItem;
import inventory.analytics.InventoryPolicyService;

/**
 * Surfaces the ROP/EOQ analytics as an actionable alert: once stock has
 * reached its reorder point, staff should place a new order — and the
 * suggested order size (EOQ) is included so they know roughly how much.
 */
public final class RestockRule implements NotificationRule {
    @Override
    public Notification evaluate(InventoryRowView row, InventoryPolicyService policyService) {
        InventoryItem item = new InventoryItem(row.getName(), row.getQuantity(), row.getUnit(), row.getAlertLevel());
        double rop = policyService.computeReorderPoint(item);
        if (row.getQuantity() > rop) {
            return null;
        }
        double eoq = policyService.computeRecommendedEoq(item);
        return new Notification(Notification.Severity.WARNING, row.getName(),
                row.getName() + " has reached its reorder point (~" + Math.round(rop) + " " + row.getUnit()
                        + ") — place a new order of about " + Math.round(eoq) + " " + row.getUnit() + ".");
    }
}
