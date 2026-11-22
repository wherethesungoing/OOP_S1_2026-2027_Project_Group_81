package itsm.exception;

/** Thrown when an assignment strategy has no technician to choose. */
public class NoAvailableTechnicianException extends ITSMException {

    public NoAvailableTechnicianException(String message) {
        super(message);
    }
}
