package itsm.domain;

import itsm.exception.SlaConfigurationException;
import itsm.exception.ValidationException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * Service Level Agreement: the response and resolution time allowed for each priority.
 * The policy table is shared by every incident and can be changed by an administrator.
 */
public class SLA {

    private static final Map<Priority, Integer> RESPONSE_MINS = new EnumMap<>(Priority.class);
    private static final Map<Priority, Integer> RESOLUTION_MINS = new EnumMap<>(Priority.class);

    static {
        resetToDefaults();
    }

    private final int responseMins;
    private final int resolutionMins;

    /** The current policy's targets for a priority. */
    public SLA(Priority priority) {
        synchronized (SLA.class) {
            Integer resolution = RESOLUTION_MINS.get(priority);
            Integer response = RESPONSE_MINS.get(priority);
            if (resolution == null || response == null) {
                throw new SlaConfigurationException("No SLA is configured for priority " + priority);
            }
            this.resolutionMins = resolution;
            this.responseMins = response;
        }
    }

    /** Fixed targets, as stored on an incident when its priority was set. */
    public SLA(int responseMins, int resolutionMins) {
        if (responseMins < 1 || resolutionMins < 1) {
            throw new SlaConfigurationException("SLA targets must be positive");
        }
        this.responseMins = responseMins;
        this.resolutionMins = resolutionMins;
    }

    // ---- policy configuration (administrator) ----

    /** Defaults: response target from the brief's table; resolution targets as used by the SLA monitor. */
    public static synchronized void resetToDefaults() {
        set(Priority.CRITICAL, 5, 60);
        set(Priority.HIGH, 15, 240);
        set(Priority.MEDIUM, 60, 480);
        set(Priority.LOW, 240, 1440);
    }

    /** Changes the targets for one priority. The response target must come before the resolution target. */
    public static synchronized void configure(Priority priority, int responseMins, int resolutionMins)
            throws ValidationException {
        if (priority == null) {
            throw new ValidationException("Choose a priority");
        }
        if (responseMins < 1 || resolutionMins < 1 || resolutionMins > 60 * 24 * 30) {
            throw new ValidationException("SLA times must be between 1 minute and 30 days");
        }
        if (responseMins >= resolutionMins) {
            throw new ValidationException("The response target must be shorter than the resolution target");
        }
        set(priority, responseMins, resolutionMins);
    }

    /** A copy of the current policy: priority to {response, resolution} minutes. */
    public static synchronized Map<Priority, int[]> policy() {
        Map<Priority, int[]> copy = new EnumMap<>(Priority.class);
        for (Priority p : Priority.values()) {
            copy.put(p, new int[] {RESPONSE_MINS.get(p), RESOLUTION_MINS.get(p)});
        }
        return copy;
    }

    /** Restores a saved policy. */
    public static synchronized void restore(Map<Priority, int[]> saved) {
        if (saved == null) {
            return;
        }
        for (Map.Entry<Priority, int[]> e : saved.entrySet()) {
            int[] v = e.getValue();
            if (e.getKey() == null || v == null || v.length != 2 || v[0] < 1 || v[1] <= v[0]) {
                throw new SlaConfigurationException("Saved SLA policy is invalid for " + e.getKey());
            }
            set(e.getKey(), v[0], v[1]);
        }
    }

    private static void set(Priority p, int response, int resolution) {
        RESPONSE_MINS.put(p, response);
        RESOLUTION_MINS.put(p, resolution);
    }

    // ---- per-incident checks ----

    public int getResolutionMins() {
        return resolutionMins;
    }

    public int getResponseMins() {
        return responseMins;
    }

    public LocalDateTime deadlineFrom(LocalDateTime start) {
        return start.plusMinutes(resolutionMins);
    }

    public boolean isBreached(LocalDateTime start, LocalDateTime resolvedAt) {
        LocalDateTime end = (resolvedAt != null) ? resolvedAt : TimeSource.now();
        return end.isAfter(deadlineFrom(start));
    }

    /** True when the incident was (or still is) not picked up within the response target. */
    public boolean responseMissed(LocalDateTime start, LocalDateTime acknowledgedAt, LocalDateTime now) {
        LocalDateTime end = (acknowledgedAt != null) ? acknowledgedAt : now;
        return Duration.between(start, end).toMinutes() > responseMins;
    }

    /** Share of the SLA window used so far: 0.5 means half the allowed time has passed. */
    public double elapsedFraction(LocalDateTime start, LocalDateTime now) {
        long mins = Duration.between(start, now).toMinutes();
        return (double) mins / resolutionMins;
    }
}
