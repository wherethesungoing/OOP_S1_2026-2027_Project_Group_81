package itsm.web;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Signed-in browser sessions. Each login gets a long random token, sent back as an
 * HttpOnly cookie, so the browser never holds the password or a guessable id.
 */
public class SessionStore {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, String> userIdByToken = new ConcurrentHashMap<>();

    public String create(String userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        userIdByToken.put(token, userId);
        return token;
    }

    /** The user id for a token, or null when the token is unknown. */
    public String userId(String token) {
        return token == null ? null : userIdByToken.get(token);
    }

    public void end(String token) {
        if (token != null) {
            userIdByToken.remove(token);
        }
    }
}
