package persistence.sqlite;

import persistence.AppDatabase;
import persistence.PasswordResetRepository;
import staff.PasswordResetRequest;
import staff.PasswordResetRequest.Status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SQLitePasswordResetRepository implements PasswordResetRepository {

    public SQLitePasswordResetRepository() throws Exception {
        AppDatabase.ensureInitialized();
    }

    @Override
    public void createRequest(String username) throws Exception {
        try (Connection conn = AppDatabase.openConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO password_reset_requests(username) VALUES (?)")) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }

    @Override
    public List<PasswordResetRequest> listAllRequests() throws Exception {
        return query("SELECT id, username, status, requested_at, resolved_at "
                + "FROM password_reset_requests ORDER BY requested_at DESC", false, null);
    }

    @Override
    public List<PasswordResetRequest> listPendingRequests() throws Exception {
        return query("SELECT id, username, status, requested_at, resolved_at "
                + "FROM password_reset_requests WHERE status = 'PENDING' ORDER BY requested_at DESC",
                false, null);
    }

    @Override
    public boolean hasPendingRequest(String username) throws Exception {
        try (Connection conn = AppDatabase.openConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM password_reset_requests WHERE username = ? AND status = 'PENDING'")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    @Override
    public void approveRequest(int requestId, String username, String newPasswordHash) throws Exception {
        try (Connection conn = AppDatabase.openConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement updPw = conn.prepareStatement(
                        "UPDATE users SET password_hash = ? WHERE username = ?")) {
                    updPw.setString(1, newPasswordHash);
                    updPw.setString(2, username);
                    updPw.executeUpdate();
                }
                try (PreparedStatement updReq = conn.prepareStatement(
                        "UPDATE password_reset_requests "
                        + "SET status = 'APPROVED', resolved_at = datetime('now','localtime') "
                        + "WHERE id = ?")) {
                    updReq.setInt(1, requestId);
                    updReq.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public void rejectRequest(int requestId) throws Exception {
        try (Connection conn = AppDatabase.openConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE password_reset_requests "
                     + "SET status = 'REJECTED', resolved_at = datetime('now','localtime') "
                     + "WHERE id = ?")) {
            stmt.setInt(1, requestId);
            stmt.executeUpdate();
        }
    }

    private List<PasswordResetRequest> query(String sql, boolean withParam, String param) throws Exception {
        List<PasswordResetRequest> list = new ArrayList<>();
        try (Connection conn = AppDatabase.openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (withParam) stmt.setString(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Status status;
                    try { status = Status.valueOf(rs.getString("status")); }
                    catch (IllegalArgumentException e) { status = Status.PENDING; }
                    list.add(new PasswordResetRequest(
                            rs.getInt("id"),
                            rs.getString("username"),
                            status,
                            rs.getString("requested_at"),
                            rs.getString("resolved_at")));
                }
            }
        }
        return list;
    }
}
