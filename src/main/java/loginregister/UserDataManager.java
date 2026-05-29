package loginregister;

import java.util.ArrayList;
import java.util.List;
import persistence.sqlite.SQLiteUserRepository;

public class UserDataManager {
    private static final SQLiteUserRepository USER_REPOSITORY;

    static {
        try {
            USER_REPOSITORY = new SQLiteUserRepository();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

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
    public static void saveUser(String username, String password, Role role) throws java.io.IOException {
        try {
            USER_REPOSITORY.saveUser(username, password, role);
        } catch (java.io.IOException e) {
            throw e;
        } catch (Exception e) {
            throw new java.io.IOException("Unable to save user", e);
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
        try {
            return USER_REPOSITORY.verifyCredentials(username, password);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the role of a user.
     *
     * @param username the username to look up
     * @return the Role (ADMIN or STAFF), or null if user not found
     */
    public static Role getUserRole(String username) {
        try {
            Role role = USER_REPOSITORY.getUserRole(username);
            return role != null ? role : Role.STAFF;
        } catch (Exception e) {
            return null;
        }
    }

    public static List<UserAccount> listUsers() {
        try {
            return USER_REPOSITORY.listUsers();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static boolean updateUserRole(String username, Role role) {
        try {
            USER_REPOSITORY.updateUserRole(username, role);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean updateUsername(String currentUsername, String newUsername, String currentPassword) {
        try {
            USER_REPOSITORY.updateUsername(currentUsername, newUsername, currentPassword);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean updatePassword(String username, String currentPassword, String newPassword) {
        try {
            USER_REPOSITORY.updatePassword(username, currentPassword, newPassword);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean resetPassword(String username, String newPassword) {
        try {
            USER_REPOSITORY.resetPassword(username, newPassword);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
