package itsm.strategy;

import itsm.domain.Incident;
import itsm.domain.Technician;
import itsm.exception.NoAvailableTechnicianException;
import java.util.List;

/** Strategy pattern: interchangeable technician-assignment algorithms. */
public interface AssignmentStrategy {

    Technician assign(Incident incident, List<Technician> technicians)
            throws NoAvailableTechnicianException;

    /** A short name for screens and saved settings. */
    default String label() {
        return getClass().getSimpleName();
    }

    /**
     * Shared rule for every strategy: only technicians whose account is enabled and who are
     * below their workload limit can take a new ticket.
     */
    static List<Technician> available(List<Technician> technicians) throws NoAvailableTechnicianException {
        if (technicians == null || technicians.isEmpty()) {
            throw new NoAvailableTechnicianException("No technicians available");
        }
        List<Technician> pool = technicians.stream().filter(Technician::isAvailable).toList();
        if (pool.isEmpty()) {
            throw new NoAvailableTechnicianException(
                    "No technician can take this ticket: every technician is unavailable or at their workload limit");
        }
        return pool;
    }
}
