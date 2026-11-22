package itsm.notify;

import itsm.domain.Incident;
import java.util.ArrayList;
import java.util.List;

public class SmsNotifier implements Notifier {

    private final List<String> sent = new ArrayList<>();

    @Override
    public void update(String event, Incident incident) {
        String msg = "[SMS] " + event + ": " + incident.getId();
        sent.add(msg);
        System.out.println(msg);
    }

    public List<String> getSent() {
        return sent;
    }
}
