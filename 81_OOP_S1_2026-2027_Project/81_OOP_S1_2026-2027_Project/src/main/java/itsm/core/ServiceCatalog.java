package itsm.core;

import itsm.domain.Category;
import itsm.domain.ItService;
import itsm.exception.ServiceNotFoundException;
import itsm.exception.ValidationException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The IT services people can report problems with. Administrators add services and set their criticality. */
public class ServiceCatalog implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, ItService> services = new LinkedHashMap<>();

    /** The campus services the app starts with. Criticality: the LMS matters most during quizzes and exams. */
    public static ServiceCatalog campusDefaults() {
        ServiceCatalog c = new ServiceCatalog();
        try {
            // Hostel technician
            c.put(new ItService("wifi", "Hostel Wi-Fi", Category.HOSTEL, 4));
            c.put(new ItService("hostel-room", "Hostel room (LAN port, power, fittings)", Category.HOSTEL, 2));
            // Network technician
            c.put(new ItService("campus-network", "Campus Wi-Fi and internet", Category.NETWORK, 5));
            c.put(new ItService("vpn", "VPN and remote access", Category.NETWORK, 3));
            // Website technician
            c.put(new ItService("lms", "LMS and student portal", Category.WEBSITE, 5));
            c.put(new ItService("website", "Campus website", Category.WEBSITE, 3));
            c.put(new ItService("email", "Campus email", Category.WEBSITE, 3));
            c.put(new ItService("other", "Something else", Category.APPLICATION, 1));
        } catch (ValidationException e) {
            throw new IllegalStateException(e);
        }
        return c;
    }

    private void put(ItService s) {
        services.put(s.getKey(), s);
    }

    /** Adds a new service; its key is made from the name. */
    public ItService add(String name, Category category, int criticality) throws ValidationException {
        if (name == null || name.isBlank()) {
            throw new ValidationException("A service needs a name");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 40) {
            throw new ValidationException("Keep the service name under 40 characters");
        }
        String key = trimmed.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (key.isEmpty()) {
            throw new ValidationException("Use letters or digits in the service name");
        }
        for (ItService s : services.values()) {
            if (s.getKey().equals(key) || s.getName().equalsIgnoreCase(trimmed)) {
                throw new ValidationException("A service called \"" + s.getName() + "\" already exists");
            }
        }
        ItService service = new ItService(key, trimmed, category, criticality);
        put(service);
        return service;
    }

    public ItService find(String key) throws ServiceNotFoundException {
        ItService s = services.get(key);
        if (s == null) {
            throw new ServiceNotFoundException(key);
        }
        return s;
    }

    public boolean contains(String key) {
        return services.containsKey(key);
    }

    /** Business criticality used by prioritization; unknown services count as least critical. */
    public int criticalityOf(String key) {
        ItService s = services.get(key);
        return (s == null) ? 1 : s.getCriticality();
    }

    public String nameOf(String key) {
        ItService s = services.get(key);
        return (s == null) ? key : s.getName();
    }

    public List<ItService> all() {
        return new ArrayList<>(services.values());
    }
}
