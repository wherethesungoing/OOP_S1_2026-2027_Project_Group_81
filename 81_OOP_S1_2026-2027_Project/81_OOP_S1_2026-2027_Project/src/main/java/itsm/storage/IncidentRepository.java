package itsm.storage;

import itsm.domain.Incident;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Repository interface: storage abstraction (swap in-memory / file / DB). */
public interface IncidentRepository {

    void save(Incident incident);
    Optional<Incident> findById(String id);
    List<Incident> findAll();

    /** Saves many incidents at once. A store that writes a whole file can override this to write once. */
    default void saveAll(Collection<Incident> incidents) {
        for (Incident i : incidents) {
            save(i);
        }
    }
}
