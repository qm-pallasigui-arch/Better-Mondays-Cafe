package inventory.validation;

import java.time.LocalDate;

public final class InventoryBatchValidator {

    private InventoryBatchValidator() {
    }

    public static double parseQuantity(String quantity) {
        if (quantity == null || quantity.trim().isEmpty()) {
            throw new IllegalArgumentException("Quantity is required");
        }
        double q = Double.parseDouble(quantity.trim());
        if (q <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        return q;
    }

    public static String normalizeExpiry(String expiry) {
        if (expiry == null) return null;
        String t = expiry.trim();
        if (t.isEmpty() || "none".equalsIgnoreCase(t)) {
            return null;
        }
        LocalDate date = LocalDate.parse(t);
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiry date cannot be in the past");
        }
        return date.toString();
    }
}
