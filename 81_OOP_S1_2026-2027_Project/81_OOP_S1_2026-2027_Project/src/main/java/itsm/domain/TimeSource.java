package itsm.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * The one clock every part of the system reads. Normally the real time; the demo clock moves it
 * forward so that report times, pick-up times, fix times and deadlines all agree.
 */
public final class TimeSource {

    private static volatile Clock clock = Clock.systemDefaultZone();

    private TimeSource() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** Real time plus the given number of minutes. */
    public static void offsetMinutes(long minutes) {
        clock = Clock.offset(Clock.systemDefaultZone(), Duration.ofMinutes(minutes));
    }
}
