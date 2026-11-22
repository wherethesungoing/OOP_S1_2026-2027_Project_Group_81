package itsm.monitor;

import itsm.core.IncidentManager;
import itsm.domain.Incident;
import itsm.domain.Priority;
import itsm.domain.SLA;
import itsm.domain.SlaState;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Innovation features: predictive SLA violation detection and automated escalation.
 * Run check(now) on a schedule (the dashboard does every 2 seconds); "now" is a parameter
 * so tests and demos can simulate time passing.
 */
public class SlaMonitor {

    /** Warn once 75% of the SLA window has been used. */
    public static final double AT_RISK_THRESHOLD = 0.75;

    private final IncidentManager manager;
    private final int ackMinutes;

    public SlaMonitor(IncidentManager manager) {
        this(manager, 15);
    }

    /** @param ackMinutes a critical incident not acknowledged within this many minutes is escalated */
    public SlaMonitor(IncidentManager manager, int ackMinutes) {
        this.manager = manager;
        this.ackMinutes = ackMinutes;
    }

    /** Checks every open incident and returns the alerts raised, e.g. "SLA_AT_RISK:INC-0001". */
    public List<String> check(LocalDateTime now) {
        List<String> alerts = new ArrayList<>();
        for (Incident inc : manager.getOpen()) {
            if (inc.getPriority() == null) {
                continue;
            }

            double used = inc.getSla().elapsedFraction(inc.getCreatedAt(), now);
            SlaState state = used >= 1.0 ? SlaState.BREACHED
                    : (used >= AT_RISK_THRESHOLD ? SlaState.AT_RISK : SlaState.ON_TRACK);
            if (state != inc.getSlaState()) {
                inc.setSlaState(state);
                if (state != SlaState.ON_TRACK) {
                    String event = (state == SlaState.BREACHED) ? "SLA_BREACHED" : "SLA_AT_RISK";
                    manager.announce(event, inc);
                    alerts.add(event + ":" + inc.getId());
                }
            }

            long minutes = Duration.between(inc.getCreatedAt(), now).toMinutes();
            if (inc.getPriority() == Priority.CRITICAL && !inc.isAcknowledged() && minutes >= ackMinutes) {
                escalate(inc, "Critical incident not acknowledged within " + ackMinutes + " min", alerts);
            }
            if (state == SlaState.BREACHED) {
                escalate(inc, "SLA breached", alerts);
            }
        }
        return alerts;
    }

    private void escalate(Incident inc, String reason, List<String> alerts) {
        if (inc.escalate(reason)) {
            manager.announce("ESCALATED", inc);
            alerts.add("ESCALATED:" + inc.getId());
        }
    }
}
