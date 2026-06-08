package notifications;

import controller.InventoryRowView;
import inventory.analytics.InventoryPolicyService;

public final class LowStockRule implements NotificationRule {
    @Override
    public Notification evaluate(InventoryRowView row, InventoryPolicyService policyService) {
        double quantity = row.getQuantity();
        if (quantity <= 0) {
            return new Notification(Notification.Severity.CRITICAL, row.getName(),
                    row.getName() + " is out of stock.");
        }
        if (quantity <= row.getAlertLevel()) {
            return new Notification(Notification.Severity.WARNING, row.getName(),
                    row.getName() + " is running low (" + quantity + " " + row.getUnit()
                            + " left, alert at " + row.getAlertLevel() + " " + row.getUnit() + ").");
        }
        return null;
    }
}
