package loginregister;

import java.util.ArrayList;
import java.util.List;
import loginregister.PasswordHasher;
import persistence.PasswordResetRepository;
import persistence.sqlite.SQLitePasswordResetRepository;
import persistence.sqlite.SQLiteUserRepository;
import staff.PasswordResetRequest;

public class UserDataManager {
    private static final SQLiteUserRepository USER_REPOSITORY;
    private static final PasswordResetRepository RESET_REPOSITORY;

    static {
        try {
            USER_REPOSITORY = new SQLiteUserRepository();
            RESET_REPOSITORY = new SQLitePasswordResetRepository();
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
     * @param role     the user role (ADMIN or STAFF)
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
            java.util.logging.Logger.getLogger(UserDataManager.class.getName())
                    .log(java.util.logging.Level.WARNING,
                            "Failed to verify credentials for user: " + username, e);
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

    public static boolean resetPassword(String username, String newPassword) throws Exception {
        USER_REPOSITORY.resetPassword(username, newPassword);
        return true;
    }

    public static void createUser(String username, String plainPassword, Role role,
            String fullName, int age, String birthdate,
            String address, String mobile, String gender) throws java.io.IOException {
        try {
            USER_REPOSITORY.createUser(username, plainPassword, role,
                    fullName, age, birthdate, address, mobile, gender);
        } catch (java.io.IOException e) {
            throw e;
        } catch (Exception e) {
            throw new java.io.IOException("Unable to create user: " + e.getMessage(), e);
        }
    }

    // --- Password reset request methods ---

    public static void submitResetRequest(String username) throws Exception {
        RESET_REPOSITORY.createRequest(username);
    }

    public static boolean hasPendingResetRequest(String username) {
        try {
            return RESET_REPOSITORY.hasPendingRequest(username);
        } catch (Exception e) {
            return false;
        }
    }

    public static List<PasswordResetRequest> listAllResetRequests() {
        try {
            return RESET_REPOSITORY.listAllRequests();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static List<PasswordResetRequest> listPendingResetRequests() {
        try {
            return RESET_REPOSITORY.listPendingRequests();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void approveResetRequest(int requestId, String username, String newPlainPassword) throws Exception {
        String hash = PasswordHasher.hashPassword(newPlainPassword);
        RESET_REPOSITORY.approveRequest(requestId, username, hash);
    }

    public static void rejectResetRequest(int requestId) throws Exception {
        RESET_REPOSITORY.rejectRequest(requestId);
    }
}
