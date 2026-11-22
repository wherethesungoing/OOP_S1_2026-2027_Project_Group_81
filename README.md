# OOP_S1_2026-2027_Project_Group_81
# I-ITSM — Intelligent IT Service Management & Incident Resolution System

**Group 81 · OOP · Semester 1, 2026–2027**

A campus IT helpdesk that manages the full incident lifecycle:
**Report → Classify → Prioritize → Assign → Investigate → Resolve → Verify → Close**.
Pure Java (JDK 17+), no external libraries.

## How to run

Requires **JDK 17 or later**.

| Command (Linux/macOS) | Windows | What it does |
|---|---|---|
| `bash run.sh` | `run.bat` | Compile, run the console demo, then all tests |
| `bash run.sh test` | `run.bat test` | Compile and run the tests only |
| `bash run.sh web` | `run.bat web` | Start the web app at http://localhost:8080 (data kept in `./data`) |
| `bash run.sh reset` | `run.bat reset` | Same as `web`, but wipe `./data` and start from the demo set-up |

## Features

**MVP**
- **User management:** registration and login for Employee, Technician and Administrator roles, with role-based authorization
- **Incidents:** creation, classification, prioritization and enforced lifecycle transitions
- **Assignment:** two interchangeable strategies — skill-based (with least-loaded fallback) and round-robin — plus a per-technician workload cap
- **SLA management:** response and resolution deadlines per priority, an early "at risk" warning, and violation detection
- **Notifications:** email, SMS, dashboard and in-app (simulated)
- **Audit:** an immutable history of who did what, and when, for every incident
- **Search & reporting:** multi-criteria search filters and analytics reports (including technician workload)

**Intelligent / innovation features**
- Duplicate and related-incident detection with a similarity score
- Major-incident correlation when several related reports of one problem come in
- Predictive SLA violation detection and automated escalation
- A real-time operational dashboard with a live activity feed
- A simulated clock, so a demo can jump ahead to show SLA alerts and escalation

**Security**
- Passwords stored only as salted PBKDF2-HMAC-SHA256 hashes
- Login throttling against password guessing
- Random session tokens

## OOP design

| Concept / pattern | Where |
|---|---|
| Abstraction & inheritance | `User` (abstract) → `Employee`, `Technician`, `Administrator` |
| Encapsulation | `Incident` controls its own state; illegal transitions throw `InvalidStateTransitionException` |
| Polymorphism | `AssignmentStrategy`, `Notifier`, `IncidentRepository` implementations |
| Strategy pattern | `strategy/` — `SkillBasedStrategy`, `RoundRobinStrategy` |
| Observer pattern | `IncidentManager` (subject) → `Notifier` implementations |
| Repository pattern | `storage/` — `InMemoryRepository`, `FileRepository` |
| Open/Closed principle | New notifiers (for example `DashboardNotifier`) are added without changing existing code |
| Custom exception hierarchy | `exception/` — everything extends `ITSMException` |

## Project structure

```
src/main/java/itsm/
├── Main.java        Console walk-through of the three workflows
├── auth/            Accounts, password hashing, login throttling
├── core/            IncidentManager, service catalog, search, duplicate matching
├── domain/          Incident, users, SLA, priority, status, category, audit
├── exception/       Custom exception hierarchy
├── monitor/         SLA monitoring and escalation
├── notify/          Email / SMS / dashboard / in-app notifiers
├── report/          Analytics and reports
├── storage/         Repository interface plus in-memory and file stores
├── strategy/        Technician-assignment strategies
├── ui/              Early Swing desktop prototype
└── web/             Web server, single-page front end, sessions, live feed
src/test/java/itsm/
└── TestRunner.java  62 tests
```

## Testing

`bash run.sh test` runs 62 tests. They cover normal operations, boundary conditions, invalid inputs, exceptions, authorization and business rules.

## Demonstrated workflows

1. An employee reports an incident → the system prioritizes it → a technician is assigned → notifications are sent
2. The technician investigates → the SLA is monitored → the incident is resolved → the user is notified
3. Several incidents are reported → duplicates or related incidents are detected → they are correlated and escalated
