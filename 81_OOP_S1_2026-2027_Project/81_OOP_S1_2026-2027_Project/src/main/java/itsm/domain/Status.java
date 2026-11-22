package itsm.domain;

public enum Status {

    NEW("New"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    CLOSED("Closed"),
    REOPENED("Reopened");

    private final String label;

    Status(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
