package staff;

public class PasswordResetRequest {

    public enum Status { PENDING, APPROVED, REJECTED }

    private final int id;
    private final String username;
    private final Status status;
    private final String requestedAt;
    private final String resolvedAt;

    public PasswordResetRequest(int id, String username, Status status,
            String requestedAt, String resolvedAt) {
        this.id = id;
        this.username = username;
        this.status = status;
        this.requestedAt = requestedAt;
        this.resolvedAt = resolvedAt;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public Status getStatus() { return status; }
    public String getRequestedAt() { return requestedAt; }
    public String getResolvedAt() { return resolvedAt; }
}
