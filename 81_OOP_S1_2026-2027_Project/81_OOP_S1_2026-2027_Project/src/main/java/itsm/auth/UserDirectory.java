package itsm.auth;

import itsm.domain.Administrator;
import itsm.domain.Category;
import itsm.domain.Employee;
import itsm.domain.Technician;
import itsm.domain.User;
import itsm.exception.InvalidLoginException;
import itsm.exception.UnauthorizedException;
import itsm.exception.ValidationException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Every account in the system: registration, login and user management.
 * Students register themselves; technicians are added by an administrator.
 */
public class UserDirectory implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._-]{3,20}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    public static final int MIN_PASSWORD = 6;

    private final Map<String, Account> accounts = new LinkedHashMap<>();   // keyed by username

    // ---- registration ----

    /** Self-registration: creates a student (Employee) account. */
    public Employee register(String username, String name, String email, String password) throws ValidationException {
        String u = checkNew(username, name, email, password);
        Employee e = new Employee(nextId("S"), name.trim(), email.trim());
        accounts.put(u, new Account(u, e, PasswordHasher.hash(password)));
        return e;
    }

    /** Administrator adds a support technician with their skills. */
    public Technician addTechnician(User actor, String username, String name, String email, String password,
            List<String> skills) throws ValidationException, UnauthorizedException {
        requireAdmin(actor);
        String u = checkNew(username, name, email, password);
        if (skills == null || skills.isEmpty()) {
            throw new ValidationException("Pick at least one skill so tickets can be matched to this technician");
        }
        for (String s : skills) {
            boolean known = false;
            for (Category c : Category.values()) {
                known |= c.label().equals(s);
            }
            if (!known) {
                throw new ValidationException("Unknown skill: " + s);
            }
        }
        Technician t = new Technician(nextId("T"), name.trim(), email.trim(), skills);
        accounts.put(u, new Account(u, t, PasswordHasher.hash(password)));
        return t;
    }

    /**
     * Gives an existing account a new username, password and user object. The new user must keep
     * the same id, so tickets already raised by or assigned to the old account stay linked to it.
     */
    public void replaceAccount(String oldUsername, String newUsername, String password, User user) throws ValidationException {
        String oldU = normalise(oldUsername);
        String newU = normalise(newUsername);
        Account old = accounts.get(oldU);
        if (old == null) {
            throw new ValidationException("No account with the username \"" + oldU + "\"");
        }
        if (!old.getUser().getUserId().equals(user.getUserId())) {
            throw new ValidationException("The replacement account must keep the user id " + old.getUser().getUserId());
        }
        if (!oldU.equals(newU) && accounts.containsKey(newU)) {
            throw new ValidationException("The username \"" + newU + "\" is taken");
        }
        user.setActive(old.getUser().isActive());
        accounts.remove(oldU);
        accounts.put(newU, new Account(newU, user, PasswordHasher.hash(password)));
    }

    public boolean hasUsername(String username) {
        return accounts.containsKey(normalise(username));
    }

    public User findByUsername(String username) {
        Account a = accounts.get(normalise(username));
        return a == null ? null : a.getUser();
    }

    /** Used when the app is first set up, to create the starting accounts. */
    public void addAccount(String username, String password, User user) throws ValidationException {
        String u = normalise(username);
        if (accounts.containsKey(u)) {
            throw new ValidationException("The username \"" + u + "\" is taken");
        }
        accounts.put(u, new Account(u, user, PasswordHasher.hash(password)));
    }

    private String checkNew(String username, String name, String email, String password) throws ValidationException {
        String u = normalise(username);
        if (!USERNAME.matcher(u).matches()) {
            throw new ValidationException("Username: 3 to 20 characters, letters, digits, dot, dash or underscore");
        }
        if (accounts.containsKey(u)) {
            throw new ValidationException("The username \"" + u + "\" is taken");
        }
        if (name == null || name.isBlank() || name.trim().length() > 60) {
            throw new ValidationException("Please enter a name (up to 60 characters)");
        }
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new ValidationException("Please enter a valid email address");
        }
        if (password == null || password.length() < MIN_PASSWORD) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD + " characters");
        }
        return u;
    }

    private String nextId(String prefix) {
        int n = 0;
        for (Account a : accounts.values()) {
            String id = a.getUser().getUserId();
            if (id.startsWith(prefix)) {
                try {
                    n = Math.max(n, Integer.parseInt(id.substring(prefix.length())));
                } catch (NumberFormatException ignored) {
                    // not a numbered id
                }
            }
        }
        return prefix + (n + 1);
    }

    private static String normalise(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    // ---- login ----

    /** Authentication. The same message for an unknown user and a wrong password, so usernames cannot be probed. */
    public User login(String username, String password) throws InvalidLoginException {
        Account a = accounts.get(normalise(username));
        if (a == null) {
            for (Account candidate : accounts.values()) {   // also accept the account's email address
                if (candidate.getUser().getEmail().equalsIgnoreCase(username == null ? "" : username.trim())) {
                    a = candidate;
                }
            }
        }
        if (a == null || !a.checkPassword(password)) {
            throw new InvalidLoginException("Wrong email/username or password");
        }
        if (!a.getUser().isActive()) {
            throw new InvalidLoginException("This account has been disabled. Contact the CCIT admin.");
        }
        return a.getUser();
    }

    // ---- user management (administrator) ----

    public void setActive(User actor, String userId, boolean active) throws UnauthorizedException, ValidationException {
        requireAdmin(actor);
        User target = findById(userId);
        if (target == null) {
            throw new ValidationException("No such user: " + userId);
        }
        if (target.equals(actor)) {
            throw new ValidationException("You cannot disable your own account");
        }
        target.setActive(active);
    }

    private static void requireAdmin(User actor) throws UnauthorizedException {
        if (actor == null || !actor.canConfigure()) {
            throw new UnauthorizedException("Only an administrator can manage users");
        }
    }

    // ---- lookups ----

    public User findById(String userId) {
        for (Account a : accounts.values()) {
            if (a.getUser().getUserId().equals(userId)) {
                return a.getUser();
            }
        }
        return null;
    }

    public String usernameOf(User user) {
        for (Account a : accounts.values()) {
            if (a.getUser().equals(user)) {
                return a.getUsername();
            }
        }
        return "";
    }

    public List<Account> accounts() {
        return new ArrayList<>(accounts.values());
    }

    public List<Technician> technicians() {
        List<Technician> list = new ArrayList<>();
        for (Account a : accounts.values()) {
            if (a.getUser() instanceof Technician) {
                list.add((Technician) a.getUser());
            }
        }
        return list;
    }

    public List<User> administrators() {
        List<User> list = new ArrayList<>();
        for (Account a : accounts.values()) {
            if (a.getUser() instanceof Administrator) {
                list.add(a.getUser());
            }
        }
        return list;
    }

    public boolean isEmpty() {
        return accounts.isEmpty();
    }
}
