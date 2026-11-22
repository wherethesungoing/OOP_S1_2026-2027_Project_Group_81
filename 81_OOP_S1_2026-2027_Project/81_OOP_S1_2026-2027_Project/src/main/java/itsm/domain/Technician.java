package itsm.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Investigates and resolves incidents. */
public class Technician extends User {

    private static final long serialVersionUID = 1L;

    /** A technician holding this many open tickets is at the workload limit and gets no more. */
    public static final int DEFAULT_CAPACITY = 8;

    private final List<String> skills;   // composition: a technician HAS skills
    private final int capacity;
    private int workload;

    public Technician(String userId, String name, String email, List<String> skills) {
        this(userId, name, email, skills, DEFAULT_CAPACITY);
    }

    public Technician(String userId, String name, String email, List<String> skills, int capacity) {
        super(userId, name, email);
        this.skills = new ArrayList<>(skills);
        this.capacity = capacity;
    }

    @Override
    public String role() {
        return "Technician";
    }

    /** A technician acknowledges (IN_PROGRESS) and resolves, but cannot close: the reporter verifies. */
    @Override
    public boolean canMoveTo(Status next) {
        return next == Status.IN_PROGRESS || next == Status.RESOLVED;
    }

    public List<String> getSkills() {
        return Collections.unmodifiableList(skills);
    }

    public int getWorkload() {
        return workload;
    }

    public int getCapacity() {
        return capacity;
    }

    /** Can take a new ticket: account enabled and below the workload limit. */
    public boolean isAvailable() {
        return isActive() && workload < capacity;
    }

    public void assignOne() {
        workload++;
    }

    public void completeOne() {
        if (workload > 0) {
            workload--;
        }
    }

    /** Used after loading saved data, when workload is recounted from the open tickets. */
    public void resetWorkload() {
        workload = 0;
    }
}
