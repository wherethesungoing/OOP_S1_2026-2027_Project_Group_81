package itsm.notify;

import itsm.domain.Incident;
import java.util.ArrayList;
import java.util.List;

public class EmailNotifier implements Notifier {

    private final List<String> sent = new ArrayList<>();

    @Override
    public void update(String event, Incident incident) {
        String msg = "[EMAIL] " + event + ": " + incident.getId() + " (" + incident.getTitle() + ")";
        sent.add(msg);
        System.out.println(msg);
    }

    public List<String> getSent() {
        return sent;
    }
}
