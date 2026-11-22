package itsm.storage;

import itsm.domain.Incident;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryRepository implements IncidentRepository {

    private final Map<String, Incident> store = new LinkedHashMap<>();

    @Override
    public void save(Incident incident) {
        store.put(incident.getId(), incident);
    }

    @Override
    public Optional<Incident> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Incident> findAll() {
        return new ArrayList<>(store.values());
    }
}
