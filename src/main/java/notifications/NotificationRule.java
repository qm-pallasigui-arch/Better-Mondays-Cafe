package notifications;

import controller.InventoryRowView;
import inventory.analytics.InventoryPolicyService;

/**
 * A single condition staff care about (low stock, expiring batch, reorder
 * point reached, ...). Implementations inspect one inventory row and either
 * raise a {@link Notification} or stay silent. Keeping each condition as its
 * own rule lets new alert types be added without touching the evaluation loop.
 */
public interface NotificationRule {
    Notification evaluate(InventoryRowView row, InventoryPolicyService policyService);
}
