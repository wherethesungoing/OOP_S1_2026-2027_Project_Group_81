package itsm.domain;

import itsm.exception.ValidationException;
import java.io.Serializable;

/**
 * An IT service people can report problems with (hostel Wi-Fi, the LMS, ...).
 * Its business criticality (1 to 5) feeds the priority calculation, and its category
 * decides which technicians' skills match.
 */
public class ItService implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String key;
    private final String name;
    private final Category category;
    private int criticality;

    public ItService(String key, String name, Category category, int criticality) throws ValidationException {
        if (key == null || key.isBlank() || name == null || name.isBlank()) {
            throw new ValidationException("A service needs a name");
        }
        if (category == null) {
            throw new ValidationException("A service needs a category");
        }
        this.key = key;
        this.name = name.trim();
        this.category = category;
        setCriticality(criticality);
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public int getCriticality() {
        return criticality;
    }

    public final void setCriticality(int criticality) throws ValidationException {
        if (criticality < 1 || criticality > 5) {
            throw new ValidationException("Business criticality must be between 1 and 5");
        }
        this.criticality = criticality;
    }
}
