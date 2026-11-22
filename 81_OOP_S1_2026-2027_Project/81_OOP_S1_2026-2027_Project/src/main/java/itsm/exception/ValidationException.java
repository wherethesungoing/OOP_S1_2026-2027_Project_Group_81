package itsm.exception;

/** Thrown when user input breaks a rule: blank text, out-of-range numbers, a taken username, and so on. */
public class ValidationException extends ITSMException {

    public ValidationException(String message) {
        super(message);
    }
}
