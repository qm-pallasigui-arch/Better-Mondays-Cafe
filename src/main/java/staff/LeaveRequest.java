package staff;

public class LeaveRequest {

    private final int id;
    private final int staffId;
    private final String username;
    private final String startDate;
    private final String endDate;
    private final String status;
    private final String createdAt;
    private final String resolvedAt;

    public LeaveRequest(int id, int staffId, String username,
            String startDate, String endDate,
            String status, String createdAt, String resolvedAt) {
        this.id = id;
        this.staffId = staffId;
        this.username = username;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    public int getId()           { return id; }
    public int getStaffId()      { return staffId; }
    public String getUsername()  { return username; }
    public String getStartDate() { return startDate; }
    public String getEndDate()   { return endDate; }
    public String getStatus()    { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getResolvedAt(){ return resolvedAt; }
}
