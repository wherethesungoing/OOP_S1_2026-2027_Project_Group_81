package itsm.storage;

import itsm.auth.UserDirectory;
import itsm.core.ServiceCatalog;
import itsm.domain.Category;
import itsm.domain.Priority;
import itsm.exception.StorageException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/** Saves and loads everything that is not an incident: accounts, the service catalog and settings. */
public class SystemStore {

    /** One saved snapshot of the system's configuration. */
    public record State(UserDirectory users, ServiceCatalog services, String strategy,
            Map<Priority, int[]> slaPolicy, List<Category> categories,
            Map<String, List<String>> inboxes, Map<String, Map<String, Integer>> readUpTo,
            long clockOffsetMinutes) implements Serializable {
        private static final long serialVersionUID = 2L;

        /** Configuration only, with empty inboxes and real time. */
        public State(UserDirectory users, ServiceCatalog services, String strategy, Map<Priority, int[]> slaPolicy) {
            this(users, services, strategy, slaPolicy, Category.values(), Map.of(), Map.of(), 0);
        }
    }

    private final Path file;

    public SystemStore(String path) {
        this.file = Path.of(path);
    }

    /** The saved state, or null on first run. */
    public State load() {
        if (!Files.exists(file)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(file); ObjectInputStream obj = new ObjectInputStream(in)) {
            return (State) obj.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new StorageException("Could not read " + file + ". Delete the data folder to start fresh.", e);
        }
    }

    public void save(State state) {
        try {
            Path dir = file.toAbsolutePath().getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp); ObjectOutputStream obj = new ObjectOutputStream(out)) {
                obj.writeObject(state);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Could not save " + file, e);
        }
    }
}
