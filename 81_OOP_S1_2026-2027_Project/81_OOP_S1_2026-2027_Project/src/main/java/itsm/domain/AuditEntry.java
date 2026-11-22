package itsm.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One immutable record of who did what, and when. Lifecycle moves also record the previous
 * and new state; the incident ID is the incident that owns the entry.
 *
 * Each entry has a kind, so screens can show the conversation between the reporter and
 * CCIT separately from internal notes and system events.
 */
public final class AuditEntry implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** What sort of entry this is. */
    public enum Kind {
        CREATED,           // the ticket was raised
        ASSIGNED,          // sent (or moved) to a technician
        CHANGE,            // priority or service changed by staff
        STATUS,            // a lifecycle move
        STAFF_MESSAGE,     // CCIT wrote to the reporter
        REPORTER_MESSAGE,  // the reporter wrote to CCIT
        RESOLUTION,        // what was done to fix it
        FEEDBACK,          // the reporter's rating
        NOTE,              // internal investigation note, staff only
        SLA,               // deadline warnings
        ESCALATION,
        LINK,              // linked to a ticket about the same problem
        SYSTEM;            // anything else, such as the priority being set

        /** Entries the reporter sees on their ticket. Notes, deadline checks and internal links stay with staff. */
        public boolean visibleToReporter() {
            return this != NOTE && this != SLA && this != LINK && this != SYSTEM;
        }

        /** Entries written by a person to another person. */
        public boolean isMessage() {
            return this == STAFF_MESSAGE || this == REPORTER_MESSAGE || this == RESOLUTION;
        }
    }

    private final String actor;
    private final String action;
    private final Status fromState;   // null when the entry is not a state change
    private final Status toState;
    private final Kind kind;
    private final String text;        // what to show in the conversation
    private final LocalDateTime timestamp;

    public AuditEntry(String actor, String action) {
        this(actor, action, null, null, Kind.SYSTEM, action);
    }

    public AuditEntry(String actor, String action, Status fromState, Status toState) {
        this(actor, action, fromState, toState, fromState == null ? Kind.CREATED : Kind.STATUS, action);
    }

    public AuditEntry(String actor, String action, Kind kind, String text) {
        this(actor, action, null, null, kind, text);
    }

    public AuditEntry(String actor, String action, Status fromState, Status toState, Kind kind, String text) {
        this.actor = actor;
        this.action = action;
        this.fromState = fromState;
        this.toState = toState;
        this.kind = kind;
        this.text = text;
        this.timestamp = TimeSource.now();
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public Status getFromState() {
        return fromState;
    }

    public Status getToState() {
        return toState;
    }

    public Kind getKind() {
        return kind == null ? Kind.SYSTEM : kind;
    }

    /** The wording for the conversation view (the action text when no separate wording was given). */
    public String getText() {
        return text == null ? action : text;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[" + timestamp.format(FMT) + "] " + actor + ": " + action;
    }
}
