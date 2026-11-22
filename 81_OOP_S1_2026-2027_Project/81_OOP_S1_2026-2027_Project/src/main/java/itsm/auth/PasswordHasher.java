package itsm.auth;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password protection: passwords are never stored, only a salted PBKDF2-HMAC-SHA256 hash.
 * PBKDF2 ships with the JDK, so no external library is needed.
 */
public final class PasswordHasher {

    private static final int ITERATIONS = 60_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /** Returns "salt:hash", both Base64. A fresh random salt each time. */
    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] key = derive(password.toCharArray(), salt);
        Base64.Encoder b64 = Base64.getEncoder();
        return b64.encodeToString(salt) + ":" + b64.encodeToString(key);
    }

    /** Checks a password against a stored "salt:hash", in constant time. */
    public static boolean matches(String password, String stored) {
        if (password == null || stored == null || !stored.contains(":")) {
            return false;
        }
        String[] parts = stored.split(":", 2);
        Base64.Decoder b64 = Base64.getDecoder();
        byte[] salt = b64.decode(parts[0]);
        byte[] expected = b64.decode(parts[1]);
        return MessageDigest.isEqual(expected, derive(password.toCharArray(), salt));
    }

    private static byte[] derive(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not available in this JDK", e);
        }
    }
}
