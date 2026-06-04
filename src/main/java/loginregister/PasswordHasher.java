package loginregister;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for password hashing and verification using SHA-256.
 * Includes salt-based hashing for enhanced security.
 */
public class PasswordHasher {
    private static final java.nio.charset.Charset CHARSET = StandardCharsets.UTF_8;

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
            byte[] hashedPassword = md.digest(password.getBytes(CHARSET));

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
     * @param password   the plaintext password to verify
     * @param storedHash the stored hash in format "salt:hash"
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) {
            return false;
        }

        String[] parts = storedHash.split(":");
        try {
            if (parts.length == 2) {
                return verifySaltedHash(password, parts[0], parts[1]);
            }
            if (isHexString(storedHash) && storedHash.length() == 64) {
                return verifyUnsaltedHexHash(password, storedHash);
            }
            return false;
        } catch (IllegalArgumentException e) {
            return false; // Invalid Base64 or hex format
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private static boolean verifySaltedHash(String password, String saltBase64, String hashBase64)
            throws NoSuchAlgorithmException {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        byte[] storedHashBytes = Base64.getDecoder().decode(hashBase64);

        MessageDigest md = MessageDigest.getInstance(ALGORITHM);
        md.update(salt);
        byte[] passwordHashBytes = md.digest(password.getBytes(CHARSET));

        return java.util.Arrays.equals(passwordHashBytes, storedHashBytes);
    }

    private static boolean verifyUnsaltedHexHash(String password, String hashHex) throws NoSuchAlgorithmException {
        byte[] storedHashBytes = hexDecode(hashHex);
        MessageDigest md = MessageDigest.getInstance(ALGORITHM);
        byte[] passwordHashBytes = md.digest(password.getBytes(CHARSET));
        return java.util.Arrays.equals(passwordHashBytes, storedHashBytes);
    }

    private static boolean isHexString(String value) {
        return value != null && value.matches("(?i)^[0-9a-f]+$");
    }

    private static byte[] hexDecode(String hex) {
        int length = hex.length();
        byte[] bytes = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return bytes;
    }
}
