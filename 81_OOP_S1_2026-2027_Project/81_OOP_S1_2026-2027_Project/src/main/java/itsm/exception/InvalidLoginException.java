package itsm.exception;

/** Thrown when a username/password pair does not match, or the account is disabled. */
public class InvalidLoginException extends ITSMException {

    public InvalidLoginException(String message) {
        super(message);
    }
}
