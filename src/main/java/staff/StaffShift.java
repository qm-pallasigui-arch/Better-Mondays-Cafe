package staff;

/** Simple model for staff shift tracking. */
public class StaffShift {

    private long id;
    private int staffId;
    private String username;
    private String startedAt;
    private String endedAt;
    private String notes;

    public StaffShift(long id, int staffId, String username, String startedAt, String endedAt, String notes) {
        this.id = id;
        this.staffId = staffId;
        this.username = username;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.notes = notes;
    }

    /** Legacy constructor — staffId defaults to 0. */
    public StaffShift(long id, String username, String startedAt, String endedAt, String notes) {
        this(id, 0, username, startedAt, endedAt, notes);
    }

    public StaffShift(String username) {
        this(-1, 0, username, null, null, null);
    }

    public long getId() { return id; }
    public int getStaffId() { return staffId; }
    public String getUsername() { return username; }
    public String getStartedAt() { return startedAt; }
    public String getEndedAt() { return endedAt; }
    public String getNotes() { return notes; }

    public void setId(long id) { this.id = id; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }
    public void setEndedAt(String endedAt) { this.endedAt = endedAt; }
    public void setNotes(String notes) { this.notes = notes; }
}
