package itsm.core;

import itsm.domain.Category;
import itsm.domain.Incident;
import itsm.domain.Priority;
import itsm.domain.SlaState;
import itsm.domain.Status;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Search filters for incidents. Every filter is optional; leave it null to ignore it.
 * Built fluently: new SearchCriteria().status(Status.CLOSED).text("wifi").
 */
public class SearchCriteria {

    private String text;          // matches id, title, description or location
    private Status status;
    private Priority priority;
    private Category category;
    private String serviceKey;
    private SlaState slaState;
    private String assigneeId;
    private LocalDate from;       // reported on or after
    private LocalDate to;         // reported on or before

    public SearchCriteria text(String value) {
        this.text = blankToNull(value);
        return this;
    }

    public SearchCriteria status(Status value) {
        this.status = value;
        return this;
    }

    public SearchCriteria priority(Priority value) {
        this.priority = value;
        return this;
    }

    public SearchCriteria category(Category value) {
        this.category = value;
        return this;
    }

    public SearchCriteria service(String value) {
        this.serviceKey = blankToNull(value);
        return this;
    }

    public SearchCriteria slaState(SlaState value) {
        this.slaState = value;
        return this;
    }

    public SearchCriteria assignee(String userId) {
        this.assigneeId = blankToNull(userId);
        return this;
    }

    public SearchCriteria from(LocalDate value) {
        this.from = value;
        return this;
    }

    public SearchCriteria to(LocalDate value) {
        this.to = value;
        return this;
    }

    public boolean matches(Incident i) {
        if (status != null && i.getStatus() != status) {
            return false;
        }
        if (priority != null && i.getPriority() != priority) {
            return false;
        }
        if (category != null && !category.equals(i.getCategory())) {
            return false;
        }
        if (serviceKey != null && !serviceKey.equals(i.getServiceKey())) {
            return false;
        }
        if (slaState != null && i.getSlaState() != slaState) {
            return false;
        }
        if (assigneeId != null && (i.getAssignee() == null || !i.getAssignee().getUserId().equals(assigneeId))) {
            return false;
        }
        LocalDate day = i.getCreatedAt().toLocalDate();
        if (from != null && day.isBefore(from)) {
            return false;
        }
        if (to != null && day.isAfter(to)) {
            return false;
        }
        if (text != null) {
            String t = text.toLowerCase(Locale.ROOT);
            return i.getId().toLowerCase(Locale.ROOT).contains(t)
                    || i.getTitle().toLowerCase(Locale.ROOT).contains(t)
                    || i.getDescription().toLowerCase(Locale.ROOT).contains(t)
                    || i.getLocation().toLowerCase(Locale.ROOT).contains(t);
        }
        return true;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
