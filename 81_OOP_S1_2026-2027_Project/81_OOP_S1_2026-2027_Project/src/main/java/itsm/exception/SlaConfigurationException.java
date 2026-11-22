package itsm.exception;

/**
 * Thrown when no SLA is configured for a priority. Unchecked, because it means the system is
 * misconfigured rather than that a user did something wrong.
 */
public class SlaConfigurationException extends IllegalStateException {

    public SlaConfigurationException(String message) {
        super(message);
    }
}
