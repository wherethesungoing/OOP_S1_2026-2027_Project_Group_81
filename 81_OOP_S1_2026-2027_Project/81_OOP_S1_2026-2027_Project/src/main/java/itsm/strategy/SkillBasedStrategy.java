package itsm.strategy;

import itsm.domain.Incident;
import itsm.domain.Technician;
import itsm.exception.NoAvailableTechnicianException;
import java.util.Comparator;
import java.util.List;

/** Prefers a skill match for the incident category; falls back to least-loaded. */
public class SkillBasedStrategy implements AssignmentStrategy {

    @Override
    public Technician assign(Incident incident, List<Technician> technicians)
            throws NoAvailableTechnicianException {
        List<Technician> available = AssignmentStrategy.available(technicians);
        String needed = incident.getCategory().label();
        List<Technician> matches = available.stream()
                .filter(t -> t.getSkills().contains(needed))
                .toList();
        List<Technician> pool = matches.isEmpty() ? available : matches;
        return pool.stream()
                .min(Comparator.comparingInt(Technician::getWorkload))
                .orElseThrow(() -> new NoAvailableTechnicianException("No technicians available"));
    }
}
