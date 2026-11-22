package itsm;

import itsm.auth.LoginThrottle;
import itsm.auth.UserDirectory;
import itsm.domain.ItService;
import itsm.domain.TimeSource;
import itsm.exception.DuplicateIncidentException;
import itsm.exception.SlaConfigurationException;
import itsm.web.DashboardServer;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import itsm.core.SearchCriteria;
import itsm.core.ServiceCatalog;
import itsm.domain.SlaState;
import itsm.exception.InvalidLoginException;
import itsm.exception.ServiceNotFoundException;
import itsm.exception.ValidationException;
import itsm.notify.InAppNotifier;
import itsm.storage.SystemStore;
import itsm.exception.UnauthorizedException;
import itsm.monitor.SlaMonitor;
import itsm.report.ReportService;
import itsm.web.Json;
import itsm.core.DuplicateMatch;
import itsm.core.IncidentManager;
import itsm.domain.Administrator;
import itsm.domain.Category;
import itsm.domain.Employee;
import itsm.domain.Incident;
import itsm.domain.Priority;
import itsm.domain.SLA;
import itsm.domain.Status;
import itsm.domain.Technician;
import itsm.domain.User;
import itsm.exception.IncidentNotFoundException;
import itsm.exception.InvalidStateTransitionException;
import itsm.exception.NoAvailableTechnicianException;
import itsm.notify.DashboardNotifier;
import itsm.storage.FileRepository;
import itsm.storage.InMemoryRepository;
import itsm.strategy.RoundRobinStrategy;
import itsm.strategy.SkillBasedStrategy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free test suite (62 cases: unit tests, plus integration tests that run the real web server). Mirrors JUnit style so it can be
 * ported to JUnit 5 later by turning each test(...) into an @Test method.
 */
public class TestRunner {

    interface Check {
        void run() throws Exception;
    }

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        test("01 new incident starts NEW", () -> {
            Incident i = new Incident("t", "d", Category.APPLICATION);
            check(i.getStatus() == Status.NEW && i.getId().startsWith("INC-"), "should be NEW with INC- id");
        });
        test("02 prioritization CRITICAL", () -> {
            IncidentManager m = mgr();
            Incident i = new Incident("p", "down", Category.APPLICATION, "lms", 3, 600);
            m.report(i);
            check(i.getPriority() == Priority.CRITICAL, "expected CRITICAL, got " + i.getPriority());
        });
        test("03 prioritization LOW", () -> {
            IncidentManager m = mgr();
            Incident i = new Incident("p", "minor", Category.STORAGE, "other", 1, 1);
            m.report(i);
            check(i.getPriority() == Priority.LOW, "expected LOW, got " + i.getPriority());
        });
        test("04 legal transition chain to CLOSED", () -> {
            Incident i = new Incident("t", "d", Category.APPLICATION);
            i.transitionTo(Status.ASSIGNED);
            i.transitionTo(Status.IN_PROGRESS);
            i.transitionTo(Status.RESOLVED);
            i.transitionTo(Status.CLOSED);
            check(i.getStatus() == Status.CLOSED, "should be CLOSED");
        });
        test("05 illegal transition throws", () ->
                expectThrows(InvalidStateTransitionException.class,
                () -> new Incident("t", "d", Category.APPLICATION).transitionTo(Status.CLOSED)));
        test("06 reopen path", () -> {
            Incident i = new Incident("t", "d", Category.APPLICATION);
            i.transitionTo(Status.ASSIGNED);
            i.transitionTo(Status.IN_PROGRESS);
            i.transitionTo(Status.RESOLVED);
            i.transitionTo(Status.REOPENED);
            check(i.getStatus() == Status.REOPENED, "should be REOPENED");
        });
        test("07 round robin alternates", () -> {
            RoundRobinStrategy s = new RoundRobinStrategy();
            List<Technician> t = techs();
            Incident i = new Incident("t", "d", Category.APPLICATION);
            List<String> picks = new ArrayList<>();
            for (int k = 0; k < 3; k++) {
                picks.add(s.assign(i, t).getUserId());
            }
            check(picks.equals(List.of("T1", "T2", "T1")), "got " + picks);
        });
        test("08 skill-based matches category", () -> {
            Technician t = new SkillBasedStrategy().assign(new Incident("t", "d", Category.DATABASE), techs());
            check(t.getUserId().equals("T1"), "expected T1, got " + t.getUserId());
        });
        test("09 skill-based falls back to least loaded", () -> {
            List<Technician> t = techs();
            t.get(1).assignOne();   // Ravi is busier
            Technician chosen = new SkillBasedStrategy().assign(new Incident("t", "d", Category.NETWORK), t);
            check(chosen.getUserId().equals("T1"), "expected T1, got " + chosen.getUserId());
        });
        test("10 no technician throws", () ->
                expectThrows(NoAvailableTechnicianException.class,
                () -> new RoundRobinStrategy().assign(new Incident("t", "d", Category.APPLICATION), List.of())));
        test("11 duplicate detection flags similar", () -> {
            IncidentManager m = mgr();
            Incident a = new Incident("a", "users cannot complete payment transactions", Category.APPLICATION, "lms", 1, 1);
            Incident b = new Incident("b", "users cannot complete payment transactions now", Category.APPLICATION, "lms", 1, 1);
            m.report(a);
            m.report(b);
            boolean found = false;
            for (DuplicateMatch d : m.findDuplicates(b)) {
                if (d.incident() == a) {
                    found = true;
                }
            }
            check(found, "a should be flagged as duplicate of b");
        });
        test("12 duplicate ignores other service", () -> {
            IncidentManager m = mgr();
            Incident a = new Incident("a", "payment transactions failing", Category.APPLICATION, "lms", 1, 1);
            Incident b = new Incident("b", "email not working", Category.NETWORK, "email", 1, 1);
            m.report(a);
            m.report(b);
            check(m.findDuplicates(b).isEmpty(), "should find no duplicates");
        });
        test("13 search by status", () -> {
            IncidentManager m = mgr();
            Incident i = new Incident("t", "d", Category.APPLICATION);
            m.report(i);
            m.assign(i.getId(), techs());
            m.advance(i.getId(), Status.IN_PROGRESS);
            m.advance(i.getId(), Status.RESOLVED);
            m.advance(i.getId(), Status.CLOSED);
            check(m.search(Status.CLOSED, null, null, null, null).contains(i), "closed incident not found");
        });
        test("14 search by text and category", () -> {
            IncidentManager m = mgr();
            Incident i = new Incident("Payment issue", "cannot pay", Category.APPLICATION);
            m.report(i);
            check(m.search(null, null, null, null, "payment").contains(i), "text search failed");
            check(m.search(null, null, Category.APPLICATION, null, null).contains(i), "category search failed");
        });
        test("15 observer is notified", () -> {
            IncidentManager m = mgr();
            DashboardNotifier dash = new DashboardNotifier();
            m.subscribe(dash);
            Incident i = new Incident("t", "d", Category.APPLICATION);
            m.report(i);
            check(dash.getEvents().contains("REPORTED:" + i.getId()), "dashboard missed REPORTED");
        });
        test("16 unknown incident throws", () ->
                expectThrows(IncidentNotFoundException.class,
                () -> mgr().advance("INC-9999", Status.ASSIGNED)));
        test("17 SLA breach detection", () -> {
            SLA sla = new SLA(Priority.CRITICAL);   // 60 minutes
            check(sla.isBreached(LocalDateTime.now().minusHours(2), null), "should be breached");
            check(!sla.isBreached(LocalDateTime.now(), null), "should not be breached yet");
        });
        test("18 audit trail records transitions", () -> {
            Incident i = new Incident("t", "d", Category.APPLICATION);
            i.transitionTo(Status.ASSIGNED, "Lab support");
            boolean found = i.getAudit().stream()
                    .anyMatch(e -> e.getActor().equals("Lab support") && e.getAction().contains("Assigned"));
            check(found, "audit entry missing");
        });
        test("19 file repository persists to disk", () -> {
            Path tmp = Files.createTempFile("itsm", ".csv");
            FileRepository repo = new FileRepository(tmp.toString());
            Incident i = new Incident("t", "d", Category.APPLICATION);
            i.setPriority(Priority.HIGH);
            repo.save(i);
            List<String> lines = Files.readAllLines(tmp);
            check(lines.size() == 2 && lines.get(1).contains("HIGH"), "file content wrong: " + lines);
            Files.deleteIfExists(tmp);
            Files.deleteIfExists(repo.getDataFile());
        });
        test("20 user roles are polymorphic", () -> {
            List<User> users = List.of(new Employee("E1", "a", "a@o.com"),
                    new Technician("T1", "b", "b@o.com", List.of()),
                    new Administrator("A1", "c", "c@o.com"));
            List<String> roles = users.stream().map(User::role).toList();
            check(roles.equals(List.of("Employee", "Technician", "Administrator")), "got " + roles);
        });

        test("21 technician cannot close (authorization)", () -> {
            IncidentManager m = mgr();
            Incident i = new Incident("t", "d", Category.APPLICATION);
            m.report(i);
            m.assign(i.getId(), techs());
            Technician t = i.getAssignee();
            m.advance(i.getId(), Status.IN_PROGRESS, t);
            m.advance(i.getId(), Status.RESOLVED, t);
            expectThrows(UnauthorizedException.class, () -> m.advance(i.getId(), Status.CLOSED, t));
        });
        test("22 reporter verifies and closes", () -> {
            IncidentManager m = mgr();
            Incident i = new Incident("t", "d", Category.APPLICATION);
            m.report(i);
            m.assign(i.getId(), techs());
            Technician t = i.getAssignee();
            m.advance(i.getId(), Status.IN_PROGRESS, t);
            m.advance(i.getId(), Status.RESOLVED, t);
            m.advance(i.getId(), Status.CLOSED, new Employee("E1", "a", "a@o.com"));
            check(i.getStatus() == Status.CLOSED, "should be CLOSED");
        });
        test("23 only admin changes strategy", () -> {
            IncidentManager m = mgr();
            expectThrows(UnauthorizedException.class,
                    () -> m.setStrategy(new SkillBasedStrategy(), new Employee("E1", "a", "a@o.com")));
            m.setStrategy(new SkillBasedStrategy(), new Administrator("A1", "c", "c@o.com"));
            check(m.getStrategyName().equals("SkillBasedStrategy"), "got " + m.getStrategyName());
        });
        test("24 SLA at risk at 75% (predictive)", () -> {
            IncidentManager m = mgr();
            Incident i = new Incident("p", "down", Category.APPLICATION, "lms", 3, 600);
            m.report(i);
            new SlaMonitor(m).check(i.getCreatedAt().plusMinutes(50));
            check(i.getSlaState() == SlaState.AT_RISK, "expected AT_RISK, got " + i.getSlaState());
        });
        test("25 SLA breached past deadline", () -> {
            IncidentManager m = mgr();
            Incident i = new Incident("p", "down", Category.APPLICATION, "lms", 3, 600);
            m.report(i);
            new SlaMonitor(m).check(i.getCreatedAt().plusMinutes(70));
            check(i.getSlaState() == SlaState.BREACHED, "expected BREACHED, got " + i.getSlaState());
            check(i.isEscalated(), "a breached incident should be escalated");
        });
        test("26 unacknowledged critical escalates", () -> {
            IncidentManager m = mgr();
            Incident a = new Incident("p", "down", Category.APPLICATION, "lms", 3, 600);
            Incident b = new Incident("q", "outage elsewhere", Category.NETWORK, "wifi", 3, 600);
            m.report(a);
            m.report(b);
            m.assign(b.getId(), techs());
            m.advance(b.getId(), Status.IN_PROGRESS, b.getAssignee());
            new SlaMonitor(m).check(a.getCreatedAt().plusMinutes(20));
            check(a.isEscalated(), "unacknowledged critical should escalate");
            check(!b.isEscalated(), "acknowledged incident should not escalate");
        });
        test("27 duplicates are correlated both ways", () -> {
            IncidentManager m = mgr();
            Incident a = new Incident("a", "users cannot complete payment transactions", Category.APPLICATION, "lms", 1, 1);
            Incident b = new Incident("b", "users cannot complete payment transactions now", Category.APPLICATION, "lms", 1, 1);
            m.report(a);
            m.report(b);
            check(b.getRelatedIds().contains(a.getId()) && a.getRelatedIds().contains(b.getId()), "not linked");
        });
        test("28 three related reports become a major incident", () -> {
            IncidentManager m = mgr();
            Incident a = new Incident("a", "users cannot complete payment transactions", Category.APPLICATION, "lms", 1, 1);
            Incident b = new Incident("b", "users cannot complete payment transactions now", Category.APPLICATION, "lms", 1, 1);
            Incident c = new Incident("c", "users cannot complete payment transactions today", Category.APPLICATION, "lms", 1, 1);
            m.report(a);
            m.report(b);
            check(!a.isEscalated(), "two reports should not escalate yet");
            m.report(c);
            check(a.isEscalated() && b.isEscalated() && c.isEscalated(), "all three should be escalated");
        });
        test("29 analytics counts by status", () -> {
            IncidentManager m = mgr();
            Incident a = new Incident("a", "x", Category.APPLICATION);
            Incident b = new Incident("b", "y", Category.DATABASE);
            m.report(a);
            m.report(b);
            m.assign(b.getId(), techs());
            ReportService r = new ReportService(m);
            check(r.byStatus().get(Status.NEW) == 1 && r.byStatus().get(Status.ASSIGNED) == 1, "got " + r.byStatus());
        });
        test("30 JSON output escapes quotes", () -> {
            check(Json.str("say \"hi\"").equals("\"say \\\"hi\\\"\""), "got " + Json.str("say \"hi\""));
        });

        // ---- login, registration and user management ----
        test("31 login with the right password; wrong password refused", () -> {
            UserDirectory d = new UserDirectory();
            d.register("riya", "Riya", "riya@campus.example", "secret12");
            check(d.login("RIYA", "secret12").getName().equals("Riya"), "login failed");
            expectThrows(InvalidLoginException.class, () -> d.login("riya", "wrong"));
            expectThrows(InvalidLoginException.class, () -> d.login("nobody", "secret12"));
        });
        test("32 registration validates input", () -> {
            UserDirectory d = new UserDirectory();
            expectThrows(ValidationException.class, () -> d.register("ab", "A", "a@b.co", "secret12"));       // username too short
            expectThrows(ValidationException.class, () -> d.register("abc", "A", "not-an-email", "secret12"));
            expectThrows(ValidationException.class, () -> d.register("abc", "A", "a@b.co", "123"));            // password too short
            d.register("abc", "A", "a@b.co", "secret12");
            expectThrows(ValidationException.class, () -> d.register("ABC", "B", "b@b.co", "secret12"));     // taken
        });
        test("33 disabled account cannot sign in; only admin can disable", () -> {
            UserDirectory d = new UserDirectory();
            Administrator admin = new Administrator("A1", "Admin", "a@o.com");
            d.addAccount("admin", "admin123", admin);
            Employee e = d.register("riya", "Riya", "riya@campus.example", "secret12");
            expectThrows(UnauthorizedException.class, () -> d.setActive(e, e.getUserId(), false));
            d.setActive(admin, e.getUserId(), false);
            expectThrows(InvalidLoginException.class, () -> d.login("riya", "secret12"));
            expectThrows(ValidationException.class, () -> d.setActive(admin, "A1", false));   // not yourself
        });
        test("34 only an admin can add a technician, with known skills", () -> {
            UserDirectory d = new UserDirectory();
            Administrator admin = new Administrator("A1", "Admin", "a@o.com");
            expectThrows(UnauthorizedException.class, () -> d.addTechnician(new Employee("S1", "s", "s@o.com"),
                    "sec", "Security", "sec@o.com", "secret12", List.of("Network")));
            expectThrows(ValidationException.class, () -> d.addTechnician(admin, "sec", "Security", "sec@o.com", "secret12", List.of()));
            Technician t = d.addTechnician(admin, "sec", "Security", "sec@o.com", "secret12", List.of("Network"));
            check(d.technicians().contains(t) && d.login("sec", "secret12") == t, "technician not added");
        });

        // ---- persistence ----
        test("35 incidents survive a restart, and new ids continue", () -> {
            Path dir = Files.createTempDirectory("itsm");
            String csv = dir.resolve("incidents.csv").toString();
            IncidentManager m1 = new IncidentManager(new FileRepository(csv), new RoundRobinStrategy());
            Incident i = new Incident("Wi-Fi down", "wifi down in block A", Category.NETWORK, "wifi", 2, 40);
            m1.report(i);
            m1.assign(i.getId(), techs());
            m1.addNote(i.getId(), i.getAssignee(), "checking the switch");
            IncidentManager m2 = new IncidentManager(new FileRepository(csv), new RoundRobinStrategy());
            Incident loaded = m2.getIncident(i.getId());
            check(loaded.getStatus() == Status.ASSIGNED && loaded.getPriority() == i.getPriority(), "state lost");
            check(loaded.getAudit().size() == i.getAudit().size(), "history lost");
            Incident next = new Incident("t", "d", Category.APPLICATION);
            check(next.getId().compareTo(i.getId()) > 0, "id reused: " + next.getId());
        });
        test("36 accounts and settings survive a restart", () -> {
            Path file = Files.createTempDirectory("itsm").resolve("system.dat");
            UserDirectory d = new UserDirectory();
            d.register("riya", "Riya", "riya@campus.example", "secret12");
            ServiceCatalog c = ServiceCatalog.campusDefaults();
            c.add("Campus VPN", Category.NETWORK, 4);
            new SystemStore(file.toString()).save(new SystemStore.State(d, c, "RoundRobinStrategy", SLA.policy()));
            SystemStore.State s = new SystemStore(file.toString()).load();
            check(s.users().login("riya", "secret12").getName().equals("Riya"), "account lost");
            check(s.services().contains("campus-vpn") && s.strategy().equals("RoundRobinStrategy"), "settings lost");
        });

        // ---- business rules on who may act ----
        test("37 a technician cannot work on another technician's ticket", () -> {
            IncidentManager m = mgr();
            List<Technician> t = techs();
            Incident i = new Incident("t", "d", Category.DATABASE);
            m.report(i);
            m.assign(i.getId(), t);
            Technician other = t.get(0).equals(i.getAssignee()) ? t.get(1) : t.get(0);
            expectThrows(UnauthorizedException.class, () -> m.advance(i.getId(), Status.IN_PROGRESS, other));
            expectThrows(UnauthorizedException.class, () -> m.addNote(i.getId(), other, "hi"));
        });
        test("38 only the reporter can close their ticket", () -> {
            IncidentManager m = mgr();
            Employee owner = new Employee("S1", "Owner", "o@o.com");
            Incident i = new Incident("t", "d", Category.APPLICATION);
            i.setReporter(owner);
            m.report(i);
            m.assign(i.getId(), techs());
            m.advance(i.getId(), Status.IN_PROGRESS, i.getAssignee());
            m.resolve(i.getId(), i.getAssignee(), "restarted it");
            expectThrows(UnauthorizedException.class,
                    () -> m.advance(i.getId(), Status.CLOSED, new Employee("S2", "Someone else", "x@o.com")));
            m.advance(i.getId(), Status.CLOSED, owner);
            check(i.getStatus() == Status.CLOSED, "owner could not close");
        });
        test("39 unavailable or full technicians are skipped", () -> {
            List<Technician> t = techs();
            t.get(0).setActive(false);
            Technician chosen = new SkillBasedStrategy().assign(new Incident("t", "d", Category.DATABASE), t);
            check(chosen.getUserId().equals("T2"), "disabled technician was chosen");
            Technician busy = new Technician("T9", "Busy", "b@o.com", List.of("Database"), 1);
            busy.assignOne();
            expectThrows(NoAvailableTechnicianException.class,
                    () -> new RoundRobinStrategy().assign(new Incident("t", "d", Category.DATABASE), List.of(busy)));
        });

        // ---- employee and technician features ----
        test("40 resolving needs resolution details, which are kept", () -> {
            IncidentManager m = mgr();
            Incident i = new Incident("t", "d", Category.APPLICATION);
            m.report(i);
            m.assign(i.getId(), techs());
            m.advance(i.getId(), Status.IN_PROGRESS, i.getAssignee());
            expectThrows(ValidationException.class, () -> m.resolve(i.getId(), i.getAssignee(), "  "));
            m.resolve(i.getId(), i.getAssignee(), "Cleared the cache");
            check(i.getStatus() == Status.RESOLVED && i.getResolution().equals("Cleared the cache"), "resolution lost");
        });
        test("41 technician asks for information; reporter replies", () -> {
            IncidentManager m = mgr();
            Employee owner = new Employee("S1", "Owner", "o@o.com");
            Incident i = new Incident("t", "d", Category.APPLICATION);
            i.setReporter(owner);
            m.report(i);
            m.assign(i.getId(), techs());
            expectThrows(ValidationException.class, () -> m.answerInfo(i.getId(), owner, "nothing was asked"));
            m.advance(i.getId(), Status.IN_PROGRESS, i.getAssignee());
            m.requestInfo(i.getId(), i.getAssignee(), "Which room?");
            check(i.isWaitingForReporter(), "question not recorded");
            m.answerInfo(i.getId(), owner, "Room 204");
            check(!i.isWaitingForReporter(), "reply did not clear the question");
        });
        test("42 feedback only after closing, rated 1 to 5, once", () -> {
            IncidentManager m = mgr();
            Employee owner = new Employee("S1", "Owner", "o@o.com");
            Incident i = new Incident("t", "d", Category.APPLICATION);
            i.setReporter(owner);
            m.report(i);
            m.assign(i.getId(), techs());
            expectThrows(ValidationException.class, () -> m.giveFeedback(i.getId(), owner, 5, ""));
            m.advance(i.getId(), Status.IN_PROGRESS, i.getAssignee());
            m.resolve(i.getId(), i.getAssignee(), "fixed");
            m.advance(i.getId(), Status.CLOSED, owner);
            expectThrows(ValidationException.class, () -> m.giveFeedback(i.getId(), owner, 6, ""));
            m.giveFeedback(i.getId(), owner, 4, "quick");
            check(i.getFeedbackRating() == 4, "rating not stored");
            expectThrows(ValidationException.class, () -> m.giveFeedback(i.getId(), owner, 5, "again"));
        });

        // ---- configuration ----
        test("43 service catalog: add, find, unknown service, criticality range", () -> {
            ServiceCatalog c = ServiceCatalog.campusDefaults();
            check(c.add("Campus VPN", Category.NETWORK, 4).getKey().equals("campus-vpn"), "wrong key");
            expectThrows(ValidationException.class, () -> c.add("campus vpn", Category.NETWORK, 4));   // duplicate
            expectThrows(ValidationException.class, () -> c.add("Library portal", Category.APPLICATION, 9));
            expectThrows(ServiceNotFoundException.class, () -> c.find("no-such-service"));
        });
        test("44 business criticality changes the calculated priority", () -> {
            IncidentManager m = mgr();
            Incident before = new Incident("p", "power socket broken", Category.HOSTEL, "hostel-room", 2, 40);
            m.report(before);
            m.getCatalog().find("hostel-room").setCriticality(5);
            Incident after = new Incident("p", "power socket broken again", Category.HOSTEL, "hostel-room", 2, 40);
            m.report(after);
            check(before.getPriority() == Priority.LOW && after.getPriority() == Priority.MEDIUM,   // score 2x2x1=4, then 5x2x1=10
                    "got " + before.getPriority() + " then " + after.getPriority());
        });
        test("45 SLA policy can be changed, with validation", () -> {
            try {
                expectThrows(ValidationException.class, () -> SLA.configure(Priority.HIGH, 300, 200));
                SLA.configure(Priority.HIGH, 10, 120);
                check(new SLA(Priority.HIGH).getResolutionMins() == 120 && new SLA(Priority.HIGH).getResponseMins() == 10,
                        "policy not applied");
            } finally {
                SLA.resetToDefaults();
            }
        });

        // ---- search, intelligence and notifications ----
        test("46 search by service, deadline state and date", () -> {
            IncidentManager m = mgr();
            Incident a = new Incident("a", "wifi slow", Category.NETWORK, "wifi", 3, 600);
            Incident b = new Incident("b", "lan port dead", Category.HOSTEL, "hostel-room", 1, 1);
            m.report(a);
            m.report(b);
            new SlaMonitor(m).check(a.getCreatedAt().plusMinutes(50));
            check(m.search(new SearchCriteria().service("hostel-room")).equals(List.of(b)), "service filter");
            check(m.search(new SearchCriteria().slaState(SlaState.AT_RISK)).equals(List.of(a)), "SLA filter");
            check(m.search(new SearchCriteria().from(java.time.LocalDate.now().plusDays(1))).isEmpty(), "date filter");
            check(m.search(new SearchCriteria().text(b.getId())).equals(List.of(b)), "id search");
        });
        test("47 suggested resolution comes from a similar past fix", () -> {
            IncidentManager m = mgr();
            Incident past = new Incident("Wi-Fi drops", "wifi keeps dropping in the library", Category.NETWORK, "wifi", 1, 1);
            m.report(past);
            m.assign(past.getId(), techs());
            m.advance(past.getId(), Status.IN_PROGRESS, past.getAssignee());
            m.resolve(past.getId(), past.getAssignee(), "Restarted the access point");
            Incident now = new Incident("Wi-Fi drops", "wifi keeps dropping in block D", Category.NETWORK, "wifi", 1, 1);
            m.report(now);
            List<DuplicateMatch> s = m.suggestResolutions(now, 2);
            check(!s.isEmpty() && s.get(0).incident() == past, "no suggestion");
        });
        test("48 in-app notifications reach the right people", () -> {
            IncidentManager m = mgr();
            Administrator admin = new Administrator("A1", "Admin", "a@o.com");
            InAppNotifier inbox = new InAppNotifier(() -> List.of(admin), LocalDateTime::now);
            m.subscribe(inbox);
            Employee owner = new Employee("S1", "Owner", "o@o.com");
            Incident i = new Incident("p", "down", Category.APPLICATION, "lms", 3, 600);
            i.setReporter(owner);
            m.report(i);
            m.assign(i.getId(), techs());
            new SlaMonitor(m).check(i.getCreatedAt().plusMinutes(70));
            check(!inbox.inbox("S1").isEmpty(), "reporter got nothing");
            check(!inbox.inbox(i.getAssignee().getUserId()).isEmpty(), "technician got nothing");
            check(inbox.inbox("A1").stream().anyMatch(n -> n.contains("escalated")), "admin missed the escalation");
        });

        // ---- staff changes, duplicates and configuration ----
        test("49 same person reporting the same problem twice is refused; another person is linked", () -> {
            IncidentManager m = mgr();
            Employee a = new Employee("S1", "A", "a@o.com");
            Employee b = new Employee("S2", "B", "b@o.com");
            Incident first = new Incident("w", "wifi keeps dropping in block D", Category.NETWORK, "wifi", 2, 40);
            first.setReporter(a);
            m.submit(first);
            Incident again = new Incident("w", "wifi keeps dropping in block D", Category.NETWORK, "wifi", 2, 40);
            again.setReporter(a);
            expectThrows(DuplicateIncidentException.class, () -> m.submit(again));
            Incident other = new Incident("w", "wifi keeps dropping in block D", Category.NETWORK, "wifi", 2, 40);
            other.setReporter(b);
            m.submit(other);
            check(other.getRelatedIds().contains(first.getId()), "second reporter's ticket should be linked");
        });
        test("50 admin reassigns a ticket; workload moves; rules enforced", () -> {
            IncidentManager m = mgr();
            List<Technician> t = techs();
            Administrator admin = new Administrator("A1", "Admin", "a@o.com");
            Incident i = new Incident("t", "d", Category.DATABASE);
            m.report(i);
            m.assign(i.getId(), t);
            Technician from = i.getAssignee();
            Technician to = from.equals(t.get(0)) ? t.get(1) : t.get(0);
            expectThrows(UnauthorizedException.class, () -> m.reassign(i.getId(), to, from));
            to.setActive(false);
            expectThrows(NoAvailableTechnicianException.class, () -> m.reassign(i.getId(), to, admin));
            to.setActive(true);
            m.reassign(i.getId(), to, admin);
            check(i.getAssignee() == to && to.getWorkload() == 1 && from.getWorkload() == 0, "workload not moved");
        });
        test("51 priority change: reason required, deadline recalculated, only assignee or admin", () -> {
            IncidentManager m = mgr();
            List<Technician> t = techs();
            Incident i = new Incident("p", "minor", Category.DATABASE, "other", 1, 1);
            m.report(i);
            m.assign(i.getId(), t);
            Technician other = t.get(0).equals(i.getAssignee()) ? t.get(1) : t.get(0);
            expectThrows(UnauthorizedException.class, () -> m.changePriority(i.getId(), Priority.CRITICAL, other, "urgent"));
            expectThrows(ValidationException.class, () -> m.changePriority(i.getId(), Priority.CRITICAL, i.getAssignee(), " "));
            m.changePriority(i.getId(), Priority.CRITICAL, i.getAssignee(), "Whole hostel affected");
            check(i.getPriority() == Priority.CRITICAL && i.getSla().getResolutionMins() == 60, "not recalculated");
        });
        test("52 admin moves a ticket to another service and category", () -> {
            IncidentManager m = mgr();
            Incident i = new Incident("t", "printer offline", Category.APPLICATION, "other", 1, 1);
            m.report(i);
            ItService printing = m.getCatalog().find("hostel-room");
            expectThrows(UnauthorizedException.class, () -> m.changeService(i.getId(), printing, new Employee("S1", "s", "s@o.com")));
            m.changeService(i.getId(), printing, new Administrator("A1", "Admin", "a@o.com"));
            check(i.getServiceKey().equals("hostel-room") && i.getCategory().equals(Category.HOSTEL), "not reclassified");
        });
        test("53 changing the SLA policy does not move existing deadlines", () -> {
            try {
                IncidentManager m = mgr();
                Incident i = new Incident("p", "down", Category.APPLICATION, "lms", 3, 600);   // CRITICAL, 60 min
                m.report(i);
                SLA.configure(Priority.CRITICAL, 5, 30);
                check(i.getSla().getResolutionMins() == 60, "existing ticket's SLA changed");
                Incident later = new Incident("p", "down again", Category.APPLICATION, "lms", 3, 600);
                m.report(later);
                check(later.getSla().getResolutionMins() == 30, "new ticket should use the new policy");
            } finally {
                SLA.resetToDefaults();
            }
        });
        test("54 admin adds a category; services, skills and assignment use it", () -> {
            Category cloud = Category.add("Cloud services " + System.nanoTime() % 100000);
            check(Category.values().contains(cloud) && Category.find(cloud.label()) == cloud, "not registered");
            expectThrows(ValidationException.class, () -> Category.add(cloud.label().toUpperCase()));
            ServiceCatalog c = ServiceCatalog.campusDefaults();
            ItService s = c.add("Cloud storage", cloud, 3);
            UserDirectory d = new UserDirectory();
            Technician t = d.addTechnician(new Administrator("A1", "Admin", "a@o.com"), "cloudy", "Cloud team",
                    "cloud@o.com", "secret12", List.of(cloud.label()));
            List<Technician> pool = new ArrayList<>(techs());
            pool.add(t);
            Technician chosen = new SkillBasedStrategy().assign(new Incident("t", "d", s.getCategory(), s.getKey(), 1, 1), pool);
            check(chosen == t, "skill match on the new category failed");
        });
        test("55 missing or invalid SLA configuration raises SlaConfigurationException", () -> {
            expectThrows(SlaConfigurationException.class, () -> new SLA(0, 10));
            java.util.Map<Priority, int[]> bad = new java.util.EnumMap<>(Priority.class);
            bad.put(Priority.HIGH, new int[] {300, 200});
            expectThrows(SlaConfigurationException.class, () -> SLA.restore(bad));
        });
        test("56 too many wrong passwords lock the username for a minute", () -> {
            long[] now = {0};
            LoginThrottle th = new LoginThrottle(() -> now[0]);
            for (int k = 0; k < LoginThrottle.MAX_FAILURES; k++) {
                th.check("riya");
                th.failed("riya");
            }
            expectThrows(InvalidLoginException.class, () -> th.check("Riya"));
            th.check("someone-else");
            now[0] += LoginThrottle.LOCK_MILLIS + 1;
            th.check("riya");
        });
        test("57 the demo clock moves report and pick-up times too", () -> {
            try {
                TimeSource.offsetMinutes(30);
                Incident i = new Incident("t", "d", Category.APPLICATION);
                check(i.getCreatedAt().isAfter(LocalDateTime.now().plusMinutes(29)), "createdAt ignored the demo clock");
            } finally {
                TimeSource.offsetMinutes(0);
            }
        });

        // ---- integration: the real web server over HTTP ----
        test("58 web: sign-in required; wrong password refused; right password works", () -> {
            Path dir = Files.createTempDirectory("itsm-web");
            DashboardServer server = new DashboardServer(dir.toString());
            try {
                Web w = new Web(server.start(0));
                check(w.get("/api/state").statusCode() == 401, "state should need sign-in");
                check(w.post("/api/login", "username", "harshit", "password", "nope").contains("\"ok\":false"), "wrong password accepted");
                check(w.post("/api/login", "username", "harshit", "password", "gvgB56UtQk").contains("\"ok\":true"), "login failed");
                check(w.get("/api/state").statusCode() == 200, "session cookie not working");
            } finally {
                server.stop();
            }
        });
        test("59 web: permissions are enforced by the server, not just the screen", () -> {
            Path dir = Files.createTempDirectory("itsm-web");
            DashboardServer server = new DashboardServer(dir.toString());
            try {
                int port = server.start(0);
                Web student = new Web(port);
                student.post("/api/login", "username", "harshit", "password", "gvgB56UtQk");
                check(student.post("/api/admin", "op", "addCategory", "name", "Hacked").contains("Not allowed"), "student used admin API");
                Web admin = new Web(port);
                admin.post("/api/login", "username", "harshini", "password", "72r0B2HVWz");
                String quiz = ticketIdWithTitle(admin.get("/api/state").body(), "Quiz page");   // assigned to the Website technician
                Web network = new Web(port);
                network.post("/api/login", "username", "harshitha", "password", "46JhbvEvcK");
                check(network.post("/api/action", "id", quiz, "action", "start").contains("Not allowed"), "other team's ticket was changed");
            } finally {
                server.stop();
            }
        });
        test("60 web: raising the same ticket twice is refused", () -> {
            Path dir = Files.createTempDirectory("itsm-web");
            DashboardServer server = new DashboardServer(dir.toString());
            try {
                Web w = new Web(server.start(0));
                w.post("/api/login", "username", "student2", "password", "student123");
                String[] form = {"service", "hostel-room", "location", "Block C, room 110", "text", "LAN port in my room is dead", "urgency", "1", "people", "1"};
                check(w.post("/api/report", form).contains("\"ok\":true"), "first report refused");
                check(w.post("/api/report", form).contains("already reported"), "duplicate accepted");
            } finally {
                server.stop();
            }
        });
        test("61 web: tickets and accounts survive a server restart", () -> {
            Path dir = Files.createTempDirectory("itsm-web");
            DashboardServer first = new DashboardServer(dir.toString());
            String id;
            try {
                Web w = new Web(first.start(0));
                w.post("/api/register", "username", "riya", "name", "Riya", "email", "riya@campus.example", "password", "secret12");
                String r = w.post("/api/report", "service", "email", "location", "Room 12", "text", "Cannot send email", "urgency", "2", "people", "1");
                Matcher mt = Pattern.compile("INC-[0-9]+").matcher(r);
                check(mt.find(), "no ticket id in " + r);
                id = mt.group();
            } finally {
                first.stop();
            }
            DashboardServer second = new DashboardServer(dir.toString());
            try {
                Web w = new Web(second.start(0));
                check(w.post("/api/login", "username", "riya", "password", "secret12").contains("\"ok\":true"), "account lost");
                check(w.get("/api/state").body().contains(id), "ticket lost after restart");
            } finally {
                second.stop();
            }
        });
        test("62 web: disabling a technician hands their open tickets to others", () -> {
            Path dir = Files.createTempDirectory("itsm-web");
            DashboardServer server = new DashboardServer(dir.toString());
            try {
                Web admin = new Web(server.start(0));
                admin.post("/api/login", "username", "harshini", "password", "72r0B2HVWz");
                String r = admin.post("/api/admin", "op", "setActive", "userId", "T1", "active", "false");
                check(r.contains("were moved to other technicians"), "tickets not handed over: " + r);
            } finally {
                server.stop();
            }
        });

        System.out.println();
        System.out.println(passed + "/" + (passed + failed) + " tests passed");
        if (failed > 0) {
            System.exit(1);
        }
    }


    /** A browser-like HTTP client with its own cookies (its own signed-in session). */
    private static final class Web {
        private final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        private final String base;

        Web(int port) {
            this.base = "http://localhost:" + port;
        }

        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        }

        String post(String path, String... keyValues) throws Exception {
            StringBuilder body = new StringBuilder();
            for (int k = 0; k + 1 < keyValues.length; k += 2) {
                if (body.length() > 0) {
                    body.append('&');
                }
                body.append(URLEncoder.encode(keyValues[k], StandardCharsets.UTF_8)).append('=')
                        .append(URLEncoder.encode(keyValues[k + 1], StandardCharsets.UTF_8));
            }
            HttpRequest r = HttpRequest.newBuilder(URI.create(base + path))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            return client.send(r, HttpResponse.BodyHandlers.ofString()).body();
        }
    }

    /** Finds the id of the ticket whose title starts with the given text in a state JSON reply. */
    private static String ticketIdWithTitle(String json, String title) {
        Matcher m = Pattern.compile("\"id\":\"(INC-[0-9]+)\",\"title\":\"" + Pattern.quote(title)).matcher(json);
        if (!m.find()) {
            throw new AssertionError("no ticket titled " + title);
        }
        return m.group(1);
    }

    // ---- helpers ----
    private static IncidentManager mgr() {
        return new IncidentManager(new InMemoryRepository(), new RoundRobinStrategy());
    }

    private static List<Technician> techs() {
        List<Technician> list = new ArrayList<>();
        list.add(new Technician("T1", "Lab support", "asha@org.com", List.of("Database")));
        list.add(new Technician("T2", "Apps team", "ravi@org.com", List.of("Application")));
        return list;
    }

    private static void test(String name, Check c) {
        try {
            c.run();
            passed++;
            System.out.println("PASS  " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("FAIL  " + name + "  ->  " + t);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectThrows(Class<? extends Throwable> type, Check c) {
        try {
            c.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) {
                return;
            }
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + t);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " but nothing was thrown");
    }
}
