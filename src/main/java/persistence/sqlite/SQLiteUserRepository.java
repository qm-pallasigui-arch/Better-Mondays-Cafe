package persistence.sqlite;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import loginregister.PasswordHasher;
import loginregister.UserAccount;
import loginregister.UserDataManager.Role;
import persistence.AccountRoleRepository;
import persistence.AppDatabase;
import persistence.LegacyUserMigration;
import persistence.UserRepository;

public class SQLiteUserRepository implements UserRepository, AccountRoleRepository {

    private static final Path LEGACY_USERS_FILE = Paths.get("users.txt");

    public SQLiteUserRepository() throws Exception {
        AppDatabase.ensureInitialized();
        LegacyUserMigration.migrateUsersFile(LEGACY_USERS_FILE);
    }

    @Override
    public void saveUser(String username, String plainPassword, Role role) throws Exception {
        String passwordHash = PasswordHasher.hashPassword(plainPassword);
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO users(username, password_hash, role) VALUES (?, ?, ?) "
                                + "ON CONFLICT(username) DO UPDATE SET password_hash = excluded.password_hash, role = excluded.role")) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, role.name());
            statement.executeUpdate();
        }
        // Set staff_id = id on the same connection if it is still 0
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement fix = connection.prepareStatement(
                        "UPDATE users SET staff_id = id WHERE username = ? AND staff_id = 0")) {
            fix.setString(1, username);
            fix.executeUpdate();
        }
    }

    @Override
    public void createUser(String username, String plainPassword, Role role,
            String fullName, int age, String birthdate,
            String address, String mobile, String gender) throws Exception {
        String passwordHash = PasswordHasher.hashPassword(plainPassword);
        try (Connection connection = AppDatabase.openConnection()) {
            // Insert the row first (staff_id defaults to 0 temporarily)
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO users(username, password_hash, role, full_name, age, birthdate, address, mobile, gender) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setString(1, username);
                stmt.setString(2, passwordHash);
                stmt.setString(3, role.name());
                stmt.setString(4, fullName);
                stmt.setInt(5, age);
                stmt.setString(6, birthdate);
                stmt.setString(7, address);
                stmt.setString(8, mobile);
                stmt.setString(9, gender);
                stmt.executeUpdate();
            }
            // Immediately set staff_id = id on the same connection, so there
            // is never a window where two rows both hold staff_id = 0
            try (PreparedStatement fix = connection.prepareStatement(
                    "UPDATE users SET staff_id = id WHERE username = ? AND staff_id = 0")) {
                fix.setString(1, username);
                fix.executeUpdate();
            }
        }
    }

    @Override
    public boolean verifyCredentials(String username, String plainPassword) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT password_hash FROM users WHERE username = ?")) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }
                return PasswordHasher.verifyPassword(plainPassword, resultSet.getString("password_hash"));
            }
        }
    }

    @Override
    public Role getUserRole(String username) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT role FROM users WHERE username = ?")) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                try {
                    return Role.valueOf(resultSet.getString("role"));
                } catch (IllegalArgumentException ex) {
                    return Role.STAFF;
                }
            }
        }
    }

    @Override
    public List<UserAccount> listUsers() throws Exception {
        List<UserAccount> users = new ArrayList<>();
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT staff_id, username, role, full_name, age, birthdate, address, mobile, gender, created_at "
                                + "FROM users ORDER BY username ASC");
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Role role;
                try {
                    role = Role.valueOf(resultSet.getString("role"));
                } catch (Exception e) {
                    role = Role.STAFF;
                }
                users.add(new UserAccount(
                        resultSet.getInt("staff_id"),
                        resultSet.getString("username"),
                        role,
                        resultSet.getString("full_name"),
                        resultSet.getInt("age"),
                        resultSet.getString("birthdate"),
                        resultSet.getString("address"),
                        resultSet.getString("mobile"),
                        resultSet.getString("gender"),
                        resultSet.getString("created_at")));
            }
        }
        return users;
    }

    @Override
    public void updateUserRole(String username, Role role) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE users SET role = ? WHERE username = ?")) {
            statement.setString(1, role.name());
            statement.setString(2, username);
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteUser(String username) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM users WHERE username = ?")) {
            statement.setString(1, username);
            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("User not found");
            }
        }
    }

    public void updateUsername(String currentUsername, String newUsername, String currentPassword) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement verify = connection.prepareStatement(
                        "SELECT password_hash FROM users WHERE username = ?");
                PreparedStatement update = connection.prepareStatement(
                        "UPDATE users SET username = ? WHERE username = ?")) {
            verify.setString(1, currentUsername);
            try (ResultSet resultSet = verify.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Current user not found");
                }
                if (!PasswordHasher.verifyPassword(currentPassword, resultSet.getString("password_hash"))) {
                    throw new SecurityException("Current password is incorrect");
                }
            }

            update.setString(1, newUsername);
            update.setString(2, currentUsername);
            int affected = update.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Unable to update username");
            }
        }
    }

    @Override
    public void updatePassword(String username, String currentPassword, String newPassword) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement verify = connection.prepareStatement(
                        "SELECT password_hash FROM users WHERE username = ?");
                PreparedStatement update = connection.prepareStatement(
                        "UPDATE users SET password_hash = ? WHERE username = ?")) {
            verify.setString(1, username);
            try (ResultSet resultSet = verify.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Current user not found");
                }
                if (!PasswordHasher.verifyPassword(currentPassword, resultSet.getString("password_hash"))) {
                    throw new SecurityException("Current password is incorrect");
                }
            }

            update.setString(1, PasswordHasher.hashPassword(newPassword));
            update.setString(2, username);
            int affected = update.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Unable to update password");
            }
        }
    }

    public void resetPassword(String username, String newPassword) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement update = connection.prepareStatement(
                        "UPDATE users SET password_hash = ? WHERE username = ?")) {
            update.setString(1, PasswordHasher.hashPassword(newPassword));
            update.setString(2, username);
            int affected = update.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Current user not found");
            }
        }
    }
}