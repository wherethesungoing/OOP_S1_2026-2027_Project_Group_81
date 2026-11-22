package itsm.report;

import itsm.core.IncidentManager;
import itsm.domain.Incident;
import itsm.domain.Priority;
import itsm.domain.SlaState;
import itsm.domain.Status;
import java.time.Duration;
import itsm.domain.Technician;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Analytics for reports and the dashboard. */
public class ReportService {

    private final IncidentManager manager;

    public ReportService(IncidentManager manager) {
        this.manager = manager;
    }

    public Map<Status, Long> byStatus() {
        Map<Status, Long> counts = new EnumMap<>(Status.class);
        for (Status s : Status.values()) {
            counts.put(s, 0L);
        }
        for (Incident i : manager.getAll()) {
            counts.merge(i.getStatus(), 1L, Long::sum);
        }
        return counts;
    }

    public Map<Priority, Long> byPriority() {
        Map<Priority, Long> counts = new EnumMap<>(Priority.class);
        for (Priority p : Priority.values()) {
            counts.put(p, 0L);
        }
        for (Incident i : manager.getAll()) {
            if (i.getPriority() != null) {
                counts.merge(i.getPriority(), 1L, Long::sum);
            }
        }
        return counts;
    }

    public Map<String, Long> byService() {
        Map<String, Long> counts = new TreeMap<>();
        for (Incident i : manager.getAll()) {
            counts.merge(i.getServiceKey(), 1L, Long::sum);
        }
        return counts;
    }

    public long openCount() {
        return manager.getOpen().size();
    }

    public long escalatedCount() {
        long n = 0;
        for (Incident i : manager.getAll()) {
            if (i.isEscalated()) {
                n++;
            }
        }
        return n;
    }

    public long slaCount(SlaState state) {
        long n = 0;
        for (Incident i : manager.getAll()) {
            if (i.getSlaState() == state) {
                n++;
            }
        }
        return n;
    }

    /** Open tickets per technician, for the workload report. */
    public Map<Technician, Long> workload(List<Technician> technicians) {
        Map<Technician, Long> counts = new LinkedHashMap<>();
        for (Technician t : technicians) {
            counts.put(t, 0L);
        }
        for (Incident i : manager.getOpen()) {
            if (i.getAssignee() != null && counts.containsKey(i.getAssignee())) {
                counts.merge(i.getAssignee(), 1L, Long::sum);
            }
        }
        return counts;
    }

    /** Average feedback rating (1 to 5) over tickets that have feedback; 0 when there is none. */
    public double averageRating() {
        int total = 0;
        int n = 0;
        for (Incident i : manager.getAll()) {
            if (i.hasFeedback()) {
                total += i.getFeedbackRating();
                n++;
            }
        }
        return n == 0 ? 0.0 : (double) total / n;
    }

    /** Average minutes from report to resolution, over resolved incidents. */
    public double avgResolutionMinutes() {
        long seconds = 0;
        int n = 0;
        for (Incident i : manager.getAll()) {
            if (i.getResolvedAt() != null) {
                seconds += Duration.between(i.getCreatedAt(), i.getResolvedAt()).toSeconds();
                n++;
            }
        }
        return n == 0 ? 0.0 : seconds / 60.0 / n;
    }
}
