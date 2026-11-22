package itsm.domain;

/** IT manager: may make any lifecycle move and change system configuration. */
public class Administrator extends User {

    private static final long serialVersionUID = 1L;

    public Administrator(String userId, String name, String email) {
        super(userId, name, email);
    }

    @Override
    public String role() {
        return "Administrator";
    }

    @Override
    public boolean canMoveTo(Status next) {
        return true;
    }

    @Override
    public boolean canConfigure() {
        return true;
    }
}
