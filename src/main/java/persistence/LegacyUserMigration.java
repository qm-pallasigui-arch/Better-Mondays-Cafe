package persistence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import loginregister.PasswordHasher;
import loginregister.UserDataManager.Role;

/**
 * One-time import helper for legacy users.txt data.
 * Supports both the old plaintext format and the Phase 1 hashed format.
 */
public final class LegacyUserMigration {

    private LegacyUserMigration() {
    }

    public static int migrateUsersFile(Path filePath) throws Exception {
        if (!Files.exists(filePath)) {
            return 0;
        }

        if (Files.size(filePath) == 0) {
            return 0;
        }

        try (Connection connection = AppDatabase.openConnection()) {
            return migrateUsersFile(connection, filePath);
        }
    }

    public static int migrateUsersFile(Connection connection, Path filePath) throws Exception {
        if (!Files.exists(filePath) || Files.size(filePath) == 0) {
            return 0;
        }

        int imported = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split(",", 3);
                if (parts.length < 2) {
                    continue;
                }

                String username = parts[0].trim();
                String passwordPart = parts[1].trim();
                String roleText = parts.length >= 3 ? parts[2].trim() : Role.STAFF.name();
                Role role;
                try {
                    role = Role.valueOf(roleText);
                } catch (IllegalArgumentException ex) {
                    role = Role.STAFF;
                }

                if (exists(connection, username)) {
                    continue;
                }

                String passwordHash = passwordPart.contains(":") && passwordPart.contains("=")
                        ? passwordPart
                        : PasswordHasher.hashPassword(passwordPart);

                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO users(username, password_hash, role) VALUES (?, ?, ?)")) {
                    statement.setString(1, username);
                    statement.setString(2, passwordHash);
                    statement.setString(3, role.name());
                    statement.executeUpdate();
                    imported++;
                }
            }
        }
        return imported;
    }

    private static boolean exists(Connection connection, String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM users WHERE username = ? LIMIT 1")) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
