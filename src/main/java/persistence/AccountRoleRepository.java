package persistence;

import loginregister.UserAccount;
import loginregister.UserDataManager.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * Abstract base class for account/role persistence.
 * Provides profile migration, profile fetch, and profile update
 * on top of the abstract contract that SQLiteUserRepository fulfils.
 */
public abstract class AccountRoleRepository {

    // =========================================================================
    // Abstract contract (already implemented by SQLiteUserRepository)
    // =========================================================================

    public abstract List<UserAccount> listUsers() throws Exception;

    public abstract void updateUserRole(String username, Role role) throws Exception;

    public abstract void deleteUser(String username) throws Exception;

    public abstract void registerStaff(
            String username,
            String password,
            String fullName,
            String email,
            String dateOfBirth,
            String employmentStart) throws Exception;

    // =========================================================================
    // Profile migration — call once from the subclass constructor
    // =========================================================================

    /**
     * Adds the four personal-info columns to the users table if they are not
     * already present. Safe to call on every startup; duplicate-column errors
     * are silently ignored.
     */
    protected void migrateProfileColumns() throws Exception {
        String[] alterStatements = {
                "ALTER TABLE users ADD COLUMN full_name        TEXT",
                "ALTER TABLE users ADD COLUMN date_of_birth    TEXT",
                "ALTER TABLE users ADD COLUMN email            TEXT",
                "ALTER TABLE users ADD COLUMN employment_start TEXT"
        };

        try (Connection conn = AppDatabase.openConnection();
                Statement stmt = conn.createStatement()) {

            for (String sql : alterStatements) {
                try {
                    stmt.execute(sql);
                } catch (Exception ex) {
                    if (!ex.getMessage().toLowerCase().contains("duplicate column")) {
                        throw ex;
                    }
                }
            }
        }
    }

    // =========================================================================
    // Profile fetch
    // =========================================================================

    /**
     * Returns the full profile for one user, including the four personal-info
     * fields. Returns {@code null} if the username does not exist.
     */
    public loginregister.UserAccount getUserProfile(String username) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT username, role, created_at, " +
                                "full_name, date_of_birth, email, employment_start " +
                                "FROM users WHERE username = ?")) {

            statement.setString(1, username);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapProfileRow(rs);
                }
            }
        }
        return null;
    }

    // =========================================================================
    // Profile update
    // =========================================================================

    /**
     * Saves the four personal-info fields for the given user.
     * Blank or null values are stored as SQL NULL.
     */
    public void updateUserProfile(
            String username,
            String fullName,
            String dateOfBirth,
            String email,
            String employmentStart) throws Exception {

        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE users " +
                                "SET full_name = ?, date_of_birth = ?, email = ?, employment_start = ? " +
                                "WHERE username = ?")) {

            statement.setString(1, nullIfBlank(fullName));
            statement.setString(2, nullIfBlank(dateOfBirth));
            statement.setString(3, nullIfBlank(email));
            statement.setString(4, nullIfBlank(employmentStart));
            statement.setString(5, username);
            statement.executeUpdate();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

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

    private static String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
