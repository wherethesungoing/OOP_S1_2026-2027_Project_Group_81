package itsm.domain;

import java.io.Serializable;
import java.util.Objects;

/** Abstract base for every user role (inheritance: each role IS-A User). */
public abstract class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String name;
    private final String email;
    private boolean active = true;

    protected User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    /** A disabled account cannot sign in, and a disabled technician gets no new tickets. */
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** Each subclass must declare its role (polymorphism). */
    public abstract String role();

    /** Authorization: which lifecycle moves this role may make. Each role overrides it. */
    public boolean canMoveTo(Status next) {
        return false;
    }

    /** Authorization: whether this role may change configuration such as the assignment strategy. */
    public boolean canConfigure() {
        return false;
    }

    /** Two User objects are the same person when their ids match (needed after loading from disk). */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User)) {
            return false;
        }
        return userId.equals(((User) other).userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return role() + "(" + userId + ", " + name + ")";
    }
}
