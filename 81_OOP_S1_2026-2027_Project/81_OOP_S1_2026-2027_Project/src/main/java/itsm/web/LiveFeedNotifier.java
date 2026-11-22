package itsm.web;

import itsm.domain.Incident;
import itsm.notify.Notifier;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Observer that turns system events into the plain-language activity feed shown in the app. */
public class LiveFeedNotifier implements Notifier {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_EVENTS = 40;

    private final Deque<String> recent = new ArrayDeque<>();
    private final SimClock clock;

    public LiveFeedNotifier(SimClock clock) {
        this.clock = clock;
    }

    @Override
    public synchronized void update(String event, Incident incident) {
        recent.addFirst(clock.now().format(TIME) + "  " + describe(event, incident));
        while (recent.size() > MAX_EVENTS) {
            recent.removeLast();
        }
    }

    public synchronized List<String> recent() {
        return new ArrayList<>(recent);
    }

    private static String describe(String event, Incident inc) {
        String id = inc.getId();
        String team = inc.getAssignee() == null ? "the support team" : inc.getAssignee().getName();
        switch (event) {
            case "REPORTED":
                return "New ticket " + id + ": " + inc.getTitle();
            case "ASSIGNED":
                return id + " sent to " + team;
            case "REASSIGNED":
                return id + " moved to " + team;
            case "PRIORITY_CHANGED":
                return "Priority of " + id + " changed to " + inc.getPriority();
            case "RECLASSIFIED":
                return id + " moved to a different service";
            case "IN_PROGRESS":
                return team + " picked up " + id;
            case "NOTE_ADDED":
                return team + " added a note to " + id;
            case "RESOLVED":
                return team + " marked " + id + " as fixed";
            case "REPORTER_NOTIFIED":
                return "Student asked to confirm the fix for " + id;
            case "INFO_REQUESTED":
                return team + " asked the student for more information on " + id;
            case "INFO_PROVIDED":
                return "Student replied on " + id;
            case "COMMENT_ADDED":
                return "Student added information to " + id;
            case "FEEDBACK":
                return "Student rated the fix for " + id + ": " + inc.getFeedbackRating() + "/5";
            case "CLOSED":
                return id + " closed";
            case "REOPENED":
                return id + " reopened: still not working";
            case "CORRELATED":
                return id + " linked to " + inc.getRelatedIds().size() + " similar ticket(s)";
            case "ESCALATED":
                return id + " escalated to the CCIT admin";
            case "SLA_AT_RISK":
                return id + " is running late (75% of its time used)";
            case "SLA_BREACHED":
                return id + " missed its deadline";
            default:
                return event + " " + id;
        }
    }
}
