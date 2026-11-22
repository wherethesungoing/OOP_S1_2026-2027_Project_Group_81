package itsm.strategy;

import itsm.domain.Incident;
import itsm.domain.Technician;
import itsm.exception.NoAvailableTechnicianException;
import java.util.List;

/** Takes turns through the available technicians. */
public class RoundRobinStrategy implements AssignmentStrategy {

    private int next = 0;

    @Override
    public Technician assign(Incident incident, List<Technician> technicians)
            throws NoAvailableTechnicianException {
        List<Technician> pool = AssignmentStrategy.available(technicians);
        Technician chosen = pool.get(next % pool.size());
        next++;
        return chosen;
    }
}
