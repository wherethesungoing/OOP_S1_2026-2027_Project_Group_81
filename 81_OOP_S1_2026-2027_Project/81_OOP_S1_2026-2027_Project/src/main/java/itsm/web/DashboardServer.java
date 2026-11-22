package itsm.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import itsm.auth.Account;
import itsm.auth.LoginThrottle;
import itsm.auth.UserDirectory;
import itsm.core.DuplicateMatch;
import itsm.core.IncidentManager;
import itsm.core.SearchCriteria;
import itsm.core.ServiceCatalog;
import itsm.domain.Administrator;
import itsm.domain.AuditEntry;
import itsm.domain.Category;
import itsm.domain.Employee;
import itsm.domain.Incident;
import itsm.domain.ItService;
import itsm.domain.Priority;
import itsm.domain.SLA;
import itsm.domain.SlaState;
import itsm.domain.Status;
import itsm.domain.Technician;
import itsm.domain.User;
import itsm.exception.ITSMException;
import itsm.exception.InvalidLoginException;
import itsm.exception.InvalidStateTransitionException;
import itsm.exception.StorageException;
import itsm.exception.UnauthorizedException;
import itsm.exception.ValidationException;
import itsm.monitor.SlaMonitor;
import itsm.notify.EmailNotifier;
import itsm.notify.InAppNotifier;
import itsm.notify.SmsNotifier;
import itsm.report.ReportService;
import itsm.storage.FileRepository;
import itsm.storage.SystemStore;
import itsm.strategy.AssignmentStrategy;
import itsm.strategy.RoundRobinStrategy;
import itsm.strategy.SkillBasedStrategy;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Wi-Fi Gaya? web app: a campus IT helpdesk built on the I-ITSM core.
 * Runs on the JDK's built-in HTTP server, so it needs no external libraries.
 * Start it with "bash run.sh web" and open http://localhost:8080
 *
 * Everything is saved under ./data and loaded again on the next start.
 * Start with "bash run.sh reset" to wipe the saved data and begin from the demo set-up.
 */
public class DashboardServer {

    private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("d MMM HH:mm");

    private final Object lock = new Object();
    private final SimClock clock = new SimClock();
    private final SessionStore sessions = new SessionStore();
    private final SystemStore systemStore;
    private final UserDirectory users;
    private final IncidentManager manager;
    private final SlaMonitor monitor;
    private final ReportService reports;
    private final LiveFeedNotifier feed;
    private final InAppNotifier inbox;
    /** For each user, how far into each ticket's history they have read: used to show "new message". */
    private final Map<String, Map<String, Integer>> seen = new HashMap<>();
    private final LoginThrottle throttle = new LoginThrottle();
    private final String dataDir;
    private HttpServer httpServer;
    private ScheduledExecutorService scheduler;
    private java.util.concurrent.ExecutorService workers;

    public static void main(String[] args) throws Exception {
        int port = 8080;
        boolean reset = false;
        for (String a : args) {
            if ("--reset".equals(a)) {
                reset = true;
            } else {
                port = Integer.parseInt(a);
            }
        }
        if (reset) {
            for (String f : List.of("system.dat", "incidents.dat", "incidents.csv")) {
                Files.deleteIfExists(Path.of("data", f));
            }
            System.out.println("Saved data cleared; starting from the demo set-up.");
        }
        DashboardServer app = new DashboardServer("data");
        app.start(port);
    }

    /**
     * Loads saved data from dataDir, or sets up the demo accounts and tickets on first run.
     * If saved data cannot be read (for example it was written by an older version), it is moved
     * to a backup folder and the app starts fresh instead of failing.
     */
    public DashboardServer(String dataDir) throws ITSMException {
        this.dataDir = dataDir;
        systemStore = new SystemStore(dataDir + "/system.dat");
        SystemStore.State saved;
        FileRepository repository;
        try {
            saved = systemStore.load();
            repository = new FileRepository(dataDir + "/incidents.csv");
        } catch (StorageException e) {
            Path backup = moveToBackup(dataDir);
            System.err.println("Saved data could not be read (" + e.getMessage() + ").");
            System.err.println("It was moved to " + backup + " and the helpdesk starts fresh.");
            saved = null;
            repository = new FileRepository(dataDir + "/incidents.csv");
        }
        boolean firstRun = (saved == null);

        ServiceCatalog catalog;
        AssignmentStrategy strategy;
        if (firstRun) {
            users = new UserDirectory();
            createStartingAccounts();
            catalog = ServiceCatalog.campusDefaults();
            strategy = new SkillBasedStrategy();
        } else {
            users = saved.users();
            renameDemoAccounts();
            catalog = saved.services();
            SLA.restore(saved.slaPolicy());
            Category.restore(saved.categories());
            strategy = "RoundRobinStrategy".equals(saved.strategy()) ? new RoundRobinStrategy() : new SkillBasedStrategy();
            clock.set(saved.clockOffsetMinutes());
            for (Map.Entry<String, Map<String, Integer>> e : saved.readUpTo().entrySet()) {
                seen.put(e.getKey(), new HashMap<>(e.getValue()));
            }
        }

        manager = new IncidentManager(repository, strategy, catalog);
        relinkUsers();
        monitor = new SlaMonitor(manager);
        reports = new ReportService(manager);
        feed = new LiveFeedNotifier(clock);
        inbox = new InAppNotifier(users::administrators, clock::now);
        if (!firstRun) {
            inbox.restore(saved.inboxes());
        }
        manager.subscribe(feed);
        manager.subscribe(inbox);
        manager.subscribe(new EmailNotifier());
        manager.subscribe(new SmsNotifier());

        if (firstRun) {
            seed();
        }
        persist();
    }

    private static Path moveToBackup(String dataDir) {
        Path dir = Path.of(dataDir);
        Path backup = dir.resolve("backup-" + System.currentTimeMillis());
        try {
            Files.createDirectories(backup);
            for (String f : List.of("system.dat", "incidents.dat", "incidents.csv")) {
                if (Files.exists(dir.resolve(f))) {
                    Files.move(dir.resolve(f), backup.resolve(f));
                }
            }
        } catch (IOException e) {
            throw new StorageException("Could not move unreadable data out of the way in " + dir, e);
        }
        return backup;
    }

    /**
     * The demo user for each workspace on the sign-in page, which fills these in when clicked.
     * They are demo passwords for a project, not real credentials.
     */
    static final String STUDENT_USER = "harshit";
    static final String STUDENT_PASSWORD = "gvgB56UtQk";
    static final String STAFF_USER = "harshitha";
    static final String STAFF_PASSWORD = "46JhbvEvcK";
    static final String ADMIN_USER = "harshini";
    static final String ADMIN_PASSWORD = "72r0B2HVWz";

    /** The accounts the app starts with. Passwords are stored only as salted hashes. */
    private void createStartingAccounts() throws ValidationException {
        users.addAccount(ADMIN_USER, ADMIN_PASSWORD, new Administrator("A1", "Harshini", "harshini@campus.example"));
        users.addAccount(STAFF_USER, STAFF_PASSWORD,
                new Technician("T1", "Harshitha", "harshitha@campus.example", List.of("Network")));
        users.addAccount("website", "website123",
                new Technician("T2", "Website technician", "website@campus.example", List.of("Website", "Application", "Authentication")));
        users.addAccount("hostel", "hostel123",
                new Technician("T3", "Hostel technician", "hostel@campus.example", List.of("Hostel", "Hardware")));
        users.addAccount(STUDENT_USER, STUDENT_PASSWORD, new Employee("S1", "Harshit", "harshit@campus.example"));
        users.addAccount("student2", "student123", new Employee("S2", "Second student", "student2@campus.example"));
        users.addAccount("student3", "student123", new Employee("S3", "Third student", "student3@campus.example"));
    }

    /**
     * Data saved by an earlier version has the old demo accounts (student, network, admin).
     * Turn them into the current demo users, keeping each user id so their tickets stay with them.
     */
    private void renameDemoAccounts() throws ValidationException {
        if (users.findByUsername("student") instanceof Employee s && !users.hasUsername(STUDENT_USER)) {
            users.replaceAccount("student", STUDENT_USER, STUDENT_PASSWORD,
                    new Employee(s.getUserId(), "Harshit", "harshit@campus.example"));
        }
        if (users.findByUsername("network") instanceof Technician t && !users.hasUsername(STAFF_USER)) {
            users.replaceAccount("network", STAFF_USER, STAFF_PASSWORD,
                    new Technician(t.getUserId(), "Harshitha", "harshitha@campus.example", t.getSkills(), t.getCapacity()));
        }
        if (users.findByUsername("admin") instanceof Administrator a && !users.hasUsername(ADMIN_USER)) {
            users.replaceAccount("admin", ADMIN_USER, ADMIN_PASSWORD,
                    new Administrator(a.getUserId(), "Harshini", "harshini@campus.example"));
        }
    }

    /**
     * After loading, incidents and accounts were read from separate files, so point each
     * incident at the live account objects and recount every technician's workload.
     */
    private void relinkUsers() {
        for (Technician t : users.technicians()) {
            t.resetWorkload();
        }
        for (Incident inc : manager.getAll()) {
            if (inc.getAssignee() != null && users.findById(inc.getAssignee().getUserId()) instanceof Technician live) {
                inc.relinkAssignee(live);
                if (inc.isOpen()) {
                    live.assignOne();
                }
            }
            if (inc.getReporter() != null && users.findById(inc.getReporter().getUserId()) != null) {
                inc.setReporter(users.findById(inc.getReporter().getUserId()));
            }
        }
    }

    /** A few tickets so the app is not empty on first start, including one fixed earlier. */
    private void seed() throws ITSMException {
        User student = users.findById("S1");

        Incident past = raise(student, "wifi", "Hostel block A, common room",
                "Wi-Fi keeps disconnecting in the hostel block A common room", 2, 40);
        manager.advance(past.getId(), Status.IN_PROGRESS, past.getAssignee());
        manager.resolve(past.getId(), past.getAssignee(),
                "Restarted the access point and moved it to a less crowded Wi-Fi channel.");
        manager.advance(past.getId(), Status.CLOSED, student);
        manager.giveFeedback(past.getId(), student, 5, "Fixed within the hour");

        raise(student, "lms", "Online", "Quiz page on LMS times out when I press submit", 3, 300);

        Incident library = raise(student, "campus-network", "Main library, 1st floor",
                "Internet is very slow across the main library", 2, 300);
        manager.advance(library.getId(), Status.IN_PROGRESS, library.getAssignee());
        manager.addNote(library.getId(), library.getAssignee(), "Uplink switch in the library is at 95% load; checking for a loop");

        Incident room = raise(student, "hostel-room", "Hostel block C, room 214", "LAN port in my room is not working", 1, 1);
        manager.advance(room.getId(), Status.IN_PROGRESS, room.getAssignee());
        manager.resolve(room.getId(), room.getAssignee(), "Replaced the faulty LAN socket and re-crimped the cable.");

        raise(student, "website", "Online", "Campus website shows an error on the admissions page", 2, 1500);

        raise(student, "wifi", "Hostel block D, 2nd floor", "Wi-Fi keeps dropping in hostel block D second floor", 2, 40);
    }

    /** A student raises a ticket: the system sets its priority and sends it to a team straight away. */
    private Incident raise(User reporter, String serviceKey, String location, String text, int urgency, int people)
            throws ITSMException {
        ItService service = manager.getCatalog().find(serviceKey);
        // The location is part of the description, so reports from the same place are matched as one problem.
        Incident inc = new Incident(text, text + " " + location, service.getCategory(), service.getKey(), urgency, people);
        inc.setReporter(reporter);
        inc.setLocation(location);
        manager.submit(inc);
        manager.assign(inc.getId(), users.technicians());
        return inc;
    }

    /** Saves incidents, accounts, services and settings. */
    private void persist() {
        manager.saveAll();
        Map<String, Map<String, Integer>> read = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> e : seen.entrySet()) {
            read.put(e.getKey(), new HashMap<>(e.getValue()));
        }
        systemStore.save(new SystemStore.State(users, manager.getCatalog(), manager.getStrategyName(), SLA.policy(),
                Category.values(), inbox.snapshot(), read, clock.offset()));
    }

    /** Starts serving; returns the port in use (pass 0 to pick any free port, as the tests do). */
    public int start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer = server;
        server.createContext("/", this::handlePage);
        route(server, "/api/login", false, true, this::login);
        route(server, "/api/register", false, true, this::register);
        route(server, "/api/logout", false, true, this::logout);
        route(server, "/api/state", true, false, (ex, user, f) -> stateJson(user, f));
        route(server, "/api/history", true, false, this::history);
        route(server, "/api/ticket", true, false, this::ticketPage);
        route(server, "/api/report", true, true, this::report);
        route(server, "/api/action", true, true, this::action);
        route(server, "/api/time", true, true, this::skipTime);
        route(server, "/api/strategy", true, true, this::strategy);
        route(server, "/api/admin", true, true, this::admin);
        workers = Executors.newFixedThreadPool(4);
        server.setExecutor(workers);
        server.start();

        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "sla-monitor");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(this::checkSlas, 1, 2, TimeUnit.SECONDS);

        int actual = server.getAddress().getPort();
        System.out.println("IT Helpdesk is running at http://localhost:" + actual + "   (press Ctrl+C to stop)");
        System.out.println("Data is saved in the '" + dataDir + "' folder.");
        return actual;
    }

    /** Stops serving (used by the tests). */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (workers != null) {
            workers.shutdownNow();
        }
    }

    private void checkSlas() {
        try {
            synchronized (lock) {
                if (!monitor.check(clock.now()).isEmpty()) {
                    persist();
                }
            }
        } catch (RuntimeException e) {
            System.err.println("SLA monitor error: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- routing

    /** One API endpoint: form or query values in, JSON out. */
    @FunctionalInterface
    private interface Handler {
        String handle(HttpExchange ex, User user, Map<String, String> form) throws Exception;
    }

    /**
     * Registers an endpoint with the shared rules: POST for changes, a signed-in session where
     * needed, one lock around the system, saving after every change, and one place that turns
     * exceptions into messages for the user.
     */
    private void route(HttpServer server, String path, boolean needsLogin, boolean changes, Handler handler) {
        server.createContext(path, ex -> {
            try {
                if (changes && !"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                    send(ex, 405, "application/json", err("Use POST"));
                    return;
                }
                Map<String, String> form = changes ? form(ex) : query(ex);
                String body;
                synchronized (lock) {
                    User user = currentUser(ex);
                    if (needsLogin && user == null) {
                        send(ex, 401, "application/json", "{\"ok\":false,\"auth\":false,\"error\":\"Please sign in.\"}");
                        return;
                    }
                    body = handler.handle(ex, user, form);
                    if (changes) {
                        persist();
                    }
                }
                send(ex, 200, "application/json", body);
            } catch (UnauthorizedException e) {
                send(ex, 200, "application/json", err("Not allowed: " + e.getMessage()));
            } catch (InvalidStateTransitionException e) {
                send(ex, 200, "application/json", err("That step isn't allowed at this stage of the ticket ("
                        + e.getMessage() + ")."));
            } catch (ITSMException | IllegalArgumentException e) {
                send(ex, 200, "application/json", err(e.getMessage()));
            } catch (StorageException e) {
                System.err.println(e.getMessage());
                send(ex, 500, "application/json", err("The change could not be saved (storage error). Please try again."));
            } catch (Exception e) {
                e.printStackTrace();
                send(ex, 500, "application/json", err("Something went wrong on the server."));
            }
        });
    }

    private User currentUser(HttpExchange ex) {
        User u = users.findById(sessions.userId(cookie(ex, "sid")));
        return (u != null && u.isActive()) ? u : null;
    }

    private void handlePage(HttpExchange ex) throws IOException {
        if (!"/".equals(ex.getRequestURI().getPath())) {
            send(ex, 404, "text/plain", "Not found");
            return;
        }
        send(ex, 200, "text/html", PAGE);
    }

    // ---------------------------------------------------------------- auth

    private String login(HttpExchange ex, User ignored, Map<String, String> f) throws InvalidLoginException {
        String username = f.getOrDefault("username", "");
        throttle.check(username);
        User user;
        try {
            user = users.login(username, f.getOrDefault("password", ""));
        } catch (InvalidLoginException e) {
            throttle.failed(username);
            throw e;
        }
        throttle.succeeded(username);
        // The sign-in page has a tab per role; an account can only sign in under its own role.
        String wanted = f.getOrDefault("role", "");
        if (!wanted.isEmpty() && !wanted.equals(roleKey(user))) {
            String actual = roleKey(user).equals("tech") ? "Support staff" : roleKey(user).equals("admin") ? "Admin" : "Student";
            String article = actual.equals("Admin") ? "an " : "a ";
            throw new InvalidLoginException("This is " + article + actual.toLowerCase(Locale.ROOT) + " account. Choose the " + actual + " workspace to sign in.");
        }
        startSession(ex, user);
        return ok("Welcome, " + user.getName() + ".");
    }

    private String register(HttpExchange ex, User ignored, Map<String, String> f) throws ValidationException {
        Employee e = users.register(f.get("username"), f.get("name"), f.get("email"), f.getOrDefault("password", ""));
        startSession(ex, e);
        return ok("Account created. Welcome, " + e.getName() + ".");
    }

    private void startSession(HttpExchange ex, User user) {
        sessions.end(cookie(ex, "sid"));
        String token = sessions.create(user.getUserId());
        ex.getResponseHeaders().add("Set-Cookie", "sid=" + token + "; Path=/; HttpOnly; SameSite=Strict");
    }

    private String logout(HttpExchange ex, User ignored, Map<String, String> f) {
        sessions.end(cookie(ex, "sid"));
        ex.getResponseHeaders().add("Set-Cookie", "sid=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict");
        return ok("Signed out.");
    }

    // ---------------------------------------------------------------- tickets

    private String report(HttpExchange ex, User user, Map<String, String> f) throws ITSMException {
        if (!(user instanceof Employee)) {
            throw new UnauthorizedException("Tickets are raised by students. Sign in as a student to raise one.");
        }
        String text = f.getOrDefault("text", "").trim();
        if (text.isEmpty()) {
            throw new ValidationException("Please describe what is not working.");
        }
        if (text.length() > 300) {
            throw new ValidationException("Keep the description under 300 characters.");
        }
        String location = f.getOrDefault("location", "").trim();
        if (location.isEmpty()) {
            location = "Not given";
        }
        if (location.length() > 80) {
            throw new ValidationException("Keep the location under 80 characters.");
        }
        int urgency = clamp(parse(f.get("urgency"), 2), 1, 3);
        int people = clamp(parse(f.get("people"), 1), 1, 5000);
        Incident inc = raise(user, f.getOrDefault("service", "other"), location, text, urgency, people);
        String message = "Ticket " + inc.getId() + " raised. Priority " + inc.getPriority().name().toLowerCase(Locale.ROOT)
                + ", sent to " + assigneeName(inc) + ".";
        if (!inc.getRelatedIds().isEmpty()) {
            message += " Looks like the same problem as " + String.join(", ", inc.getRelatedIds()) + ".";
        }
        if (inc.isEscalated()) {
            message += " Escalated to the CCIT admin as a major incident.";
        }
        return ok(message);
    }

    private String action(HttpExchange ex, User user, Map<String, String> f) throws ITSMException {
        Incident inc = manager.getIncident(f.getOrDefault("id", ""));
        String id = inc.getId();
        String text = f.getOrDefault("text", "");
        switch (f.getOrDefault("action", "")) {
            case "start":
                manager.advance(id, Status.IN_PROGRESS, user);
                return ok(user.getName() + " picked up " + id + ".");
            case "note":
                manager.addNote(id, user, text);
                return ok("Note added to " + id + ".");
            case "ask":
                manager.requestInfo(id, user, text);
                return ok("Message sent to the student.");
            case "resolve":
                manager.resolve(id, user, text);
                return ok(id + " marked fixed. The student has been asked to confirm.");
            case "close":
                if (user instanceof Technician) {
                    throw new UnauthorizedException("Only the student who raised this ticket can confirm the fix and close it");
                }
                manager.advance(id, Status.CLOSED, user);
                return ok(user instanceof Employee ? id + " closed. Thanks for confirming." : id + " closed.");
            case "reopen":
                if (!text.isBlank()) {
                    manager.addComment(id, user, text);   // what is still wrong, so the technician knows
                }
                manager.advance(id, Status.REOPENED, user);
                return ok(id + " reopened and sent back to " + assigneeName(inc) + ".");
            case "comment":
                manager.addComment(id, user, text);
                return ok("Information added to " + id + ".");
            case "reply":
                manager.answerInfo(id, user, text);
                return ok("Reply sent to " + assigneeName(inc) + ".");
            case "message":
                // The reporter's message box: it answers CCIT's question if there is one, otherwise adds information.
                if (inc.isWaitingForReporter()) {
                    manager.answerInfo(id, user, text);
                } else {
                    manager.addComment(id, user, text);
                }
                return ok("Message sent to " + assigneeName(inc) + ".");
            case "feedback":
                manager.giveFeedback(id, user, parse(f.get("rating"), 0), text);
                return ok("Thanks for rating the fix.");
            case "priority": {
                Priority p = enumOrNull(Priority.class, f.get("priority"));
                manager.changePriority(id, p, user, text);
                return ok("Priority of " + id + " changed to " + pretty(p) + ".");
            }
            case "reassign": {
                User to = users.findById(f.getOrDefault("to", ""));
                if (!(to instanceof Technician)) {
                    throw new ValidationException("Choose a technician");
                }
                manager.reassign(id, (Technician) to, user);
                return ok(id + " moved to " + to.getName() + ".");
            }
            case "service": {
                ItService service = manager.getCatalog().find(f.getOrDefault("service", ""));
                manager.changeService(id, service, user);
                return ok(id + " moved to " + service.getName() + ".");
            }
            default:
                throw new ValidationException("Unknown action");
        }
    }

    private String history(HttpExchange ex, User user, Map<String, String> f) throws ITSMException {
        Incident inc = manager.getIncident(f.getOrDefault("id", ""));
        if (!canSee(inc, user)) {
            throw new UnauthorizedException("You can only see the history of your own tickets");
        }
        StringBuilder b = new StringBuilder("{\"ok\":true,\"id\":").append(Json.str(inc.getId()))
                .append(",\"title\":").append(Json.str(inc.getTitle())).append(",\"entries\":[");
        List<AuditEntry> audit = inc.getAudit();
        for (int k = 0; k < audit.size(); k++) {
            AuditEntry e = audit.get(k);
            if (k > 0) {
                b.append(',');
            }
            b.append(new Obj()
                    .put("time", e.getTimestamp().format(DAY_TIME))
                    .put("actor", e.getActor())
                    .put("action", e.getAction())
                    .put("from", e.getFromState() == null ? "" : e.getFromState().label())
                    .put("to", e.getToState() == null ? "" : e.getToState().label())
                    .end());
        }
        return b.append("]}").toString();
    }

    private String skipTime(HttpExchange ex, User user, Map<String, String> f) {
        int minutes = clamp(parse(f.get("minutes"), 5), 1, 600);
        clock.advance(minutes);
        List<String> alerts = monitor.check(clock.now());
        String message = "Skipped ahead " + minutes + " min.";
        if (!alerts.isEmpty()) {
            message += " " + alerts.size() + " new alert(s): see Activity.";
        }
        return ok(message);
    }

    private String strategy(HttpExchange ex, User user, Map<String, String> f) throws UnauthorizedException {
        boolean takeTurns = "turns".equals(f.get("name"));
        manager.setStrategy(takeTurns ? new RoundRobinStrategy() : new SkillBasedStrategy(), user);
        return ok("New tickets will now be assigned: " + strategyLabel() + ".");
    }

    // ---------------------------------------------------------------- administration

    private String admin(HttpExchange ex, User user, Map<String, String> f) throws ITSMException {
        if (!user.canConfigure()) {
            throw new UnauthorizedException("Only the CCIT admin can change settings");
        }
        switch (f.getOrDefault("op", "")) {
            case "addTech": {
                List<String> skills = new ArrayList<>();
                for (String s : f.getOrDefault("skills", "").split(",")) {
                    if (!s.isBlank()) {
                        skills.add(s.trim());
                    }
                }
                Technician t = users.addTechnician(user, f.get("username"), f.get("name"), f.get("email"),
                        f.getOrDefault("password", ""), skills);
                return ok(t.getName() + " added as a technician (" + t.getUserId() + ").");
            }
            case "setActive": {
                boolean active = "true".equals(f.get("active"));
                users.setActive(user, f.get("userId"), active);
                if (active) {
                    return ok("Account enabled.");
                }
                return ok("Account disabled: they can no longer sign in or receive tickets." + handOver(f.get("userId"), user));
            }
            case "addCategory": {
                Category c = Category.add(f.get("name"));
                return ok("Category \"" + c.label() + "\" added. You can now use it for services and technician skills.");
            }
            case "addService": {
                ItService s = manager.getCatalog().add(f.get("name"), category(f.get("category")),
                        parse(f.get("criticality"), 0));
                return ok("Service \"" + s.getName() + "\" added. Students can now report problems with it.");
            }
            case "setCriticality": {
                ItService s = manager.getCatalog().find(f.getOrDefault("key", ""));
                s.setCriticality(parse(f.get("criticality"), 0));
                return ok(s.getName() + " criticality set to " + s.getCriticality() + ". New tickets use it.");
            }
            case "setSla": {
                Priority p = enumOrNull(Priority.class, f.get("priority"));
                SLA.configure(p, parse(f.get("response"), 0), parse(f.get("resolution"), 0));
                return ok("SLA for " + p + " updated.");
            }
            default:
                throw new ValidationException("Unknown setting");
        }
    }

    /** When a technician is disabled, their open tickets go to whoever the assignment strategy picks. */
    private String handOver(String technicianId, User admin) throws ITSMException {
        int moved = 0;
        int stuck = 0;
        for (Incident inc : manager.getOpen()) {
            if (inc.getAssignee() != null && inc.getAssignee().getUserId().equals(technicianId)) {
                try {
                    manager.reassign(inc.getId(), users.technicians(), admin);
                    moved++;
                } catch (itsm.exception.NoAvailableTechnicianException e) {
                    stuck++;
                }
            }
        }
        String msg = moved == 0 ? "" : " Their " + moved + " open ticket(s) were moved to other technicians.";
        if (stuck > 0) {
            msg += " " + stuck + " ticket(s) could not be moved because no other technician is available.";
        }
        return msg;
    }

    private static String pretty(Priority p) {
        return p == null ? "" : p.name().charAt(0) + p.name().substring(1).toLowerCase(Locale.ROOT);
    }

    private static Category category(String value) throws ValidationException {
        Category c = Category.find(value);
        if (c == null) {
            throw new ValidationException("Choose a category");
        }
        return c;
    }

    // ---------------------------------------------------------------- what each role sees and can do

    private static String roleKey(User user) {
        if (user instanceof Administrator) {
            return "admin";
        }
        if (user instanceof Technician) {
            return "tech";
        }
        return "student";
    }

    /** Students see their own tickets, technicians the tickets assigned to them, the admin everything. */
    private static boolean canSee(Incident inc, User user) {
        if (user instanceof Administrator) {
            return true;
        }
        if (user instanceof Technician) {
            return user.equals(inc.getAssignee());
        }
        return user.equals(inc.getReporter());
    }

    /** Buttons to show for a ticket. The server still checks every action when it is clicked. */
    private static List<String[]> actionsFor(Incident inc, User user) {
        List<String[]> actions = new ArrayList<>();
        Status status = inc.getStatus();
        if (user instanceof Employee) {
            if (inc.isWaitingForReporter()) {
                actions.add(new String[] {"reply", "Reply to CCIT", "primary"});
            }
            if (status == Status.RESOLVED) {
                actions.add(new String[] {"close", "Yes, it's fixed", "primary"});
                actions.add(new String[] {"reopen", "Still broken", "ghost"});
            } else if (inc.isOpen()) {
                actions.add(new String[] {"comment", "Add information", "ghost"});
            }
            if (status == Status.CLOSED && !inc.hasFeedback()) {
                actions.add(new String[] {"feedback", "Rate the fix", "ghost"});
            }
        } else {
            boolean admin = user instanceof Administrator;
            if (status == Status.ASSIGNED || status == Status.REOPENED) {
                actions.add(new String[] {"start", "Pick up", "primary"});
            }
            if (status == Status.IN_PROGRESS) {
                actions.add(new String[] {"note", "Add note", "ghost"});
                actions.add(new String[] {"ask", "Ask student", "ghost"});
                actions.add(new String[] {"resolve", "Mark as fixed", "primary"});
            }
            if (!admin && status == Status.RESOLVED) {
                actions.add(new String[] {"close", "Close", "locked"});
            }
            if (admin && status == Status.RESOLVED) {
                actions.add(new String[] {"close", "Close", "primary"});
                actions.add(new String[] {"reopen", "Reopen", "ghost"});
            }
            if (admin && inc.isOpen()) {
                actions.add(new String[] {"close", "Close now", "warn"});
            }
        }
        return actions;
    }

    // ---------------------------------------------------------------- state JSON

    private String stateJson(User user, Map<String, String> f) {
        LocalDateTime now = clock.now();
        String role = roleKey(user);
        Obj o = new Obj()
                .put("ok", true)
                .raw("me", meJson(user))
                .put("clock", clockText())
                .put("strategy", strategyLabel())
                .raw("services", servicesJson())
                .raw("categories", stringList(Category.values().stream().map(Category::label).toList()))
                .raw("inbox", stringList(inbox.inbox(user.getUserId())));

        SearchCriteria criteria = criteria(f);
        Status statusFilter = enumOrNull(Status.class, f.get("status"));
        List<String> rows = new ArrayList<>();
        List<Incident> found = manager.search(criteria);
        for (int k = found.size() - 1; k >= 0; k--) {
            Incident inc = found.get(k);
            if (!canSee(inc, user)) {
                continue;
            }
            // Technicians' default view hides closed tickets; filtering by "Closed" shows them.
            if (user instanceof Technician && inc.getStatus() == Status.CLOSED && statusFilter != Status.CLOSED) {
                continue;
            }
            rows.add(ticketJson(inc, user, now));
        }
        o.raw("tickets", "[" + String.join(",", rows) + "]");
        // Counts for the navigation badges, over everything this user can see (not just the current filter).
        int attention = 0;
        int openCount = 0;
        for (Incident inc : manager.getAll()) {
            if (!canSee(inc, user)) {
                continue;
            }
            if (inc.isOpen()) {
                openCount++;
            }
            if (user instanceof Technician && inc.getStatus() == Status.CLOSED) {
                continue;   // a technician's lists hide closed tickets, so the badge must too
            }
            if (!needsAction(inc, user).isEmpty() || unread(inc, user) > 0) {
                attention++;
            }
        }
        o.raw("counts", new Obj().put("attention", attention).put("open", openCount).end());

        if (!"student".equals(role)) {
            o.raw("feed", stringList(feed.recent()));
        }
        if ("admin".equals(role)) {
            o.raw("kpis", kpiJson())
                    .raw("byService", countsJson(reports.byService(), true))
                    .raw("byPriority", priorityJson())
                    .raw("users", usersJson())
                    .raw("sla", slaJson())
                    .raw("audit", auditJson());
        }
        o.put("role", role);
        return o.end();
    }

    private SearchCriteria criteria(Map<String, String> f) {
        SearchCriteria c = new SearchCriteria()
                .text(f.get("q"))
                .status(enumOrNull(Status.class, f.get("status")))
                .priority(enumOrNull(Priority.class, f.get("priority")))
                .service(f.get("service"))
                .slaState(enumOrNull(SlaState.class, f.get("sla")))
                .assignee(f.get("tech"));
        try {
            String from = f.get("from");
            if (from != null && !from.isBlank()) {
                c.from(LocalDate.parse(from));
            }
        } catch (DateTimeParseException ignored) {
            // an invalid date is ignored rather than failing the whole page
        }
        return c;
    }

    private String meJson(User user) {
        Obj o = new Obj()
                .put("id", user.getUserId())
                .put("name", user.getName())
                .put("username", users.usernameOf(user))
                .put("role", roleKey(user))
                .put("roleName", roleName(user))
                .put("email", user.getEmail());
        if (user instanceof Technician t) {
            o.put("workload", t.getWorkload()).put("capacity", t.getCapacity())
                    .raw("skills", stringList(t.getSkills()));
        }
        return o.end();
    }

    private static String roleName(User user) {
        if (user instanceof Administrator) {
            return "CCIT admin";
        }
        if (user instanceof Technician) {
            return "Technician";
        }
        return "Student";
    }

    private String ticketJson(Incident inc, User user, LocalDateTime now) {
        Obj o = new Obj()
                .put("id", inc.getId())
                .put("title", inc.getTitle())
                .put("service", serviceName(inc.getServiceKey()))
                .put("location", inc.getLocation())
                .put("ago", ago(inc.getCreatedAt(), now))
                .put("priority", inc.getPriority() == null ? "-" : inc.getPriority().name())
                .put("status", statusFor(inc, user))
                .put("stateKey", inc.isWaitingForReporter() && inc.getStatus() == Status.IN_PROGRESS ? "WAITING" : inc.getStatus().name())
                .put("category", inc.getCategory().label())
                .put("urgency", inc.getUrgency())
                .put("people", inc.getAffectedUsers())
                .put("created", inc.getCreatedAt().format(DAY_TIME))
                .put("needsAction", needsAction(inc, user))
                .put("unread", unread(inc, user))
                .put("updated", ago(lastUpdate(inc), now))
                .put("updatedAt", lastUpdate(inc).toString())
                .put("sla", inc.getSlaState().name())
                .put("slaPct", slaPercent(inc, now))
                .put("timeLeft", timeLeft(inc, now))
                .put("response", responseText(inc, now))
                .put("assignee", assigneeName(inc))
                .put("reporter", inc.getReporter() == null ? "" : inc.getReporter().getName())
                .put("note", lastNote(inc))
                .put("question", inc.getPendingQuestion())
                .put("resolution", inc.getResolution())
                .put("feedback", inc.hasFeedback() ? inc.getFeedbackRating() + "/5"
                        + (inc.getFeedbackComment().isEmpty() ? "" : ": " + inc.getFeedbackComment()) : "")
                .put("escalated", inc.isEscalated())
                .put("reason", reasonText(inc))
                .raw("related", stringList(inc.getRelatedIds()));

        List<String> suggestions = new ArrayList<>();
        if (!(user instanceof Employee) && inc.isOpen()) {
            for (DuplicateMatch m : manager.suggestResolutions(inc, 2)) {
                suggestions.add(new Obj().put("id", m.incident().getId())
                        .put("text", m.incident().getResolution())
                        .put("match", Math.round(m.similarity() * 100)).end());
            }
        }
        o.raw("suggestions", "[" + String.join(",", suggestions) + "]");

        List<String> actions = new ArrayList<>();
        for (String[] a : actionsFor(inc, user)) {
            actions.add(new Obj().put("key", a[0]).put("label", a[1]).put("style", a[2]).end());
        }
        return o.raw("actions", "[" + String.join(",", actions) + "]").end();
    }

    private String kpiJson() {
        Map<Status, Long> byStatus = reports.byStatus();
        long fixed = byStatus.get(Status.RESOLVED) + byStatus.get(Status.CLOSED);
        return new Obj()
                .put("total", manager.getAll().size())
                .put("open", reports.openCount())
                .put("critical", manager.getOpen().stream().filter(i -> i.getPriority() == Priority.CRITICAL).count())
                .put("late", reports.slaCount(SlaState.AT_RISK))
                .put("missed", reports.slaCount(SlaState.BREACHED))
                .put("escalated", reports.escalatedCount())
                .put("fixed", fixed)
                .put("avgFix", formatMinutes(Math.round(reports.avgResolutionMinutes())))
                .put("rating", reports.averageRating() == 0 ? "-" : String.format(Locale.ROOT, "%.1f / 5", reports.averageRating()))
                .end();
    }

    private String countsJson(Map<String, Long> counts, boolean serviceNames) {
        List<String> items = new ArrayList<>();
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            items.add(new Obj().put("name", serviceNames ? serviceName(e.getKey()) : e.getKey())
                    .put("count", e.getValue()).end());
        }
        return "[" + String.join(",", items) + "]";
    }

    private String priorityJson() {
        Map<String, Long> named = new java.util.LinkedHashMap<>();
        for (Map.Entry<Priority, Long> e : reports.byPriority().entrySet()) {
            named.put(e.getKey().name(), e.getValue());
        }
        return countsJson(named, false);
    }

    private String servicesJson() {
        Map<String, Long> counts = reports.byService();
        List<String> items = new ArrayList<>();
        for (ItService s : manager.getCatalog().all()) {
            items.add(new Obj().put("key", s.getKey()).put("name", s.getName())
                    .put("category", s.getCategory().name()).put("categoryName", s.getCategory().label())
                    .put("criticality", s.getCriticality())
                    .put("count", counts.getOrDefault(s.getKey(), 0L)).end());
        }
        return "[" + String.join(",", items) + "]";
    }

    private String usersJson() {
        Map<Technician, Long> load = reports.workload(users.technicians());
        List<String> items = new ArrayList<>();
        for (Account a : users.accounts()) {
            User u = a.getUser();
            Obj o = new Obj().put("id", u.getUserId()).put("username", a.getUsername()).put("name", u.getName())
                    .put("email", u.getEmail()).put("role", roleName(u)).put("active", u.isActive())
                    .put("admin", u instanceof Administrator);
            if (u instanceof Technician t) {
                o.put("workload", load.getOrDefault(t, 0L)).put("capacity", t.getCapacity())
                        .put("skills", String.join(", ", t.getSkills()));
            }
            items.add(o.end());
        }
        return "[" + String.join(",", items) + "]";
    }

    private static String slaJson() {
        List<String> items = new ArrayList<>();
        for (Map.Entry<Priority, int[]> e : SLA.policy().entrySet()) {
            items.add(new Obj().put("priority", e.getKey().name())
                    .put("response", e.getValue()[0]).put("resolution", e.getValue()[1]).end());
        }
        return "[" + String.join(",", items) + "]";
    }

    /** The most recent activity across every ticket, newest first. */
    private String auditJson() {
        record Row(String incidentId, AuditEntry entry) { }
        List<Row> rows = new ArrayList<>();
        for (Incident inc : manager.getAll()) {
            for (AuditEntry e : inc.getAudit()) {
                rows.add(new Row(inc.getId(), e));
            }
        }
        rows.sort(Comparator.comparing((Row r) -> r.entry().getTimestamp()).reversed());
        List<String> items = new ArrayList<>();
        for (Row r : rows.subList(0, Math.min(80, rows.size()))) {
            items.add(new Obj().put("time", r.entry().getTimestamp().format(DAY_TIME)).put("id", r.incidentId())
                    .put("actor", r.entry().getActor()).put("action", r.entry().getAction())
                    .put("from", r.entry().getFromState() == null ? "" : r.entry().getFromState().label())
                    .put("to", r.entry().getToState() == null ? "" : r.entry().getToState().label()).end());
        }
        return "[" + String.join(",", items) + "]";
    }

    private static String stringList(List<String> values) {
        StringBuilder b = new StringBuilder("[");
        for (int k = 0; k < values.size(); k++) {
            if (k > 0) {
                b.append(',');
            }
            b.append(Json.str(values.get(k)));
        }
        return b.append(']').toString();
    }

    /** Small JSON object builder. */
    private static final class Obj {
        private final StringBuilder b = new StringBuilder("{");

        private Obj key(String k) {
            if (b.length() > 1) {
                b.append(',');
            }
            b.append(Json.str(k)).append(':');
            return this;
        }

        Obj put(String k, String v) {
            key(k).b.append(Json.str(v == null ? "" : v));
            return this;
        }

        Obj put(String k, long v) {
            key(k).b.append(v);
            return this;
        }

        Obj put(String k, double v) {
            key(k).b.append(String.format(Locale.ROOT, "%.1f", v));
            return this;
        }

        Obj put(String k, boolean v) {
            key(k).b.append(v);
            return this;
        }

        Obj raw(String k, String json) {
            key(k).b.append(json);
            return this;
        }

        String end() {
            return b.append('}').toString();
        }
    }


    // ---------------------------------------------------------------- the ticket page

    /** One ticket with its conversation. Opening it marks its messages as read for this user. */
    private String ticketPage(HttpExchange ex, User user, Map<String, String> f) throws ITSMException {
        Incident inc = manager.getIncident(f.getOrDefault("id", ""));
        if (!canSee(inc, user)) {
            throw new UnauthorizedException("You can only open your own tickets");
        }
        boolean staff = !(user instanceof Employee);
        int seenUpTo = readUpTo(inc, user);
        List<String> thread = new ArrayList<>();
        List<AuditEntry> audit = inc.getAudit();
        for (int k = 0; k < audit.size(); k++) {
            AuditEntry e = audit.get(k);
            AuditEntry.Kind kind = e.getKind();
            if (!staff && !kind.visibleToReporter()) {
                continue;
            }
            if (kind == AuditEntry.Kind.STATUS && e.getToState() == Status.ASSIGNED) {
                continue;   // "Sent to <team>" already says this
            }
            String actor = e.getActor();
            if (kind == AuditEntry.Kind.CREATED && inc.getReporter() != null) {
                actor = inc.getReporter().getName();
            }
            boolean system = "system".equals(actor) || "sla-monitor".equals(actor);
            thread.add(new Obj()
                    .put("kind", kind.name())
                    .put("actor", system ? "" : actor)
                    .put("mine", actor.equals(user.getName()))
                    .put("isNew", k >= seenUpTo && kind.isMessage() && !actor.equals(user.getName()))
                    .put("time", e.getTimestamp().format(DAY_TIME))
                    .put("text", e.getText())
                    .end());
        }
        boolean canMessage = inc.getStatus() != Status.CLOSED
                && (staff ? (user.canConfigure() || user.equals(inc.getAssignee())) : user.equals(inc.getReporter()));
        String body = new Obj()
                .put("ok", true)
                .raw("ticket", ticketJson(inc, user, clock.now()))
                .raw("thread", "[" + String.join(",", thread) + "]")
                .put("canMessage", canMessage)
                .put("mode", staff ? "staff" : "student")
                .put("waiting", inc.isWaitingForReporter())
                .put("canChangePriority", inc.isOpen() && (user.canConfigure() || user.equals(inc.getAssignee())))
                .put("canReassign", inc.isOpen() && user.canConfigure())
                .put("priorityKey", inc.getPriority() == null ? "" : inc.getPriority().name())
                .put("serviceKey", inc.getServiceKey())
                .put("assigneeId", inc.getAssignee() == null ? "" : inc.getAssignee().getUserId())
                .end();
        seen.computeIfAbsent(user.getUserId(), k -> new HashMap<>()).put(inc.getId(), audit.size());
        return body;
    }

    /** Messages from other people on this ticket that this user has not opened yet. */
    private int unread(Incident inc, User user) {
        if (user instanceof Administrator) {
            return 0;   // the admin oversees every conversation but is not a party to them
        }
        int from = readUpTo(inc, user);
        List<AuditEntry> audit = inc.getAudit();
        int count = 0;
        for (int k = from; k < audit.size(); k++) {
            AuditEntry e = audit.get(k);
            if (e.getKind().isMessage() && !e.getActor().equals(user.getName())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Where unread messages start: after the last time this user opened the ticket, and after
     * anything they did on it themselves (anyone who acted on a ticket has seen what came before).
     */
    private int readUpTo(Incident inc, User user) {
        int from = seen.getOrDefault(user.getUserId(), Map.of()).getOrDefault(inc.getId(), 0);
        List<AuditEntry> audit = inc.getAudit();
        for (int k = audit.size() - 1; k >= from; k--) {
            if (audit.get(k).getActor().equals(user.getName())) {
                return k + 1;
            }
        }
        return from;
    }

    private static LocalDateTime lastUpdate(Incident inc) {
        List<AuditEntry> audit = inc.getAudit();
        return audit.isEmpty() ? inc.getCreatedAt() : audit.get(audit.size() - 1).getTimestamp();
    }

    /** What this user needs to do on the ticket, in a few words, or "" when nothing is waiting on them. */
    private static String needsAction(Incident inc, User user) {
        Status s = inc.getStatus();
        if (user instanceof Employee) {
            if (inc.isWaitingForReporter()) {
                return "Reply to CCIT's question";
            }
            if (s == Status.RESOLVED) {
                return "Confirm the fix";
            }
            if (s == Status.CLOSED && !inc.hasFeedback()) {
                return "Rate how it was handled";
            }
            return "";
        }
        if (user instanceof Technician) {
            if (s == Status.ASSIGNED || s == Status.REOPENED) {
                return s == Status.REOPENED ? "Reopened: pick it up again" : "Pick up this ticket";
            }
            return "";
        }
        if (inc.isOpen() && inc.getSlaState() == SlaState.BREACHED) {
            return "Deadline missed";
        }
        if (inc.isOpen() && inc.isEscalated()) {
            return "Escalated to you";
        }
        return "";
    }

    /** Status in words that fit the person reading it. */
    private static String statusFor(Incident inc, User user) {
        boolean student = user instanceof Employee;
        String team = assigneeName(inc);
        switch (inc.getStatus()) {
            case NEW:
                return "New";
            case ASSIGNED:
                return student ? "Sent to " + team + ", not started yet" : "Not picked up yet";
            case IN_PROGRESS:
                if (inc.isWaitingForReporter()) {
                    return student ? "Waiting for your reply" : "Waiting for the student's reply";
                }
                return student ? team + " is working on it" : "In progress";
            case RESOLVED:
                return student ? "Fixed, please confirm" : "Fixed, waiting for the student to confirm";
            case REOPENED:
                return student ? "Reopened, back with " + team : "Reopened by the student";
            case CLOSED:
                return "Closed";
            default:
                return inc.getStatus().label();
        }
    }

    // ---------------------------------------------------------------- display text

    private String clockText() {
        long offset = clock.offset();
        return offset == 0 ? "real time" : offset + " min ahead";
    }

    private String strategyLabel() {
        return "RoundRobinStrategy".equals(manager.getStrategyName()) ? "Take turns" : "Match by skill";
    }

    private String serviceName(String key) {
        return manager.getCatalog().nameOf(key);
    }

    private static String assigneeName(Incident inc) {
        return inc.getAssignee() == null ? "Not assigned yet" : inc.getAssignee().getName();
    }

    private static String ago(LocalDateTime created, LocalDateTime now) {
        long minutes = Duration.between(created, now).toMinutes();
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + " min ago";
        }
        if (minutes < 60 * 48) {
            return (minutes / 60) + " h ago";
        }
        return created.format(DAY_TIME);
    }

    private static double slaPercent(Incident inc, LocalDateTime now) {
        if (inc.getPriority() == null) {
            return 0;
        }
        LocalDateTime end = (!inc.isOpen() && inc.getResolvedAt() != null) ? inc.getResolvedAt() : now;
        return Math.max(0, inc.getSla().elapsedFraction(inc.getCreatedAt(), end) * 100);
    }

    private static String timeLeft(Incident inc, LocalDateTime now) {
        if (!inc.isOpen()) {
            return "Done";
        }
        if (inc.getPriority() == null) {
            return "";
        }
        long left = inc.getSla().getResolutionMins() - Duration.between(inc.getCreatedAt(), now).toMinutes();
        return left < 0 ? formatMinutes(-left) + " late" : formatMinutes(left) + " left";
    }

    /** Response-time tracking against the SLA's response target. */
    private static String responseText(Incident inc, LocalDateTime now) {
        if (inc.getPriority() == null) {
            return "";
        }
        SLA sla = inc.getSla();
        if (inc.getAcknowledgedAt() != null) {
            long took = Duration.between(inc.getCreatedAt(), inc.getAcknowledgedAt()).toMinutes();
            boolean met = took <= sla.getResponseMins();
            return "Picked up in " + formatMinutes(took) + (met ? " (target met)" : " (target " + formatMinutes(sla.getResponseMins()) + " missed)");
        }
        if (inc.getStatus() == Status.CLOSED) {
            return "";
        }
        boolean missed = sla.responseMissed(inc.getCreatedAt(), null, now);
        return missed ? "Not picked up: response target " + formatMinutes(sla.getResponseMins()) + " missed"
                : "Pick up within " + formatMinutes(sla.getResponseMins());
    }

    private static String formatMinutes(long minutes) {
        return minutes >= 120 ? (minutes / 60) + " h" : minutes + " min";
    }

    private static String lastNote(Incident inc) {
        String note = "";
        for (AuditEntry entry : inc.getAudit()) {
            if (entry.getAction().startsWith("Note: ")) {
                note = entry.getAction().substring(6);
            }
        }
        return note;
    }

    private String reasonText(Incident inc) {
        String reason = inc.getEscalationReason();
        if (reason == null) {
            return "";
        }
        return reason.replace(" on " + inc.getServiceKey(), " on " + serviceName(inc.getServiceKey()));
    }

    // ---------------------------------------------------------------- HTTP helpers

    private static String ok(String message) {
        return "{\"ok\":true,\"message\":" + Json.str(message) + "}";
    }

    private static String err(String message) {
        return "{\"ok\":false,\"error\":" + Json.str(message == null ? "Something went wrong." : message) + "}";
    }

    private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String cookie(HttpExchange ex, String name) {
        List<String> headers = ex.getRequestHeaders().get("Cookie");
        if (headers == null) {
            return null;
        }
        for (String header : headers) {
            for (String part : header.split(";")) {
                String p = part.trim();
                if (p.startsWith(name + "=")) {
                    return p.substring(name.length() + 1);
                }
            }
        }
        return null;
    }

    private static Map<String, String> form(HttpExchange ex) throws IOException {
        byte[] bytes = ex.getRequestBody().readNBytes(64 * 1024);
        return parsePairs(new String(bytes, StandardCharsets.UTF_8));
    }

    private static Map<String, String> query(HttpExchange ex) {
        String raw = ex.getRequestURI().getRawQuery();
        return parsePairs(raw == null ? "" : raw);
    }

    private static Map<String, String> parsePairs(String text) {
        Map<String, String> values = new HashMap<>();
        for (String pair : text.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            String value = eq < 0 ? "" : pair.substring(eq + 1);
            values.put(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return values;
    }

    private static int parse(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int clamp(int value, int low, int high) {
        return Math.max(low, Math.min(high, value));
    }

    private static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.getResponseHeaders().set("X-Frame-Options", "DENY");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    // ---------------------------------------------------------------- page
    private static final String PAGE = Page.HTML;
}
