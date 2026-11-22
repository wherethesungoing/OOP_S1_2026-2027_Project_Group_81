package itsm.core;

import itsm.domain.Category;
import itsm.domain.Employee;
import itsm.domain.Incident;
import itsm.domain.Priority;
import itsm.domain.Status;
import itsm.domain.Technician;
import itsm.domain.User;
import itsm.domain.ItService;
import itsm.exception.DuplicateIncidentException;
import itsm.exception.ITSMException;
import itsm.exception.NoAvailableTechnicianException;
import itsm.exception.IncidentNotFoundException;
import itsm.exception.InvalidStateTransitionException;
import itsm.exception.UnauthorizedException;
import itsm.exception.ValidationException;
import itsm.notify.Notifier;
import itsm.storage.IncidentRepository;
import itsm.strategy.AssignmentStrategy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Orchestrates the incident lifecycle; acts as the Subject for observers. */
public class IncidentManager {

    /** This many related reports of one problem make it a major incident. */
    private static final int MAJOR_INCIDENT_SIZE = 3;
    private static final int MAX_TEXT = 1000;

    private final IncidentRepository repository;
    private final ServiceCatalog catalog;
    private AssignmentStrategy strategy;
    private final List<Notifier> observers = new ArrayList<>();
    private final Map<String, Incident> incidents = new LinkedHashMap<>();

    public IncidentManager(IncidentRepository repository, AssignmentStrategy strategy) {
        this(repository, strategy, ServiceCatalog.campusDefaults());
    }

    /** Loads any incidents the repository already holds, so a restart picks up where it left off. */
    public IncidentManager(IncidentRepository repository, AssignmentStrategy strategy, ServiceCatalog catalog) {
        this.repository = repository;
        this.strategy = strategy;
        this.catalog = catalog;
        for (Incident saved : repository.findAll()) {
            incidents.put(saved.getId(), saved);
            Incident.ensureCounterAbove(saved.getId());
        }
    }

    // ---- Observer wiring ----
    public void subscribe(Notifier notifier) {
        observers.add(notifier);
    }

    private void publish(String event, Incident incident) {
        for (Notifier n : observers) {
            n.update(event, incident);
        }
    }

    /** Lets collaborators such as the SLA monitor raise events through the same observers. */
    public void announce(String event, Incident incident) {
        publish(event, incident);
    }

    // ---- Strategy swap at runtime ----
    public void setStrategy(AssignmentStrategy strategy) {
        this.strategy = strategy;
    }

    /** Configuration change with an authorization check: administrators only. */
    public void setStrategy(AssignmentStrategy strategy, User actor) throws UnauthorizedException {
        if (actor == null || !actor.canConfigure()) {
            throw new UnauthorizedException("Only an administrator can change the assignment strategy");
        }
        this.strategy = strategy;
    }

    public String getStrategyName() {
        return strategy.getClass().getSimpleName();
    }

    public ServiceCatalog getCatalog() {
        return catalog;
    }

    // ---- lifecycle ----

    /**
     * Reports an incident on behalf of a person, refusing it when the same person already has an
     * open ticket that says the same thing. Reports of one problem by different people are accepted
     * and linked (see correlate), because more reports mean more people are affected.
     */
    public Incident submit(Incident incident) throws DuplicateIncidentException {
        if (incident.getReporter() != null) {
            for (DuplicateMatch d : findDuplicates(incident, 0.8)) {
                if (incident.getReporter().equals(d.incident().getReporter()) && d.incident().isOpen()) {
                    throw new DuplicateIncidentException(d.incident().getId());
                }
            }
        }
        return report(incident);
    }

    public Incident report(Incident incident) {
        incidents.put(incident.getId(), incident);
        incident.setPriority(prioritize(incident));
        repository.save(incident);
        publish("REPORTED", incident);
        correlate(incident);
        return incident;
    }

    /** Workflow 3: link likely duplicates, and escalate when many reports describe one problem. */
    private void correlate(Incident incident) {
        List<DuplicateMatch> dups = findDuplicates(incident);
        if (dups.isEmpty()) {
            return;
        }
        for (DuplicateMatch d : dups) {
            incident.addRelated(d.incident().getId());
            d.incident().addRelated(incident.getId());
        }
        publish("CORRELATED", incident);
        int clusterSize = incident.getRelatedIds().size() + 1;
        if (clusterSize >= MAJOR_INCIDENT_SIZE) {
            String reason = "Major incident: " + clusterSize + " related reports on " + catalog.nameOf(incident.getServiceKey());
            if (incident.escalate(reason)) {
                publish("ESCALATED", incident);
            }
            for (String rid : incident.getRelatedIds()) {
                Incident rel = incidents.get(rid);
                if (rel != null && rel.escalate(reason)) {
                    publish("ESCALATED", rel);
                }
            }
        }
    }

    public Technician assign(String incidentId, List<Technician> technicians) throws ITSMException {
        Incident inc = get(incidentId);
        Technician tech = strategy.assign(inc, technicians);
        inc.setAssignee(tech);
        inc.transitionTo(Status.ASSIGNED);
        tech.assignOne();
        repository.save(inc);
        publish("ASSIGNED", inc);
        return tech;
    }

    public Incident advance(String incidentId, Status next) throws ITSMException {
        return advance(incidentId, next, "system");
    }

    public Incident advance(String incidentId, Status next, String actor) throws ITSMException {
        Incident inc = get(incidentId);
        inc.transitionTo(next, actor);
        repository.save(inc);
        publish(next.name(), inc);
        return inc;
    }

    /** Role-checked move: the user's role decides what is allowed (polymorphic authorization). */
    public Incident advance(String incidentId, Status next, User actor) throws ITSMException {
        if (actor == null || !actor.canMoveTo(next)) {
            String who = (actor == null) ? "Unknown user" : actor.role() + " " + actor.getName();
            throw new UnauthorizedException(who + " is not allowed to move an incident to " + next.label());
        }
        checkOwnership(get(incidentId), actor);
        Incident inc = advance(incidentId, next, actor.getName());
        if (next == Status.RESOLVED && inc.getAssignee() != null) {
            inc.getAssignee().completeOne();
        }
        if (next == Status.REOPENED && inc.getAssignee() != null) {
            inc.getAssignee().assignOne();
        }
        if (next == Status.RESOLVED && inc.getReporter() != null) {
            publish("REPORTER_NOTIFIED", inc);
        }
        return inc;
    }

    /**
     * Business rule: a technician works only on tickets assigned to them, and only the
     * person who reported a ticket can verify (close or reopen) it. Administrators may act on any ticket.
     */
    private static void checkOwnership(Incident inc, User actor) throws UnauthorizedException {
        if (actor instanceof Technician && inc.getAssignee() != null && !inc.getAssignee().equals(actor)) {
            throw new UnauthorizedException(inc.getId() + " is assigned to " + inc.getAssignee().getName());
        }
        if (actor instanceof Employee && inc.getReporter() != null && !inc.getReporter().equals(actor)) {
            throw new UnauthorizedException("Only the person who reported " + inc.getId() + " can do that");
        }
    }

    // ---- changes by staff ----

    /** Administrator moves an open ticket to a chosen technician, who must be available. */
    public void reassign(String incidentId, Technician to, User actor) throws ITSMException {
        requireAdmin(actor, "reassign tickets");
        Incident inc = get(incidentId);
        if (!inc.isOpen()) {
            throw new ValidationException(inc.getId() + " is no longer open");
        }
        if (to == null) {
            throw new ValidationException("Choose a technician");
        }
        if (to.equals(inc.getAssignee())) {
            throw new ValidationException(inc.getId() + " is already with " + to.getName());
        }
        if (!to.isActive()) {
            throw new NoAvailableTechnicianException(to.getName() + " is unavailable (account disabled)");
        }
        if (!to.isAvailable()) {
            throw new NoAvailableTechnicianException(to.getName() + " is at their workload limit of " + to.getCapacity());
        }
        moveTo(inc, to, actor.getName());
    }

    /** Moves an open ticket to whoever the current assignment strategy picks (used when a technician is disabled). */
    public Technician reassign(String incidentId, List<Technician> technicians, User actor) throws ITSMException {
        requireAdmin(actor, "reassign tickets");
        Incident inc = get(incidentId);
        Technician to = strategy.assign(inc, technicians);
        if (!to.equals(inc.getAssignee())) {
            moveTo(inc, to, actor.getName());
        }
        return to;
    }

    private void moveTo(Incident inc, Technician to, String actor) {
        if (inc.getAssignee() != null) {
            inc.getAssignee().completeOne();
        }
        inc.reassign(actor, to);
        to.assignOne();
        repository.save(inc);
        publish("REASSIGNED", inc);
    }

    /** Administrator or the assigned technician changes the priority, giving a reason. */
    public void changePriority(String incidentId, Priority priority, User actor, String reason) throws ITSMException {
        Incident inc = get(incidentId);
        if (actor == null || !(actor.canConfigure() || (actor instanceof Technician && actor.equals(inc.getAssignee())))) {
            throw new UnauthorizedException("Only the assigned technician or an administrator can change the priority");
        }
        if (priority == null) {
            throw new ValidationException("Choose a priority");
        }
        if (priority == inc.getPriority()) {
            throw new ValidationException(inc.getId() + " already has that priority");
        }
        if (!inc.isOpen()) {
            throw new ValidationException(inc.getId() + " is no longer open");
        }
        inc.changePriority(actor.getName(), priority, requireText(reason, "the reason for the change"));
        repository.save(inc);
        publish("PRIORITY_CHANGED", inc);
    }

    /** Administrator moves a ticket to a different service, for example when it was reported under the wrong one. */
    public void changeService(String incidentId, ItService service, User actor) throws ITSMException {
        requireAdmin(actor, "change a ticket's service");
        Incident inc = get(incidentId);
        if (service.getKey().equals(inc.getServiceKey())) {
            throw new ValidationException(inc.getId() + " is already under " + service.getName());
        }
        inc.reclassify(actor.getName(), service.getKey(), service.getName(), service.getCategory());
        repository.save(inc);
        publish("RECLASSIFIED", inc);
    }

    private static void requireAdmin(User actor, String what) throws UnauthorizedException {
        if (actor == null || !actor.canConfigure()) {
            throw new UnauthorizedException("Only an administrator can " + what);
        }
    }

    /** A technician (or administrator) records an investigation note. */
    public void addNote(String incidentId, User actor, String note) throws ITSMException {
        if (actor == null || !(actor instanceof Technician || actor.canConfigure())) {
            throw new UnauthorizedException("Only technicians or administrators can add investigation notes");
        }
        Incident inc = get(incidentId);
        checkOwnership(inc, actor);
        inc.addNote(actor.getName(), requireText(note, "a note"));
        repository.save(inc);
        publish("NOTE_ADDED", inc);
    }

    /** The reporter adds more detail to an open ticket. */
    public void addComment(String incidentId, User actor, String text) throws ITSMException {
        Incident inc = get(incidentId);
        if (!(actor instanceof Employee) && (actor == null || !actor.canConfigure())) {
            throw new UnauthorizedException("Only the reporter can add information to a ticket");
        }
        checkOwnership(inc, actor);
        if (inc.getStatus() == Status.CLOSED) {
            throw new ValidationException(inc.getId() + " is closed. Raise a new ticket instead.");
        }
        inc.addComment(actor.getName(), requireText(text, "the extra information"));
        repository.save(inc);
        publish("COMMENT_ADDED", inc);
    }

    /** The assigned technician asks the reporter for more information. */
    public void requestInfo(String incidentId, User actor, String question) throws ITSMException {
        if (!(actor instanceof Technician) && (actor == null || !actor.canConfigure())) {
            throw new UnauthorizedException("Only support staff can ask the reporter for information");
        }
        Incident inc = get(incidentId);
        checkOwnership(inc, actor);
        if (!inc.isOpen()) {
            throw new ValidationException(inc.getId() + " is no longer open");
        }
        inc.requestInfo(actor.getName(), requireText(question, "your question"));
        repository.save(inc);
        publish("INFO_REQUESTED", inc);
    }

    /** The reporter answers the technician's question. */
    public void answerInfo(String incidentId, User actor, String answer) throws ITSMException {
        Incident inc = get(incidentId);
        if (!(actor instanceof Employee) && (actor == null || !actor.canConfigure())) {
            throw new UnauthorizedException("Only the reporter can answer a support request");
        }
        checkOwnership(inc, actor);
        if (!inc.isWaitingForReporter()) {
            throw new ValidationException("Support has not asked anything on " + inc.getId());
        }
        inc.answerInfo(actor.getName(), requireText(answer, "your reply"));
        repository.save(inc);
        publish("INFO_PROVIDED", inc);
    }

    /** Resolve with resolution details, which the reporter sees when asked to confirm. */
    public Incident resolve(String incidentId, User actor, String resolution) throws ITSMException {
        Incident inc = get(incidentId);
        String text = requireText(resolution, "what was done to fix it");
        if (actor == null || !actor.canMoveTo(Status.RESOLVED)) {
            throw new UnauthorizedException("Your role cannot resolve tickets");
        }
        checkOwnership(inc, actor);
        if (inc.getStatus() != Status.IN_PROGRESS) {
            throw new InvalidStateTransitionException(
                    inc.getId() + ": cannot go " + inc.getStatus().label() + " -> Resolved");
        }
        inc.setResolution(actor.getName(), text);
        return advance(incidentId, Status.RESOLVED, actor);
    }

    /** The reporter rates the fix after closing. */
    public void giveFeedback(String incidentId, User actor, int rating, String comment) throws ITSMException {
        Incident inc = get(incidentId);
        if (!(actor instanceof Employee)) {
            throw new UnauthorizedException("Only the reporter can give feedback");
        }
        checkOwnership(inc, actor);
        if (comment != null && comment.length() > MAX_TEXT) {
            throw new ValidationException("Keep the comment under " + MAX_TEXT + " characters");
        }
        inc.giveFeedback(actor.getName(), rating, comment);
        repository.save(inc);
        publish("FEEDBACK", inc);
    }

    private static String requireText(String text, String what) throws ValidationException {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Please type " + what + ".");
        }
        String trimmed = text.trim();
        if (trimmed.length() > MAX_TEXT) {
            throw new ValidationException("Keep it under " + MAX_TEXT + " characters.");
        }
        return trimmed;
    }

    // ---- intelligent feature #1: rule-based prioritization ----
    Priority prioritize(Incident incident) {
        int impact = catalog.criticalityOf(incident.getServiceKey());
        int users = incident.getAffectedUsers();
        int userFactor = users >= 500 ? 3 : (users >= 50 ? 2 : 1);
        int score = impact * incident.getUrgency() * userFactor;
        if (score >= 30) {
            return Priority.CRITICAL;
        }
        if (score >= 15) {
            return Priority.HIGH;
        }
        if (score >= 6) {
            return Priority.MEDIUM;
        }
        return Priority.LOW;
    }

    // ---- intelligent feature #2: duplicate detection (Jaccard similarity) ----
    public List<DuplicateMatch> findDuplicates(Incident incident) {
        return findDuplicates(incident, 0.5);
    }

    public List<DuplicateMatch> findDuplicates(Incident incident, double threshold) {
        Set<String> a = tokens(incident.getDescription());
        List<DuplicateMatch> out = new ArrayList<>();
        for (Incident other : incidents.values()) {
            if (other.getId().equals(incident.getId())) {
                continue;
            }
            if (other.getStatus() == Status.CLOSED) {
                continue;
            }
            if (!other.getServiceKey().equals(incident.getServiceKey())) {
                continue;
            }
            double sim = jaccard(a, tokens(other.getDescription()));
            if (sim >= threshold) {
                out.add(new DuplicateMatch(other, Math.round(sim * 100) / 100.0));
            }
        }
        out.sort(Comparator.comparingDouble(DuplicateMatch::similarity).reversed());
        return out;
    }

    // ---- intelligent feature #3: suggested resolution from past fixes ----
    /**
     * Past incidents in the same category that were fixed and look similar, most similar first.
     * Their resolution text is offered to the technician as a starting point.
     */
    public List<DuplicateMatch> suggestResolutions(Incident incident, int max) {
        Set<String> a = tokens(incident.getTitle() + " " + incident.getDescription());
        List<DuplicateMatch> out = new ArrayList<>();
        for (Incident past : incidents.values()) {
            if (past.getId().equals(incident.getId()) || past.getResolution().isEmpty()) {
                continue;
            }
            if (!past.getCategory().equals(incident.getCategory())) {
                continue;
            }
            double sim = jaccard(a, tokens(past.getTitle() + " " + past.getDescription()));
            if (sim >= 0.2) {
                out.add(new DuplicateMatch(past, Math.round(sim * 100) / 100.0));
            }
        }
        out.sort(Comparator.comparingDouble(DuplicateMatch::similarity).reversed());
        return out.size() > max ? out.subList(0, max) : out;
    }

    private static Set<String> tokens(String text) {
        return Arrays.stream(text.toLowerCase().split("[^a-z]+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) inter.size() / union.size();
    }

    // ---- search ----
    /** Search with any combination of filters (see SearchCriteria). */
    public List<Incident> search(SearchCriteria criteria) {
        return incidents.values().stream().filter(criteria::matches).toList();
    }

    /** Shorthand for the five most common filters (pass null to ignore one). */
    public List<Incident> search(Status status, Priority priority, Category category,
            String assigneeId, String text) {
        return search(new SearchCriteria().status(status).priority(priority).category(category)
                .assignee(assigneeId).text(text));
    }

    // ---- SLA check ----
    public boolean slaBreached(String incidentId) throws IncidentNotFoundException {
        Incident inc = get(incidentId);
        if (inc.getPriority() == null) {
            return false;
        }
        return inc.getSla().isBreached(inc.getCreatedAt(), inc.getResolvedAt());
    }

    // ---- persistence ----
    /** Writes every incident, including changes made outside the manager (SLA state, links, escalation). */
    public void saveAll() {
        repository.saveAll(incidents.values());
    }

    public List<Incident> getAll() {
        return new ArrayList<>(incidents.values());
    }

    public List<Incident> getOpen() {
        List<Incident> open = new ArrayList<>();
        for (Incident i : incidents.values()) {
            if (i.isOpen()) {
                open.add(i);
            }
        }
        return open;
    }

    public Incident getIncident(String id) throws IncidentNotFoundException {
        return get(id);
    }

    private Incident get(String id) throws IncidentNotFoundException {
        Incident inc = incidents.get(id);
        if (inc == null) {
            throw new IncidentNotFoundException(id);
        }
        return inc;
    }
}
