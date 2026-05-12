package loginregister;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for password hashing and verification using SHA-256.
 * Includes salt-based hashing for enhanced security.
 */
public class PasswordHasher {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16; // 128 bits

    /**
     * Hash a password with a random salt using SHA-256.
     * Returns format: "salt:hash" where both are Base64 encoded.
     *
     * @param password the plaintext password to hash
     * @return salt:hash string encoded in Base64
     */
    public static String hashPassword(String password) {
        try {
            // Generate random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // Hash password with salt
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());

            // Encode as Base64 for storage
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hashedPassword);

            return saltBase64 + ":" + hashBase64;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verify a plaintext password against a stored hash.
     *
     * @param password the plaintext password to verify
     * @param storedHash the stored hash in format "salt:hash"
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            // Parse stored hash
            String[] parts = storedHash.split(":");
            if (parts.length != 2) {
                return false;
            }

            String saltBase64 = parts[0];
            String hashBase64 = parts[1];

            // Decode salt and hash from Base64
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            byte[] storedHashBytes = Base64.getDecoder().decode(hashBase64);

            // Hash the provided password with the stored salt
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            byte[] passwordHashBytes = md.digest(password.getBytes());

            // Compare hashes byte-by-byte to prevent timing attacks
            return java.util.Arrays.equals(passwordHashBytes, storedHashBytes);
        } catch (IllegalArgumentException e) {
            return false; // Invalid Base64 format
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
