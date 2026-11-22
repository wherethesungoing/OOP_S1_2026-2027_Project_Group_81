package itsm.exception;

/** Thrown when an incident id is not known to the manager. */
public class IncidentNotFoundException extends ITSMException {

    public IncidentNotFoundException(String id) {
        super("Incident not found: " + id);
    }
}
