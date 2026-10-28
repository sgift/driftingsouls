package net.driftingsouls.ds2.server.framework.authentication;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashes and verifies the passwords stored in {@code users.passwort}.
 *
 * <p>New hashes are always bcrypt. Verification also accepts the legacy scheme - unsalted MD5 with a
 * lossy hex encoding - so that existing accounts can still log in and be rehashed (see ADR-0007). The
 * scheme is told apart by the stored value: bcrypt hashes start with {@code $2}, legacy hashes are
 * lowercase hex and never contain {@code $}.
 */
public class PasswordHasher {
    // Static for the same reason as PasswordGenerator's SecureRandom: instances are created per request.
    // BCryptPasswordEncoder is thread-safe.
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    /**
     * Hashes a password for storage.
     *
     * @param password the password in cleartext
     * @return the bcrypt hash
     */
    public String hash(String password) {
        return BCRYPT.encode(password);
    }

    /**
     * Checks a password against a stored hash of either scheme.
     *
     * @param password the password in cleartext
     * @param storedHash the value of {@code users.passwort}
     * @return true if the password matches
     */
    public boolean matches(String password, String storedHash) {
        if (storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        if (isBcrypt(storedHash)) {
            return BCRYPT.matches(password, storedHash);
        }
        return MessageDigest.isEqual(
            legacyHash(password).getBytes(StandardCharsets.US_ASCII),
            storedHash.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Tells whether a stored hash should be replaced by {@link #hash(String)} after a successful
     * {@link #matches(String, String)}.
     *
     * @param storedHash the value of {@code users.passwort}
     * @return true if the hash uses the legacy scheme
     */
    public boolean needsRehash(String storedHash) {
        return !isBcrypt(storedHash);
    }

    private static boolean isBcrypt(String storedHash) {
        return storedHash.startsWith("$2");
    }

    /**
     * Reproduces the legacy password hash exactly as {@code Common.md5} computed it: MD5 over UTF-8,
     * hex-encoded without zero-padding, so bytes below 0x10 become a single character. This is a
     * deliberate copy rather than a call, so that a later fix to {@code Common.md5} cannot lock out
     * accounts that still carry a legacy hash. Delete it together with the legacy path.
     */
    private static String legacyHash(String password) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(password.getBytes(StandardCharsets.UTF_8));
            var hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(Integer.toHexString(0xFF & b));
            }
            return hex.toString();
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is not available", e);
        }
    }
}
