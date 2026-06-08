package notifications;

import controller.InventoryRowView;
import inventory.InventoryBatch;
import inventory.analytics.InventoryPolicyService;
import java.time.LocalDate;

/**
 * Flags items with batches that are already expired or expiring within the
 * warning window — the FEFO concern surfaced as an actionable alert instead
 * of something staff only notice while browsing batch details.
 */
public final class ExpirationRule implements NotificationRule {

    private static final int EXPIRING_SOON_DAYS = 7;

    @Override
    public Notification evaluate(InventoryRowView row, InventoryPolicyService policyService) {
        if (row.getBatches() == null || row.getBatches().isEmpty()) {
            return null;
        }
        LocalDate today = LocalDate.now();
        int expired = 0;
        int expiringSoon = 0;
        for (InventoryBatch batch : row.getBatches()) {
            if (batch.isArchived() || batch.getQuantity() <= 0) {
                continue;
            }
            String expiry = batch.getExpiryDate();
            if (expiry == null || expiry.isBlank()) {
                continue;
            }
            try {
                LocalDate date = LocalDate.parse(expiry);
                if (date.isBefore(today)) {
                    expired++;
                } else if (!date.isAfter(today.plusDays(EXPIRING_SOON_DAYS))) {
                    expiringSoon++;
                }
            } catch (Exception ignored) {
            }
        }
        if (expired > 0) {
            return new Notification(Notification.Severity.CRITICAL, row.getName(),
                    row.getName() + " has " + expired + " expired batch" + (expired == 1 ? "" : "es")
                            + " — remove from stock (FEFO: oldest batches deduct first).");
        }
        if (expiringSoon > 0) {
            return new Notification(Notification.Severity.WARNING, row.getName(),
                    row.getName() + " has " + expiringSoon + " batch" + (expiringSoon == 1 ? "" : "es")
                            + " expiring within " + EXPIRING_SOON_DAYS + " days.");
        }
        return null;
    }
}
