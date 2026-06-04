package persistence.sqlite;

import loginregister.PasswordHasher;
import loginregister.UserAccount;
import loginregister.UserDataManager.Role;
import persistence.AppDatabase;
import persistence.AccountRoleRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite-backed user repository.
 */
public class SQLiteUserRepository extends persistence.AccountRoleRepository {

    public SQLiteUserRepository() throws Exception {
        // Ensure DB and schema are initialized (creates 'users' table and migrations)
        try {
            persistence.AppDatabase.ensureInitialized();
            migrateProfileColumns();
        } catch (java.sql.SQLException e) {
            throw new Exception("Unable to initialize database", e);
        }
    }

    // =========================================================================
    // Auth
    // =========================================================================

    public void saveUser(String username, String password, Role role) throws java.io.IOException {
        String hashed = PasswordHasher.hashPassword(password);
        try (Connection conn = AppDatabase.openConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO users(username, password_hash, role) VALUES (?, ?, ?)")) {
            stmt.setString(1, username);
            stmt.setString(2, hashed);
            stmt.setString(3, role.name());
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new java.io.IOException("Unable to save user", e);
        }
    }

    public boolean verifyCredentials(String username, String password) throws Exception {
        try (Connection conn = AppDatabase.openConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT password_hash FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next())
                    return false;
                return PasswordHasher.verifyPassword(password, rs.getString("password_hash"));
            }
        }
    }

    public Role getUserRole(String username) throws Exception {
        try (Connection conn = AppDatabase.openConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT role FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next())
                    return null;
                try {
                    return Role.valueOf(rs.getString("role"));
                } catch (Exception ex) {
                    return Role.STAFF;
                }
            }
        }
    }

    // =========================================================================
    // AccountRoleRepository — listUsers & updateUserRole
    // (getUserProfile, updateUserProfile, migrateProfileColumns
    // are all inherited from AccountRoleRepository)
    // =========================================================================

    /**
     * Returns all users including the four personal-info fields.
     */
    @Override
    public List<UserAccount> listUsers() throws Exception {
        List<UserAccount> out = new ArrayList<>();
        try (Connection conn = AppDatabase.openConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT username, role, created_at, " +
                                "full_name, date_of_birth, email, employment_start " +
                                "FROM users ORDER BY id DESC");
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                out.add(mapProfileRow(rs));
            }
        }
        return out;
    }

    @Override
    public void updateUserRole(String username, Role role) throws Exception {
        try (Connection conn = AppDatabase.openConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE users SET role = ? WHERE username = ?")) {
            stmt.setString(1, role.name());
            stmt.setString(2, username);
            stmt.executeUpdate();
        }
    }

    @Override
    public void deleteUser(String username) throws Exception {
        try (Connection conn = AppDatabase.openConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }

    @Override
    public void registerStaff(
            String username,
            String password,
            String fullName,
            String email,
            String dateOfBirth,
            String employmentStart) throws Exception {
        saveUser(username, password, Role.STAFF);
        updateUserProfile(username, fullName, dateOfBirth, email, employmentStart);
    }

    protected static UserAccount mapProfileRow(ResultSet rs) throws Exception {
        Role role;
        try {
            role = Role.valueOf(rs.getString("role"));
        } catch (Exception e) {
            role = Role.STAFF;
        }
        return new UserAccount(
                rs.getString("username"),
                role,
                rs.getString("created_at"),
                rs.getString("full_name"),
                rs.getString("date_of_birth"),
                rs.getString("email"),
                rs.getString("employment_start"));
    }

    // =========================================================================
    // Username / password management — unchanged from original
    // =========================================================================

    public void updateUsername(
            String currentUsername, String newUsername, String currentPassword) throws Exception {
        if (!verifyCredentials(currentUsername, currentPassword)) {
            throw new IllegalArgumentException("Invalid current password");
        }
        try (Connection conn = AppDatabase.openConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE users SET username = ? WHERE username = ?")) {
            stmt.setString(1, newUsername);
            stmt.setString(2, currentUsername);
            stmt.executeUpdate();
        }
    }

    public void updatePassword(
            String username, String currentPassword, String newPassword) throws Exception {
        if (!verifyCredentials(username, currentPassword)) {
            throw new IllegalArgumentException("Invalid current password");
        }
        String hashed = PasswordHasher.hashPassword(newPassword);
        try (Connection conn = AppDatabase.openConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE users SET password_hash = ? WHERE username = ?")) {
            stmt.setString(1, hashed);
            stmt.setString(2, username);
            stmt.executeUpdate();
        }
    }

    public void resetPassword(String username, String newPassword) throws Exception {
        String hashed = PasswordHasher.hashPassword(newPassword);
        try (Connection conn = AppDatabase.openConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE users SET password_hash = ? WHERE username = ?")) {
            stmt.setString(1, hashed);
            stmt.setString(2, username);
            stmt.executeUpdate();
        }
    }
}