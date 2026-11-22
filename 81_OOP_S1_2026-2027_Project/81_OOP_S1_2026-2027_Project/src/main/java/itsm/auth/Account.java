package itsm.auth;

import itsm.domain.User;
import java.io.Serializable;

/** Login details for one user: a username and the hash of their password (never the password itself). */
public final class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final User user;
    private final String passwordHash;

    Account(String username, User user, String passwordHash) {
        this.username = username;
        this.user = user;
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public User getUser() {
        return user;
    }

    boolean checkPassword(String password) {
        return PasswordHasher.matches(password, passwordHash);
    }
}
