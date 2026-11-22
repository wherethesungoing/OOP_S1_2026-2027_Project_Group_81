package itsm.exception;

/** Thrown when a user's role is not allowed to perform an action (authorization). */
public class UnauthorizedException extends ITSMException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
