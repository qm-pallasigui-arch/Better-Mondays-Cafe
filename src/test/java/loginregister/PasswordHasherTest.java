package loginregister;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    void hashPasswordProducesValidSaltHashPair() {
        String password = "Admin123!";
        String hashed = PasswordHasher.hashPassword(password);

        assertNotNull(hashed);
        assertTrue(hashed.contains(":"), "Hash output should contain a salt and hash separated by ':'");
        assertTrue(PasswordHasher.verifyPassword(password, hashed));
        assertFalse(PasswordHasher.verifyPassword("wrongPassword", hashed));
    }

    @Test
    void verifyUnsaltedHexHashLegacyFormat() throws Exception {
        String password = "Admin123!";
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }

        assertTrue(PasswordHasher.verifyPassword(password, hex.toString()));
    }

    @Test
    void verifyPasswordReturnsFalseForInvalidStoredFormat() {
        assertFalse(PasswordHasher.verifyPassword("Admin123!", "invalid-format"));
    }
}
