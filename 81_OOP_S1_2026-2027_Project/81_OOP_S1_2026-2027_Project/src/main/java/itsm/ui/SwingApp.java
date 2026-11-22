package itsm.ui;

import itsm.core.IncidentManager;
import itsm.domain.Administrator;
import itsm.domain.Category;
import itsm.domain.Employee;
import itsm.domain.Incident;
import itsm.domain.Status;
import itsm.domain.Technician;
import itsm.exception.ITSMException;
import itsm.storage.InMemoryRepository;
import itsm.strategy.SkillBasedStrategy;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * Early desktop prototype, kept to show that the core classes work behind a second interface
 * (Swing instead of the web). It is not the product: it has no sign-in and keeps data in memory only.
 * The helpdesk itself is the web app (itsm.web.DashboardServer).
 */
public class SwingApp {

    private static final String[] SERVICES = {"wifi", "lms", "email", "lab", "printing", "other"};

    private final IncidentManager manager = new IncidentManager(new InMemoryRepository(), new SkillBasedStrategy());
    private final Employee student = new Employee("S1", "Student", "student@campus.example");
    private final Administrator admin = new Administrator("A1", "CCIT admin", "ccit@campus.example");
    private final List<Technician> teams = List.of(
            new Technician("T1", "Network team", "network@campus.example", List.of("Network")),
            new Technician("T2", "Apps team", "apps@campus.example", List.of("Application", "Authentication")),
            new Technician("T3", "Lab support", "labs@campus.example", List.of("Hardware", "Storage", "Database")));
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] {"Ticket", "Problem", "Status", "Priority", "Handled by"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private JFrame frame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SwingApp().show());
    }

    private void show() {
        frame = new JFrame("IT Helpdesk desktop prototype (no sign-in, data not saved)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JComboBox<String> service = new JComboBox<>(SERVICES);
        JTextField problem = new JTextField(28);
        JSpinner urgency = new JSpinner(new SpinnerNumberModel(2, 1, 3, 1));
        JSpinner people = new JSpinner(new SpinnerNumberModel(40, 1, 5000, 10));
        JButton raise = new JButton("Raise ticket");

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("Service"));
        form.add(service);
        form.add(new JLabel("Problem"));
        form.add(problem);
        form.add(new JLabel("Urgency"));
        form.add(urgency);
        form.add(new JLabel("People affected"));
        form.add(people);
        form.add(raise);

        raise.addActionListener(event -> raiseTicket((String) service.getSelectedItem(), problem.getText(),
                (Integer) urgency.getValue(), (Integer) people.getValue()));

        JButton next = new JButton("Next step for selected ticket");
        next.addActionListener(event -> advanceSelected());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(next);

        frame.add(form, BorderLayout.NORTH);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(actions, BorderLayout.SOUTH);
        frame.setSize(1000, 450);
        frame.setVisible(true);
    }

    private void raiseTicket(String service, String text, int urgency, int people) {
        if (text.isBlank()) {
            return;
        }
        Incident inc = new Incident(text, text, categoryFor(service), service, urgency, people);
        inc.setReporter(student);
        manager.report(inc);
        try {
            manager.assign(inc.getId(), teams);
        } catch (ITSMException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Could not assign", JOptionPane.ERROR_MESSAGE);
        }
        String message = "Raised " + inc.getId() + " with priority " + inc.getPriority();
        if (!inc.getRelatedIds().isEmpty()) {
            message += "\nSame problem as " + String.join(", ", inc.getRelatedIds());
        }
        JOptionPane.showMessageDialog(frame, message);
        refresh();
    }

    private void advanceSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(frame, "Select a ticket first.");
            return;
        }
        String id = (String) model.getValueAt(row, 0);
        try {
            Incident inc = manager.getIncident(id);
            Status next = nextStatus(inc.getStatus());
            if (next == null) {
                JOptionPane.showMessageDialog(frame, "This ticket is already closed.");
                return;
            }
            manager.advance(id, next, admin);
        } catch (ITSMException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Not allowed", JOptionPane.ERROR_MESSAGE);
        }
        refresh();
    }

    private static Status nextStatus(Status current) {
        switch (current) {
            case NEW:
            case ASSIGNED:
            case REOPENED:
                return Status.IN_PROGRESS;
            case IN_PROGRESS:
                return Status.RESOLVED;
            case RESOLVED:
                return Status.CLOSED;
            default:
                return null;
        }
    }

    private static Category categoryFor(String service) {
        switch (service) {
            case "wifi":
                return Category.NETWORK;
            case "lab":
            case "printing":
                return Category.HARDWARE;
            default:
                return Category.APPLICATION;
        }
    }

    private void refresh() {
        model.setRowCount(0);
        for (Incident inc : manager.getAll()) {
            String priority = inc.getPriority() == null ? "-" : inc.getPriority().name();
            String team = inc.getAssignee() == null ? "-" : inc.getAssignee().getName();
            model.addRow(new Object[] {inc.getId(), inc.getTitle(), inc.getStatus().label(), priority, team});
        }
    }
}
