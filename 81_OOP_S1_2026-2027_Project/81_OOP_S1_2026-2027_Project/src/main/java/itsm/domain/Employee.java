package itsm.domain;

/** Reports incidents, then verifies the fix: may close or reopen a resolved incident. */
public class Employee extends User {

    private static final long serialVersionUID = 1L;

    public Employee(String userId, String name, String email) {
        super(userId, name, email);
    }

    @Override
    public String role() {
        return "Employee";
    }

    @Override
    public boolean canMoveTo(Status next) {
        return next == Status.CLOSED || next == Status.REOPENED;
    }
}
