package itsm.notify;

import itsm.domain.Incident;

/** Observer pattern: anything that wants to hear about incident events. */
public interface Notifier {

    void update(String event, Incident incident);
}
