package persistence;

import ui.OrderQueuePanel.Receipt;
import ui.OrderQueuePanel.ReceiptItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight JSON-serializable message exchanged over WebSocket.
 *
 * Two message types:
 * NEW_ORDER — POS placed a new order; carries full receipt data
 * STATUS_CHANGE — kitchen moved an order to a new lane
 *
 * We use hand-rolled JSON (no Gson/Jackson dependency) so the code
 * drops straight into your existing project without any new JARs.
 */
public class OrderPayload {

    // ── Message type constants ────────────────────────────────────────────
    public static final String TYPE_NEW_ORDER = "NEW_ORDER";
    public static final String TYPE_STATUS_CHANGE = "STATUS_CHANGE";
    public static final String TYPE_MENU_UPDATE = "MENU_UPDATE";
    public static final String TYPE_INVENTORY_UPDATE = "INVENTORY_UPDATE";

    // ── Fields ────────────────────────────────────────────────────────────
    public String type; // TYPE_NEW_ORDER | TYPE_STATUS_CHANGE
    public int orderId;
    public int statusCode; // used for STATUS_CHANGE; 0 for NEW_ORDER

    // Receipt fields (populated for NEW_ORDER)
    public String customerName;
    public String timestamp;
    public double subtotal;
    public double vat;
    public double totalInclusive;
    public double cash;
    public double change;
    public String discountType;
    public List<Item> items = new ArrayList<>();
    // Free-form JSON for other entity updates (menu/inventory)
    public String entityJson = "";

    /** Flat item representation (mirrors ReceiptItem). */
    public static class Item {
        public String description;
        public int quantity;
        public double unitPrice;
        public double lineTotal;

        public Item() {
        }

        public Item(String description, int quantity, double unitPrice, double lineTotal) {
            this.description = description;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.lineTotal = lineTotal;
        }
    }

    // ── Factory methods ───────────────────────────────────────────────────

    /** Build a NEW_ORDER payload from a Receipt. */
    public static OrderPayload fromReceipt(Receipt receipt) {
        OrderPayload p = new OrderPayload();
        p.type = TYPE_NEW_ORDER;
        p.orderId = receipt.orderId;
        p.statusCode = 0;
        p.customerName = receipt.customerName;
        p.timestamp = receipt.timestamp;
        p.subtotal = receipt.subtotal;
        p.vat = receipt.vat;
        p.totalInclusive = receipt.totalInclusive;
        p.cash = receipt.cash;
        p.change = receipt.change;
        p.discountType = receipt.discountType == null ? "" : receipt.discountType;
        for (ReceiptItem ri : receipt.items) {
            p.items.add(new Item(ri.description, ri.quantity, ri.unitPrice, ri.lineTotal));
        }
        return p;
    }

    /** Build a STATUS_CHANGE payload. */
    public static OrderPayload statusChange(int orderId, int statusCode) {
        OrderPayload p = new OrderPayload();
        p.type = TYPE_STATUS_CHANGE;
        p.orderId = orderId;
        p.statusCode = statusCode;
        return p;
    }

    public static OrderPayload menuUpdate(String entityJson) {
        OrderPayload p = new OrderPayload();
        p.type = TYPE_MENU_UPDATE;
        p.entityJson = entityJson == null ? "" : entityJson;
        return p;
    }

    public static OrderPayload inventoryUpdate(String entityJson) {
        OrderPayload p = new OrderPayload();
        p.type = TYPE_INVENTORY_UPDATE;
        p.entityJson = entityJson == null ? "" : entityJson;
        return p;
    }

    // ── Serialisation: hand-rolled JSON (no external library needed) ──────

    /** Serialize this payload to a JSON string. */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\":").append(q(type)).append(",");
        sb.append("\"orderId\":").append(orderId).append(",");
        sb.append("\"statusCode\":").append(statusCode).append(",");
        sb.append("\"customerName\":").append(q(customerName)).append(",");
        sb.append("\"timestamp\":").append(q(timestamp)).append(",");
        sb.append("\"subtotal\":").append(subtotal).append(",");
        sb.append("\"vat\":").append(vat).append(",");
        sb.append("\"totalInclusive\":").append(totalInclusive).append(",");
        sb.append("\"cash\":").append(cash).append(",");
        sb.append("\"change\":").append(change).append(",");
        sb.append("\"discountType\":").append(q(discountType)).append(",");
        sb.append("\"items\":[");
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            sb.append("{");
            sb.append("\"description\":").append(q(it.description)).append(",");
            sb.append("\"quantity\":").append(it.quantity).append(",");
            sb.append("\"unitPrice\":").append(it.unitPrice).append(",");
            sb.append("\"lineTotal\":").append(it.lineTotal);
            sb.append("}");
            if (i < items.size() - 1)
                sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    /** Parse a JSON string back into an OrderPayload. */
    public static OrderPayload fromJson(String json) {
        OrderPayload p = new OrderPayload();
        if (json == null || json.isBlank())
            return p;

        p.type = strField(json, "type");
        p.orderId = intField(json, "orderId");
        p.statusCode = intField(json, "statusCode");
        p.customerName = strField(json, "customerName");
        p.timestamp = strField(json, "timestamp");
        p.subtotal = dblField(json, "subtotal");
        p.vat = dblField(json, "vat");
        p.totalInclusive = dblField(json, "totalInclusive");
        p.cash = dblField(json, "cash");
        p.change = dblField(json, "change");
        p.discountType = strField(json, "discountType");

        p.entityJson = strField(json, "entityJson");

        // Parse items array
        int arrStart = json.indexOf("\"items\":[");
        if (arrStart >= 0) {
            int open = json.indexOf('[', arrStart);
            int close = json.lastIndexOf(']');
            if (open >= 0 && close > open) {
                String arr = json.substring(open + 1, close);
                // Split on },{
                String[] objects = arr.split("\\},\\s*\\{");
                for (String obj : objects) {
                    obj = obj.replace("{", "").replace("}", "").trim();
                    if (obj.isBlank())
                        continue;
                    Item it = new Item();
                    it.description = strField("{" + obj + "}", "description");
                    it.quantity = intField("{" + obj + "}", "quantity");
                    it.unitPrice = dblField("{" + obj + "}", "unitPrice");
                    it.lineTotal = dblField("{" + obj + "}", "lineTotal");
                    p.items.add(it);
                }
            }
        }
        return p;
    }

    /** Convert this payload back into a Receipt (for NEW_ORDER messages). */
    public Receipt toReceipt() {
        List<ReceiptItem> receiptItems = new ArrayList<>();
        for (Item it : items) {
            receiptItems.add(new ReceiptItem(it.description, it.quantity, it.unitPrice, it.lineTotal));
        }
        return new Receipt(orderId, customerName, receiptItems,
                timestamp, subtotal, vat, totalInclusive,
                cash, change, discountType);
    }

    // ── JSON helpers ─────────────────────────────────────────────────────

    /** Quote and escape a string value for JSON. */
    private static String q(String s) {
        if (s == null)
            return "\"\"";
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }

    /** Extract a string field value from a flat JSON object. */
    private static String strField(String json, String key) {
        // Matches: "key":"value"
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start < 0)
            return "";
        start += pattern.length();
        int end = start;
        boolean escaped = false;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (escaped) {
                escaped = false;
                end++;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                end++;
                continue;
            }
            if (c == '"')
                break;
            end++;
        }
        return json.substring(start, end)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\\", "\\");
    }

    /** Extract an integer field value from a flat JSON object. */
    private static int intField(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start < 0)
            return 0;
        start += pattern.length();
        // Skip leading whitespace / quote (shouldn't be there for int, but be safe)
        while (start < json.length() && json.charAt(start) == ' ')
            start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-'))
            end++;
        String val = json.substring(start, end).trim();
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Extract a double field value from a flat JSON object. */
    private static double dblField(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start < 0)
            return 0.0;
        start += pattern.length();
        while (start < json.length() && json.charAt(start) == ' ')
            start++;
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (Character.isDigit(c) || c == '-' || c == '.' || c == 'E' || c == 'e')
                end++;
            else
                break;
        }
        String val = json.substring(start, end).trim();
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
