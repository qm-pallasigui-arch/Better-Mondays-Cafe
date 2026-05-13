package loginregister;

import java.io.FileOutputStream;
import java.io.IOException;

public class UserDataManager {

    private static final String FILE_PATH = "users.txt";

    /**
     * Role enumeration: ADMIN has full access, STAFF has limited access
     */
    public enum Role {
        ADMIN, STAFF
    }

    /**
     * Save a user with hashed password and role.
     * Format in file: username,passwordHash,role
     *
     * @param username the username
     * @param password the plaintext password (will be hashed)
     * @param role the user role (ADMIN or STAFF)
     * @throws IOException if file write fails
     */
    public static void saveUser(String username, String password, Role role) throws IOException {
        String hashedPassword = PasswordHasher.hashPassword(password);
        String data = username + "," + hashedPassword + "," + role.name() + "\n";
        try (FileOutputStream fos = new FileOutputStream(FILE_PATH, true)) {
            fos.write(data.getBytes());
        }
    }

    /**
     * Verify user credentials (username and password).
     *
     * @param username the username to check
     * @param password the plaintext password to verify
     * @return true if credentials are valid, false otherwise
     */
    public static boolean verifyCredentials(String username, String password) {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 3); // Limit split to 3 parts: username, hash, role
                if (parts.length >= 2) {
                    String fileUser = parts[0].trim();
                    String fileHash = parts[1].trim();

                    if (fileUser.equals(username)) {
                        return PasswordHasher.verifyPassword(password, fileHash);
                    }
                }
            }
        } catch (java.io.IOException e) {
            return false;
        }
        return false;
    }

    /**
     * Get the role of a user.
     *
     * @param username the username to look up
     * @return the Role (ADMIN or STAFF), or null if user not found
     */
    public static Role getUserRole(String username) {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 3);
                if (parts.length >= 3) {
                    String fileUser = parts[0].trim();
                    String roleStr = parts[2].trim();

                    if (fileUser.equals(username)) {
                        try {
                            return Role.valueOf(roleStr);
                        } catch (IllegalArgumentException e) {
                            return Role.STAFF; // Default to STAFF if role is invalid
                        }
                    }
                }
            }
        } catch (java.io.IOException e) {
            return null;
        }
        return null;
    }
}
