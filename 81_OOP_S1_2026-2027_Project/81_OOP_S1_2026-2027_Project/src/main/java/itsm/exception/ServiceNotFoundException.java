package itsm.exception;

/** Thrown when an incident names an IT service that is not in the service catalog. */
public class ServiceNotFoundException extends ITSMException {

    public ServiceNotFoundException(String key) {
        super("Service not found: " + key);
    }
}
