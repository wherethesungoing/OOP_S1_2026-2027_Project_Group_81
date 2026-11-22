package itsm.exception;

/**
 * Thrown when the persistent store cannot be read or written (the "database failure" case).
 * Unchecked, because a storage failure is not something the caller can fix at that point;
 * the web layer catches it and tells the user the change could not be saved.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
