package notifications;

public final class Notification {

    public enum Severity { INFO, WARNING, CRITICAL }

    private final Severity severity;
    private final String itemName;
    private final String message;

    public Notification(Severity severity, String itemName, String message) {
        this.severity = severity;
        this.itemName = itemName;
        this.message = message;
    }

    public Severity getSeverity() { return severity; }
    public String getItemName() { return itemName; }
    public String getMessage() { return message; }
}
