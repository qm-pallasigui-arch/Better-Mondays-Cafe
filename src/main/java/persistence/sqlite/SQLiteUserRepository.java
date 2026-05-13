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
                     "SELECT username, role, created_at FROM users ORDER BY username ASC");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Role role;
                try {
                    role = Role.valueOf(resultSet.getString("role"));
                } catch (Exception e) {
                    role = Role.STAFF;
                }
                users.add(new UserAccount(
                        resultSet.getString("username"),
                        role,
                        resultSet.getString("created_at")
                ));
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
}
