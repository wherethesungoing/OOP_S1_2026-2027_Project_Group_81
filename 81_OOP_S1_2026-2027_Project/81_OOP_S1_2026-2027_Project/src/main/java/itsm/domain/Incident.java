package itsm.domain;

import itsm.exception.InvalidStateTransitionException;
import itsm.exception.ValidationException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The central domain object, with enforced lifecycle transitions. */
public class Incident implements Serializable {

    private static final long serialVersionUID = 1L;
    private static int counter = 0;

    // Legal lifecycle transitions: the single source of truth

    private static final Map<Status, Set<Status>> ALLOWED = new EnumMap<>(Status.class);
    static {
        ALLOWED.put(Status.NEW, EnumSet.of(Status.ASSIGNED));
        ALLOWED.put(Status.ASSIGNED, EnumSet.of(Status.IN_PROGRESS));
        ALLOWED.put(Status.IN_PROGRESS, EnumSet.of(Status.RESOLVED));
        ALLOWED.put(Status.RESOLVED, EnumSet.of(Status.CLOSED, Status.REOPENED));
        ALLOWED.put(Status.REOPENED, EnumSet.of(Status.IN_PROGRESS));
        ALLOWED.put(Status.CLOSED, EnumSet.noneOf(Status.class));
    }

    private final String id;
    private final String title;
    private final String description;
    private Category category;
    private String serviceKey;            // e.g. "wifi", "lms"
    private final int urgency;            // 1..3
    private final int affectedUsers;
    private final LocalDateTime createdAt;
    private final List<AuditEntry> audit = new ArrayList<>();   // composition
    private final List<String> relatedIds = new ArrayList<>();  // correlated duplicates

    private Status status = Status.NEW;
    private Priority priority;
    private int slaResponseMins;          // SLA targets fixed when the priority was set,
    private int slaResolutionMins;        // so later policy changes do not move this ticket's deadlines
    private Technician assignee;
    private User reporter;
    private String location = "";
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private String resolution = "";
    private String pendingQuestion = "";  // technician asked the reporter for more information
    private SlaState slaState = SlaState.ON_TRACK;
    private boolean escalated;
    private String escalationReason;
    private int feedbackRating;           // 0 = no feedback yet, else 1..5
    private String feedbackComment = "";

    /** Overloaded constructor: sensible defaults. */
    public Incident(String title, String description, Category category) {
        this(title, description, category, "other", 1, 1);
    }

    public Incident(String title, String description, Category category,
            String serviceKey, int urgency, int affectedUsers) {
        this.id = nextId();
        this.title = title;
        this.description = description;
        this.category = category;
        this.serviceKey = serviceKey;
        this.urgency = urgency;
        this.affectedUsers = affectedUsers;
        this.createdAt = TimeSource.now();
        audit.add(new AuditEntry("system", "Incident created", null, Status.NEW, AuditEntry.Kind.CREATED, "raised the ticket"));
    }

    private static synchronized String nextId() {
        counter++;
        return String.format("INC-%04d", counter);
    }

    /** After loading saved incidents, make sure new ids continue after the highest saved one. */
    public static synchronized void ensureCounterAbove(String savedId) {
        try {
            int n = Integer.parseInt(savedId.substring(savedId.indexOf('-') + 1));
            counter = Math.max(counter, n);
        } catch (RuntimeException ignored) {
            // not one of our ids; nothing to do
        }
    }

    // ---- getters ----

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public int getUrgency() {
        return urgency;
    }

    public int getAffectedUsers() {
        return affectedUsers;
    }

    public Status getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public Technician getAssignee() {
        return assignee;
    }

    public User getReporter() {
        return reporter;
    }

    public String getLocation() {
        return location;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public String getResolution() {
        return resolution;
    }

    public String getPendingQuestion() {
        return pendingQuestion;
    }

    public boolean isWaitingForReporter() {
        return !pendingQuestion.isEmpty();
    }

    public SlaState getSlaState() {
        return slaState;
    }

    public boolean isEscalated() {
        return escalated;
    }

    public String getEscalationReason() {
        return escalationReason;
    }

    public int getFeedbackRating() {
        return feedbackRating;
    }

    public String getFeedbackComment() {
        return feedbackComment;
    }

    public boolean hasFeedback() {
        return feedbackRating > 0;
    }

    public List<AuditEntry> getAudit() {
        return Collections.unmodifiableList(audit);
    }

    public List<String> getRelatedIds() {
        return Collections.unmodifiableList(relatedIds);
    }

    /** Open means still being worked on (not yet resolved or closed). */
    public boolean isOpen() {
        return status != Status.RESOLVED && status != Status.CLOSED;
    }

    /** A technician has picked it up once it reaches IN_PROGRESS. */
    public boolean isAcknowledged() {
        return status != Status.NEW && status != Status.ASSIGNED;
    }

    // ---- changes ----

    /** The SLA this ticket is measured against: the targets in force when its priority was set. */
    public SLA getSla() {
        if (slaResponseMins > 0 && slaResolutionMins > 0) {
            return new SLA(slaResponseMins, slaResolutionMins);
        }
        return new SLA(priority);
    }

    private void fixSla() {
        SLA sla = new SLA(priority);
        slaResponseMins = sla.getResponseMins();
        slaResolutionMins = sla.getResolutionMins();
    }

    /** Staff change the priority. The deadline is recalculated from the report time with the new priority's targets. */
    public void changePriority(String actor, Priority p, String reason) {
        Priority old = priority;
        priority = p;
        fixSla();
        audit.add(new AuditEntry(actor, "Priority " + old + " -> " + p + ": " + reason, AuditEntry.Kind.CHANGE,
                "changed the priority from " + pretty(old) + " to " + pretty(p) + ". Reason: " + reason));
    }

    /** Staff move the ticket to a different service (and so possibly a different category). */
    public void reclassify(String actor, String newServiceKey, String serviceName, Category newCategory) {
        serviceKey = newServiceKey;
        category = newCategory;
        audit.add(new AuditEntry(actor, "Reclassified as " + newServiceKey + " / " + newCategory.label(),
                AuditEntry.Kind.CHANGE, "moved the ticket to " + serviceName + " (" + newCategory.label() + ")"));
    }

    /** Moved to another technician. */
    public void reassign(String actor, Technician to) {
        String from = assignee == null ? "nobody" : assignee.getName();
        assignee = to;
        audit.add(new AuditEntry(actor, "Reassigned from " + from + " to " + to.getName(), AuditEntry.Kind.ASSIGNED,
                "Moved from " + from + " to " + to.getName() + "."));
    }

    private static String pretty(Priority p) {
        return p == null ? "none" : p.name().charAt(0) + p.name().substring(1).toLowerCase(java.util.Locale.ROOT);
    }

    public void setPriority(Priority p) {
        this.priority = p;
        fixSla();
        String name = p.name().charAt(0) + p.name().substring(1).toLowerCase(java.util.Locale.ROOT);
        audit.add(new AuditEntry("system", "Priority set to " + p, AuditEntry.Kind.SYSTEM, "Priority set to " + name + "."));
    }

    public void setAssignee(Technician technician) {
        this.assignee = technician;
        if (technician != null) {
            audit.add(new AuditEntry("system", "Assigned to " + technician.getName(), AuditEntry.Kind.ASSIGNED,
                    "Sent to " + technician.getName() + "."));
        }
    }

    /** Re-points the assignee at the live Technician object after loading, without an audit entry. */
    public void relinkAssignee(Technician technician) {
        this.assignee = technician;
    }

    public void setReporter(User reporter) {
        this.reporter = reporter;
    }

    public void setLocation(String location) {
        this.location = (location == null) ? "" : location.trim();
    }

    /** Investigation note from a technician; kept in the audit trail. */
    public void addNote(String actor, String note) {
        audit.add(new AuditEntry(actor, "Note: " + note, AuditEntry.Kind.NOTE, note));
    }

    /** Extra information from the reporter. */
    public void addComment(String actor, String text) {
        audit.add(new AuditEntry(actor, "Info from reporter: " + text, AuditEntry.Kind.REPORTER_MESSAGE, text));
    }

    /** A technician asks the reporter a question; the ticket shows it until the reporter answers. */
    public void requestInfo(String actor, String question) {
        pendingQuestion = question;
        audit.add(new AuditEntry(actor, "Asked the reporter: " + question, AuditEntry.Kind.STAFF_MESSAGE, question));
    }

    /** The reporter answers the pending question. */
    public void answerInfo(String actor, String answer) {
        audit.add(new AuditEntry(actor, "Reporter replied: " + answer, AuditEntry.Kind.REPORTER_MESSAGE, answer));
        pendingQuestion = "";
    }

    /** What was done to fix it; shown to the reporter. */
    public void setResolution(String actor, String text) {
        resolution = text;
        audit.add(new AuditEntry(actor, "Resolution: " + text, AuditEntry.Kind.RESOLUTION, text));
    }

    /** The reporter rates the fix once the ticket is closed. */
    public void giveFeedback(String actor, int rating, String comment) throws ValidationException {
        if (status != Status.CLOSED) {
            throw new ValidationException("Feedback can only be given once the ticket is closed");
        }
        if (hasFeedback()) {
            throw new ValidationException("Feedback was already given for " + id);
        }
        if (rating < 1 || rating > 5) {
            throw new ValidationException("Rating must be between 1 and 5");
        }
        feedbackRating = rating;
        feedbackComment = (comment == null) ? "" : comment.trim();
        String summary = rating + "/5" + (feedbackComment.isEmpty() ? "" : " \"" + feedbackComment + "\"");
        audit.add(new AuditEntry(actor, "Feedback: " + summary, AuditEntry.Kind.FEEDBACK, "Rated the fix " + summary));
    }

    /** Link this incident to a likely duplicate. */
    public void addRelated(String otherId) {
        if (!otherId.equals(id) && !relatedIds.contains(otherId)) {
            relatedIds.add(otherId);
            audit.add(new AuditEntry("system", "Correlated with " + otherId, AuditEntry.Kind.LINK,
                    "Linked to " + otherId + ", which looks like the same problem."));
        }
    }

    public void setSlaState(SlaState next) {
        if (next != slaState) {
            String words = next == SlaState.BREACHED ? "The resolution deadline was missed."
                    : next == SlaState.AT_RISK ? "Running late: 75% of the time allowed has been used." : "Back on track.";
            audit.add(new AuditEntry("sla-monitor", "SLA " + slaState + " -> " + next, AuditEntry.Kind.SLA, words));
            slaState = next;
        }
    }

    /** Escalate once. Returns true only when this call escalated it, so alerts are not repeated. */
    public boolean escalate(String reason) {
        if (escalated) {
            return false;
        }
        escalated = true;
        escalationReason = reason;
        audit.add(new AuditEntry("system", "Escalated: " + reason, AuditEntry.Kind.ESCALATION,
                "Escalated to the CCIT admin. " + reason + "."));
        return true;
    }

    /** Overloaded: actor defaults to "system". */
    public void transitionTo(Status next) throws InvalidStateTransitionException {
        transitionTo(next, "system");
    }

    public void transitionTo(Status next, String actor) throws InvalidStateTransitionException {
        if (!ALLOWED.get(status).contains(next)) {
            throw new InvalidStateTransitionException(
                    id + ": cannot go " + status.label() + " -> " + next.label());
        }
        Status old = status;
        status = next;
        if (next == Status.IN_PROGRESS && acknowledgedAt == null) {
            acknowledgedAt = TimeSource.now();
        }
        if (next == Status.RESOLVED) {
            resolvedAt = TimeSource.now();
        }
        if (next == Status.REOPENED) {
            resolvedAt = null;
        }
        audit.add(new AuditEntry(actor, old.label() + " -> " + next.label(), old, next, AuditEntry.Kind.STATUS, describe(next)));
    }

    /** How a lifecycle move reads in the conversation, after the person's name. */
    private static String describe(Status next) {
        switch (next) {
            case ASSIGNED:
                return "assigned the ticket";
            case IN_PROGRESS:
                return "picked up the ticket";
            case RESOLVED:
                return "marked the ticket as fixed";
            case CLOSED:
                return "closed the ticket";
            case REOPENED:
                return "reopened the ticket";
            default:
                return "moved the ticket to " + next.label();
        }
    }

    @Override
    public String toString() {
        String p = (priority == null) ? "-" : priority.name();
        String who = (assignee == null) ? "-" : assignee.getUserId();
        return "<" + id + " " + status.label() + " P=" + p + " assignee=" + who + " '" + title + "'>";
    }
}
