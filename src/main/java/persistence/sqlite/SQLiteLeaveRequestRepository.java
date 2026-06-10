package persistence.sqlite;

import persistence.AppDatabase;
import persistence.LeaveRequestRepository;
import staff.LeaveRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SQLiteLeaveRequestRepository implements LeaveRequestRepository {

    public SQLiteLeaveRequestRepository() throws Exception {
        AppDatabase.ensureInitialized();
    }

    @Override
    public void createRequest(String username, String startDate, String endDate) throws Exception {
        try (Connection conn = AppDatabase.openConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO staff_leave_requests(username, start_date, end_date) VALUES (?, ?, ?)")) {
            stmt.setString(1, username);
            stmt.setString(2, startDate);
            stmt.setString(3, endDate);
            stmt.executeUpdate();
        }
    }

    @Override
    public List<LeaveRequest> listAllRequests() throws Exception {
        return query(
                "SELECT lr.id, COALESCE(u.staff_id, 0) AS staff_id, lr.username, "
                + "lr.start_date, lr.end_date, lr.status, lr.created_at, lr.resolved_at "
                + "FROM staff_leave_requests lr "
                + "LEFT JOIN users u ON u.username = lr.username "
                + "ORDER BY lr.id DESC",
                false, null);
    }

    @Override
    public List<LeaveRequest> listRequestsForUser(String username) throws Exception {
        return query(
                "SELECT lr.id, COALESCE(u.staff_id, 0) AS staff_id, lr.username, "
                + "lr.start_date, lr.end_date, lr.status, lr.created_at, lr.resolved_at "
                + "FROM staff_leave_requests lr "
                + "LEFT JOIN users u ON u.username = lr.username "
                + "WHERE lr.username = ? "
                + "ORDER BY lr.id DESC",
                true, username);
    }

    @Override
    public void approveRequest(int id) throws Exception {
        try (Connection conn = AppDatabase.openConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE staff_leave_requests "
                     + "SET status = 'approved', resolved_at = datetime('now','localtime') "
                     + "WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public void rejectRequest(int id) throws Exception {
        try (Connection conn = AppDatabase.openConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE staff_leave_requests "
                     + "SET status = 'rejected', resolved_at = datetime('now','localtime') "
                     + "WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private List<LeaveRequest> query(String sql, boolean withParam, String param) throws Exception {
        List<LeaveRequest> list = new ArrayList<>();
        try (Connection conn = AppDatabase.openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (withParam) stmt.setString(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new LeaveRequest(
                            rs.getInt("id"),
                            rs.getInt("staff_id"),
                            rs.getString("username"),
                            rs.getString("start_date"),
                            rs.getString("end_date"),
                            rs.getString("status"),
                            rs.getString("created_at"),
                            rs.getString("resolved_at")));
                }
            }
        }
        return list;
    }
}
