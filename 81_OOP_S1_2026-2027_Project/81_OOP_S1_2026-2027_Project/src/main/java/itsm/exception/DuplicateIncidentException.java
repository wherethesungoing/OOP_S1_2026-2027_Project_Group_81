package itsm.exception;

/** Thrown when someone reports a problem they already have an open ticket for. */
public class DuplicateIncidentException extends ITSMException {

    private final String existingId;

    public DuplicateIncidentException(String existingId) {
        super("You already reported this problem as " + existingId
                + ". Add any new details to that ticket instead of raising a new one.");
        this.existingId = existingId;
    }

    public String getExistingId() {
        return existingId;
    }
}
