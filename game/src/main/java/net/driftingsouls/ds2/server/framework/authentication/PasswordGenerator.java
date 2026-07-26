package net.driftingsouls.ds2.server.framework.authentication;

import java.security.SecureRandom;

/**
 * Generates the passwords that are issued to users on registration and on password reset.
 */
public class PasswordGenerator {
    /**
     * Alphabet for generated passwords. Digits and letters that are easily confused with one another
     * ({@code 0/O}, {@code 1/l/I}) are left out - these passwords are sent by mail and typed by hand.
     */
    private static final String ALPHABET = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /**
     * Length of a generated password. 16 characters out of the alphabet above are roughly 93 bits of
     * entropy.
     */
    private static final int LENGTH = 16;

    // Static because DSApplication builds a controller - and with it a generator - per request, and
    // seeding a SecureRandom each time would be wasted work. SecureRandom is thread-safe.
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a new random password.
     *
     * @return the password in cleartext, to be hashed before storing and sent to the user by mail
     */
    public String generate() {
        var password = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            // nextInt(bound) is rejection-sampled and therefore free of the modulo bias that
            // nextInt() % ALPHABET.length() would introduce.
            password.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }

        return password.toString();
    }
}
