package notifications;

import controller.InventoryRowView;
import inventory.analytics.InventoryPolicyService;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates a configurable set of {@link NotificationRule}s across the current
 * inventory and collects the resulting alerts. New conditions can be added by
 * appending to {@link #defaultRules()} without changing this evaluation logic.
 */
public final class NotificationService {

    private final List<NotificationRule> rules;
    private final InventoryPolicyService policyService;

    public NotificationService() {
        this(defaultRules(), new InventoryPolicyService());
    }

    public NotificationService(List<NotificationRule> rules, InventoryPolicyService policyService) {
        this.rules = rules;
        this.policyService = policyService;
    }

    public static List<NotificationRule> defaultRules() {
        return List.of(new LowStockRule(), new ExpirationRule(), new RestockRule());
    }

    public List<Notification> evaluate(List<InventoryRowView> rows) {
        List<Notification> notifications = new ArrayList<>();
        for (InventoryRowView row : rows) {
            for (NotificationRule rule : rules) {
                Notification notification = rule.evaluate(row, policyService);
                if (notification != null) {
                    notifications.add(notification);
                }
            }
        }
        notifications.sort((a, b) -> severityRank(b.getSeverity()) - severityRank(a.getSeverity()));
        return notifications;
    }

    private static int severityRank(Notification.Severity severity) {
        return switch (severity) {
            case CRITICAL -> 2;
            case WARNING -> 1;
            case INFO -> 0;
        };
    }
}
