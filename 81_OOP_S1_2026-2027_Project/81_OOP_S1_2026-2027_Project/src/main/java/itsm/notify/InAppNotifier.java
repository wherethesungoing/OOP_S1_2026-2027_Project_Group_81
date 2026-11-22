package itsm.notify;

import itsm.domain.Incident;
import itsm.domain.User;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * In-app notifications: each event goes to the people it concerns, into their own inbox.
 * The reporter hears about progress on their ticket, the assigned technician about new work
 * and replies, and administrators about escalations and missed deadlines.
 */
public class InAppNotifier implements Notifier {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_PER_USER = 30;

    private final Map<String, Deque<String>> inboxes = new HashMap<>();
    private final Supplier<List<User>> administrators;
    private final Supplier<LocalDateTime> clock;

    public InAppNotifier(Supplier<List<User>> administrators, Supplier<LocalDateTime> clock) {
        this.administrators = administrators;
        this.clock = clock;
    }

    @Override
    public synchronized void update(String event, Incident inc) {
        User reporter = inc.getReporter();
        User tech = inc.getAssignee();
        String id = inc.getId();
        switch (event) {
            case "REPORTED":
                send(reporter, "We received " + id + " (" + inc.getTitle() + "). Priority: " + priority(inc) + ".");
                break;
            case "ASSIGNED":
                send(tech, "New ticket for you: " + id + " " + inc.getTitle() + " (" + priority(inc) + " priority).");
                send(reporter, id + " has been sent to " + name(tech) + ".");
                break;
            case "REASSIGNED":
                send(tech, "Ticket moved to you: " + id + " " + inc.getTitle() + " (" + priority(inc) + " priority).");
                send(reporter, id + " is now handled by " + name(tech) + ".");
                break;
            case "PRIORITY_CHANGED":
                send(reporter, "The priority of " + id + " was changed to " + priority(inc) + ".");
                send(tech, "The priority of " + id + " was changed to " + priority(inc) + ".");
                break;
            case "RECLASSIFIED":
                send(tech, id + " was moved to a different service.");
                break;
            case "IN_PROGRESS":
                send(reporter, name(tech) + " is working on " + id + ".");
                break;
            case "INFO_REQUESTED":
                send(reporter, name(tech) + " needs more information on " + id + ": " + inc.getPendingQuestion());
                break;
            case "INFO_PROVIDED":
            case "COMMENT_ADDED":
                send(tech, "The reporter added information to " + id + ".");
                break;
            case "REPORTER_NOTIFIED":
                send(reporter, id + " has been fixed: " + inc.getResolution() + " Please confirm it works.");
                break;
            case "REOPENED":
                send(tech, id + " was reopened: the reporter says it is still not working.");
                break;
            case "CLOSED":
                send(tech, id + " was confirmed fixed and closed.");
                send(reporter, id + " is closed. You can rate the fix.");
                break;
            case "FEEDBACK":
                send(tech, "Feedback on " + id + ": " + inc.getFeedbackRating() + "/5.");
                break;
            case "SLA_AT_RISK":
                send(tech, id + " is running late: 75% of its time is used.");
                break;
            case "SLA_BREACHED":
                send(tech, id + " has missed its deadline.");
                sendAll(administrators.get(), id + " has missed its deadline.");
                break;
            case "ESCALATED":
                sendAll(administrators.get(), id + " escalated: " + inc.getEscalationReason());
                break;
            default:
                break;
        }
    }

    private void sendAll(List<User> users, String message) {
        Set<User> unique = new LinkedHashSet<>(users);
        for (User u : unique) {
            send(u, message);
        }
    }

    private void send(User user, String message) {
        if (user == null) {
            return;
        }
        Deque<String> box = inboxes.computeIfAbsent(user.getUserId(), k -> new ArrayDeque<>());
        box.addFirst(clock.get().format(TIME) + "  " + message);
        while (box.size() > MAX_PER_USER) {
            box.removeLast();
        }
    }

    private static String priority(Incident inc) {
        if (inc.getPriority() == null) {
            return "not set";
        }
        String p = inc.getPriority().name();
        return p.charAt(0) + p.substring(1).toLowerCase(java.util.Locale.ROOT);
    }

    private static String name(User u) {
        return u == null ? "Support" : u.getName();
    }

    /** A copy of every inbox, for saving. */
    public synchronized Map<String, List<String>> snapshot() {
        Map<String, List<String>> copy = new HashMap<>();
        for (Map.Entry<String, Deque<String>> e : inboxes.entrySet()) {
            copy.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return copy;
    }

    /** Restores saved inboxes after a restart. */
    public synchronized void restore(Map<String, List<String>> saved) {
        if (saved == null) {
            return;
        }
        for (Map.Entry<String, List<String>> e : saved.entrySet()) {
            inboxes.put(e.getKey(), new ArrayDeque<>(e.getValue()));
        }
    }

    /** Newest first. */
    public synchronized List<String> inbox(String userId) {
        Deque<String> box = inboxes.get(userId);
        return box == null ? List.of() : new ArrayList<>(box);
    }
}
