package itsm.auth;

import itsm.exception.InvalidLoginException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Slows down password guessing: after too many wrong passwords for a username, that username
 * cannot sign in for a short time.
 */
public class LoginThrottle {

    public static final int MAX_FAILURES = 5;
    public static final long LOCK_MILLIS = 60_000;

    private final Map<String, int[]> failures = new HashMap<>();
    private final Map<String, Long> lockedUntil = new HashMap<>();
    private final LongSupplier clock;

    public LoginThrottle() {
        this(System::currentTimeMillis);
    }

    public LoginThrottle(LongSupplier clock) {
        this.clock = clock;
    }

    /** Call before checking the password. */
    public synchronized void check(String username) throws InvalidLoginException {
        Long until = lockedUntil.get(key(username));
        if (until != null) {
            long left = until - clock.getAsLong();
            if (left > 0) {
                throw new InvalidLoginException("Too many wrong passwords. Try again in " + ((left + 999) / 1000) + " seconds.");
            }
            lockedUntil.remove(key(username));
        }
    }

    public synchronized void failed(String username) {
        int[] count = failures.computeIfAbsent(key(username), k -> new int[1]);
        count[0]++;
        if (count[0] >= MAX_FAILURES) {
            lockedUntil.put(key(username), clock.getAsLong() + LOCK_MILLIS);
            count[0] = 0;
        }
    }

    public synchronized void succeeded(String username) {
        failures.remove(key(username));
        lockedUntil.remove(key(username));
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
