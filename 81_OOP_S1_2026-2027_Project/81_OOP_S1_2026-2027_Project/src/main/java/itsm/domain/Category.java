package itsm.domain;

import itsm.exception.ValidationException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * An incident category (Network, Database, ...). The six standard categories are built in, and an
 * administrator can add more while the system runs; nothing else in the code has to change, because
 * services, skills, search and assignment all work with whatever categories exist.
 */
public final class Category implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Map<String, Category> REGISTRY = new LinkedHashMap<>();

    public static final Category NETWORK = builtIn("Network");
    public static final Category DATABASE = builtIn("Database");
    public static final Category AUTHENTICATION = builtIn("Authentication");
    public static final Category APPLICATION = builtIn("Application");
    public static final Category STORAGE = builtIn("Storage");
    public static final Category HARDWARE = builtIn("Hardware");
    public static final Category HOSTEL = builtIn("Hostel");
    public static final Category WEBSITE = builtIn("Website");

    private final String name;    // key, e.g. "CLOUD_SERVICES"
    private final String label;   // shown to people, e.g. "Cloud services"

    private Category(String name, String label) {
        this.name = name;
        this.label = label;
    }

    private static Category builtIn(String label) {
        Category c = new Category(keyOf(label), label);
        REGISTRY.put(c.name, c);
        return c;
    }

    private static String keyOf(String label) {
        return label.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_|_$)", "");
    }

    /** Administrator adds a category. */
    public static synchronized Category add(String label) throws ValidationException {
        if (label == null || label.isBlank()) {
            throw new ValidationException("A category needs a name");
        }
        String trimmed = label.trim();
        if (trimmed.length() > 30) {
            throw new ValidationException("Keep the category name under 30 characters");
        }
        if (trimmed.contains(",")) {
            throw new ValidationException("Category names cannot contain commas");
        }
        String key = keyOf(trimmed);
        if (key.isEmpty()) {
            throw new ValidationException("Use letters or digits in the category name");
        }
        if (REGISTRY.containsKey(key)) {
            throw new ValidationException("The category \"" + REGISTRY.get(key).label + "\" already exists");
        }
        Category c = new Category(key, trimmed);
        REGISTRY.put(key, c);
        return c;
    }

    /** All categories, built-in first. */
    public static synchronized List<Category> values() {
        return new ArrayList<>(REGISTRY.values());
    }

    /** Finds a category by key or label (any case); null when there is none. */
    public static synchronized Category find(String nameOrLabel) {
        if (nameOrLabel == null || nameOrLabel.isBlank()) {
            return null;
        }
        return REGISTRY.get(keyOf(nameOrLabel));
    }

    /** Restores saved categories after a restart. */
    public static synchronized void restore(Collection<Category> saved) {
        if (saved != null) {
            for (Category c : saved) {
                REGISTRY.putIfAbsent(c.name, c);
            }
        }
    }

    public String name() {
        return name;
    }

    public String label() {
        return label;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Category && ((Category) other).name.equals(name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
