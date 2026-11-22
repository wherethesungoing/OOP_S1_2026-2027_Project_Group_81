package itsm;

import itsm.core.DuplicateMatch;
import itsm.core.IncidentManager;
import itsm.domain.Administrator;
import itsm.domain.AuditEntry;
import itsm.domain.Category;
import itsm.domain.Employee;
import itsm.domain.Incident;
import itsm.domain.Status;
import itsm.domain.Technician;
import itsm.exception.ITSMException;
import itsm.monitor.SlaMonitor;
import itsm.notify.DashboardNotifier;
import itsm.notify.EmailNotifier;
import itsm.notify.SmsNotifier;
import itsm.report.ReportService;
import itsm.storage.FileRepository;
import itsm.strategy.RoundRobinStrategy;
import itsm.strategy.SkillBasedStrategy;
import java.util.List;

/** Console walk-through of the helpdesk: the brief's three workflows, plus guards, escalation and analytics. */
public class Main {

    public static void main(String[] args) throws Exception {
        java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("out/demo/incidents.dat"));   // fresh run each time
        IncidentManager manager = new IncidentManager(new FileRepository("out/demo/incidents.csv"), new SkillBasedStrategy());
        manager.subscribe(new EmailNotifier());
        manager.subscribe(new SmsNotifier());
        manager.subscribe(new DashboardNotifier());
        SlaMonitor monitor = new SlaMonitor(manager);

        Employee student = new Employee("S1", "Student", "student@campus.example");
        Administrator admin = new Administrator("A1", "CCIT admin", "admin@campus.example");
        List<Technician> teams = List.of(
                new Technician("T1", "Network technician", "network@campus.example", List.of("Network")),
                new Technician("T2", "Website technician", "website@campus.example", List.of("Website", "Application", "Authentication")),
                new Technician("T3", "Hostel technician", "hostel@campus.example", List.of("Hostel", "Hardware")));

        heading("Workflow 1: student raises a ticket -> priority set -> team assigned -> notified");
        Incident quiz = new Incident("LMS quiz will not submit", "Quiz page on LMS times out when I press submit",
                Category.WEBSITE, "lms", 3, 300);
        quiz.setReporter(student);
        manager.report(quiz);
        System.out.println("Priority set automatically: " + quiz.getPriority());
        Technician team = manager.assign(quiz.getId(), teams);
        System.out.println("Sent to: " + team.getName());

        heading("Workflow 2: team investigates -> SLA watched -> fixed -> student confirms");
        manager.advance(quiz.getId(), Status.IN_PROGRESS, team);
        manager.addNote(quiz.getId(), team, "Quiz server ran out of connections; raised the limit");
        for (String alert : monitor.check(quiz.getCreatedAt().plusMinutes(50))) {
            System.out.println("SLA monitor, 50 min later: " + alert);
        }
        manager.advance(quiz.getId(), Status.RESOLVED, team);
        manager.advance(quiz.getId(), Status.CLOSED, student);
        System.out.println("Final: " + quiz);

        heading("Workflow 3: same Wi-Fi problem reported 3 times -> linked -> escalated");
        String[] complaints = {
            "Wi-Fi keeps dropping in hostel block D second floor",
            "Wi-Fi dropping again in hostel block D second floor",
            "Hostel block D second floor Wi-Fi keeps dropping"
        };
        Incident firstReport = null;
        Incident lastReport = null;
        for (String complaint : complaints) {
            Incident report = new Incident("Wi-Fi dropping, block D", complaint, Category.HOSTEL, "wifi", 2, 40);
            report.setReporter(student);
            manager.report(report);
            if (firstReport == null) {
                firstReport = report;
            }
            lastReport = report;
        }
        System.out.println(lastReport.getId() + " is the same problem as " + lastReport.getRelatedIds());
        for (DuplicateMatch match : manager.findDuplicates(lastReport)) {
            System.out.println("  similarity with " + match.incident().getId() + ": " + match.similarity());
        }
        System.out.println(firstReport.getId() + " escalated: " + firstReport.isEscalated()
                + " (" + firstReport.getEscalationReason() + ")");

        heading("Workflow 4: permissions, lifecycle guard and assignment switch");
        Incident lab = new Incident("LAN port in room 214 is dead", "The LAN port in hostel block C room 214 gives no connection",
                Category.HOSTEL, "hostel-room", 2, 1);
        lab.setReporter(student);
        manager.report(lab);
        Technician labTeam = manager.assign(lab.getId(), teams);
        System.out.println("Matched by skill to: " + labTeam.getName() + " " + labTeam.getSkills());
        try {
            manager.advance(lab.getId(), Status.IN_PROGRESS, student);
        } catch (ITSMException e) {
            System.out.println("Refused (permissions): " + e.getMessage());
        }
        try {
            manager.advance(lab.getId(), Status.CLOSED, admin);
        } catch (ITSMException e) {
            System.out.println("Refused (lifecycle): " + e.getMessage());
        }
        try {
            manager.setStrategy(new RoundRobinStrategy(), student);
        } catch (ITSMException e) {
            System.out.println("Refused (configuration): " + e.getMessage());
        }
        manager.setStrategy(new RoundRobinStrategy(), admin);
        System.out.println("Admin switched assignment to: " + manager.getStrategyName());

        heading("Workflow 5: critical ticket nobody picks up -> escalated automatically");
        Incident login = new Incident("Campus Wi-Fi login page down", "Wi-Fi login page is not loading anywhere on campus",
                Category.NETWORK, "campus-network", 3, 1500);
        login.setReporter(student);
        manager.report(login);
        manager.assign(login.getId(), teams);
        for (String alert : monitor.check(login.getCreatedAt().plusMinutes(20))) {
            System.out.println("SLA monitor, 20 min later: " + alert);
        }
        System.out.println(login.getId() + " escalated: " + login.isEscalated() + " (" + login.getEscalationReason() + ")");

        heading("Analytics");
        ReportService reports = new ReportService(manager);
        System.out.println("By status:   " + reports.byStatus());
        System.out.println("By priority: " + reports.byPriority());
        System.out.println("By service:  " + reports.byService());
        System.out.println("Escalated:   " + reports.escalatedCount());

        heading("Audit trail for " + quiz.getId());
        for (AuditEntry entry : quiz.getAudit()) {
            System.out.println("  " + entry);
        }
        System.out.println();
        System.out.println("Console demo saved to out/demo/incidents.csv. Open the app with: bash run.sh web");
    }

    private static void heading(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
