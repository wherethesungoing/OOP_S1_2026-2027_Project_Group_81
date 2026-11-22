package itsm.web;

import itsm.domain.TimeSource;
import java.time.LocalDateTime;

/** Real time plus an offset, so a demo can skip ahead to show SLA alerts and escalation. */
public class SimClock {

    private long offsetMinutes;

    public synchronized LocalDateTime now() {
        return TimeSource.now();
    }

    /** Moves the shared clock forward, so new tickets, pick-ups and fixes all use the demo time. */
    public synchronized void advance(long minutes) {
        set(offsetMinutes + minutes);
    }

    /** Sets the offset directly (used when restoring a saved demo clock). */
    public synchronized void set(long minutes) {
        offsetMinutes = minutes;
        TimeSource.offsetMinutes(minutes);
    }

    public synchronized long offset() {
        return offsetMinutes;
    }
}
