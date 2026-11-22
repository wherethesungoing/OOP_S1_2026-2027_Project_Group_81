package itsm.storage;

import itsm.domain.Incident;
import itsm.exception.StorageException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * File-based incident store that survives restarts.
 *
 * Two files are kept side by side:
 *   incidents.dat  the full incidents (history, notes, links, feedback), loaded again on start-up
 *   incidents.csv  a readable summary, one row per incident, for reports and spreadsheets
 * Each write goes to a temporary file first and then replaces the old one, so a crash
 * mid-write cannot leave a half-written store behind.
 */
public class FileRepository implements IncidentRepository {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Path csvFile;
    private final Path dataFile;
    private final Map<String, Incident> cache = new LinkedHashMap<>();

    /** @param path the CSV summary file; the full data file sits next to it with a .dat extension */
    public FileRepository(String path) {
        this.csvFile = Path.of(path);
        String name = csvFile.getFileName().toString();
        String base = name.endsWith(".csv") ? name.substring(0, name.length() - 4) : name;
        this.dataFile = csvFile.resolveSibling(base + ".dat");
        load();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(dataFile)) {
            return;
        }
        try (InputStream in = Files.newInputStream(dataFile); ObjectInputStream obj = new ObjectInputStream(in)) {
            List<Incident> saved = (List<Incident>) obj.readObject();
            for (Incident i : saved) {
                cache.put(i.getId(), i);
            }
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new StorageException("Could not read saved incidents from " + dataFile
                    + ". Delete the data folder to start fresh.", e);
        }
    }

    @Override
    public void save(Incident incident) {
        cache.put(incident.getId(), incident);
        writeAll();
    }

    @Override
    public void saveAll(Collection<Incident> incidents) {
        for (Incident i : incidents) {
            cache.put(i.getId(), i);
        }
        writeAll();
    }

    private void writeAll() {
        try {
            Path dir = csvFile.toAbsolutePath().getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            Path tmp = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp); ObjectOutputStream obj = new ObjectOutputStream(out)) {
                obj.writeObject(new ArrayList<>(cache.values()));
            }
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING);
            Files.write(csvFile, csvLines());
        } catch (IOException e) {
            throw new StorageException("Could not save incidents to " + dataFile, e);
        }
    }

    private List<String> csvLines() {
        List<String> lines = new ArrayList<>();
        lines.add("id,title,status,priority,category,service,assignee,reporter,created,sla,escalated");
        for (Incident i : cache.values()) {
            lines.add(String.join(",",
                    i.getId(),
                    clean(i.getTitle()),
                    i.getStatus().label(),
                    i.getPriority() == null ? "" : i.getPriority().name(),
                    i.getCategory().label(),
                    i.getServiceKey(),
                    i.getAssignee() == null ? "" : i.getAssignee().getUserId(),
                    i.getReporter() == null ? "" : i.getReporter().getUserId(),
                    i.getCreatedAt().format(TIME),
                    i.getSlaState().name(),
                    String.valueOf(i.isEscalated())));
        }
        return lines;
    }

    private static String clean(String s) {
        return s.replace(",", ";").replace("\n", " ");
    }

    @Override
    public Optional<Incident> findById(String id) {
        return Optional.ofNullable(cache.get(id));
    }

    @Override
    public List<Incident> findAll() {
        return new ArrayList<>(cache.values());
    }

    public Path getFile() {
        return csvFile;
    }

    public Path getDataFile() {
        return dataFile;
    }
}
