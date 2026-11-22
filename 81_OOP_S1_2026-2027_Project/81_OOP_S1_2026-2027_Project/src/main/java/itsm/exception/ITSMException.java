package itsm.exception;

/** Base checked exception for all I-ITSM errors. */
public class ITSMException extends Exception {

    public ITSMException(String message) {
        super(message);
    }
}
