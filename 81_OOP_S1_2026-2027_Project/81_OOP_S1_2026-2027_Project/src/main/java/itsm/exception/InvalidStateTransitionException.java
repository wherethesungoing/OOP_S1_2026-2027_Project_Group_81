package itsm.exception;

/** Thrown when an incident is moved through an illegal lifecycle edge. */
public class InvalidStateTransitionException extends ITSMException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
