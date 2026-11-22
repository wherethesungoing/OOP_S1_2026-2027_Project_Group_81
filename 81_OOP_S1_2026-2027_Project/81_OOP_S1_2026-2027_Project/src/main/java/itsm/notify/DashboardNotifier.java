package itsm.notify;

import itsm.domain.Incident;
import java.util.ArrayList;
import java.util.List;

/** Third notifier, added with zero changes to the others (Open/Closed). */
public class DashboardNotifier implements Notifier {

    private final List<String> events = new ArrayList<>();

    @Override
    public void update(String event, Incident incident) {
        events.add(event + ":" + incident.getId());
    }

    public List<String> getEvents() {
        return events;
    }
}
