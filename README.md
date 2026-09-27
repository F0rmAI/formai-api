# FormAI API

![CI](https://github.com/F0rmAI/formai-api/actions/workflows/ci.yml/badge.svg)

## Summary

FormAI API, built with Java, the Spring Boot Framework, and Spring Data JPA on a
PostgreSQL database, following Domain-Driven Design. Each bounded context lives as
an internal module inside a single deployable, and contexts communicate in-process
through Open Host Services and domain events rather than over the network.

## Features

- RESTful API
- Domain-Driven Design (modular monolith)
- Own JWT authentication (locally issued by the IAM module, carried in an httpOnly cookie)
- Role- and channel-aware sign-in (trainers on the web platform, clients on the mobile app)
- Account lockout, client activation codes and password recovery
- Trainer client management with body profiles and weight history
- Exercise catalog, versioned routines and routine assignments
- Workout logging with compliance status, history, progress reports and progress charts
- Scheduled daily job that schedules today's sessions and skips overdue ones
- Spring Boot Framework
- Spring Data JPA
- Bean Validation
- PostgreSQL Database (schema per module)
- Flyway Migrations (per module)
- In-process Domain Events
- ArchUnit boundary enforcement
- Health endpoint (Spring Boot Actuator)
- RFC 7807 `ProblemDetail` error responses and a global fallback exception handler
- Interactive API documentation (springdoc-openapi / Swagger UI)
- Continuous Integration (GitHub Actions)
- Git Flow branching strategy (`main` protected, `develop`, `feature/*`, `release/*`)

## Bounded Contexts

The application is divided into internal modules, each one a bounded context with
its own domain, application, infrastructure, and interfaces layers.

### Identity and Access Management (IAM) Context

The IAM Context owns every FormAI account (trainers and clients) and how they get in:

- **Trainer sign-up** with email, full name and a password (8–128 characters), stored as
  a BCrypt hash. The account is created `ACTIVE` with the `REGISTERED_USER` and `TRAINER`
  roles.
- **Sign-in per client application.** Clients sign in only from the mobile app
  (`MOBILE_APP`); trainers and administrators only from the web platform (`WEB_PLATFORM`).
  Any other combination answers `403`. Wrong credentials always answer the same `401`,
  whether the email exists or not.
- **Account lockout.** The fifth consecutive failed sign-in locks the account for 15
  minutes (`429`). A successful sign-in resets the counter.
- **Sign-out** clears the httpOnly JWT cookie server-side.
- **Client accounts with activation codes.** A trainer creates a client account
  `PENDING_ACTIVATION` with a one-time, 8-character code valid for 72 hours, shown on screen
  and shared by hand. Reissuing a code replaces the previous one. The client activates the
  account with the code, a password and the personal data processing consent.
- **Password reset by email.** A request issues a one-time link token valid for 30
  minutes, and only its SHA-256 hash is stored. The request always answers the same
  message, whether the email exists or not.
- **Accumulable roles** (`REGISTERED_USER`, `TRAINER`, `CLIENT`, `ADMINISTRATOR`) travel in
  the JWT `roles` claim and land as `ROLE_<name>` authorities via `JwtAuthenticationFilter`.
  `SecurityConfig` restricts trainer routes to `ROLE_TRAINER` and client routes to
  `ROLE_CLIENT`.

Other contexts reach IAM only through its Open Host Service,
`iam.interfaces.acl.IamContextFacade`, which speaks neutral types and the
`shared.contracts.iam.AccountActivationSummary` record:

| Facade method | Purpose |
|---|---|
| `createClientAccount(email)` | Create a pending client account and return its activation code |
| `reissueActivationCode(userId)` | Replace the activation code of a pending client |
| `disableAccount(userId)` | Disable an account so it can no longer sign in |
| `fetchAccountStatus(userId)` | Read the account status (`PENDING_ACTIVATION`, `ACTIVE`, `DISABLED`) |

IAM publishes these in-process domain events:

| Event | Published when | Intended consumer |
|---|---|---|
| `UserRegistered` | A trainer signs up (carries full name and email) | clients context |
| `AccountActivated` | A client activates the account | clients context |
| `PasswordResetRequested` | A password reset link is issued (carries the raw token) | notifications context |
| `ActivationCodeIssued` | An activation code is created or reissued | audit only |
| `AccountLocked` | An account gets locked after failed sign-ins | audit only |

### Clients Context

The Clients Context is responsible for the trainer's clients and their data for planning. It
includes the following features:

- Register a client as `INVITED` and show a 72-hour activation code on screen.
- Renew the activation code of a client who has not activated the account yet.
- List, search by name and filter by status the trainer's own clients; rename a client.
- Deactivate a client: the account can no longer sign in and the history is kept.
- Record the body profile (goal, height between 100 and 250 cm, weight above 0 kg,
  restrictions), keeping every weight change with its date.

It also exposes an Open Host Service (OHS) for in-process communication with other contexts,
`clients.interfaces.acl.ClientsContextFacade`, offering the following capabilities:

- Fetch one of a trainer's clients, returning a `ClientSummary` or empty.
- Fetch a page of a trainer's clients, returning a `ClientSummaryPage`.

It relies on an anti-corruption layer (ACL) to consume the IAM Context, translating its
contract into this context's own model. It reacts to the `UserRegistered` domain event
published by the IAM Context to register the trainer, and to `AccountActivated` to make the
client `ACTIVE`, keeping both contexts decoupled. It publishes `ClientDeactivated`.

### Planning Context

The Planning Context is responsible for the trainer's exercises, routines and assignments. It
includes the following features:

- Keep an exercise catalog per trainer; an exercise used by a routine can only be archived,
  and an archived exercise can be restored.
- Create routines with sessions and prescribed exercises (sets, reps, target load, rest),
  starting as `DRAFT`, and duplicate them.
- Revise a routine: every change adds a version with its date and author.
- Assign a routine to one or several active clients from a start date, closing the previous
  assignment. A routine with no open assignment becomes `CLOSED` and can be assigned again
  without duplicating it.

It also exposes an Open Host Service (OHS) for in-process communication with other contexts,
`planning.interfaces.acl.PlanningContextFacade`, offering the following capabilities:

- Fetch a client's current routine, returning an `ActiveRoutineSnapshot` at its latest version.

It relies on an anti-corruption layer (ACL) to consume the Clients Context, translating its
contract into this context's own model. It reacts to the `ClientDeactivated` domain event
published by the Clients Context to close the client's assignment, keeping both contexts
decoupled. It publishes `RoutineAssigned`, `RoutineUpdated` and `AssignmentClosed`.

### Tracking Context

The Tracking Context is responsible for what clients actually train and how they progress. It
includes the following features:

- Show the client's current routine and today's session, with every session's detail.
- Record load and reps per set, correct a set without duplicating it, and finish a session as
  `COMPLETED` or, once confirmed, `PARTIAL`; the daily job marks unrecorded sessions `SKIPPED`.
- Workout history, most recent first, with volume, per-set detail and a date filter, for the
  client and for the client's trainer.
- Trainer client list with each client's current routine and last workout date.
- Progress report per period: adherence, sessions by status, and each exercise's heaviest load
  and volume in its first and last session.
- Progress chart per exercise over 4, 8 or 12 weeks, for the client and the trainer.

It relies on an anti-corruption layer (ACL) to consume the Planning and Clients Contexts,
translating their contracts into this context's own model. It reacts to the `RoutineAssigned`,
`RoutineUpdated` and `AssignmentClosed` domain events published by the Planning Context to keep
the client's current routine in sync, keeping both contexts decoupled.

## Technology Stack

| Concern | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 3.5.15 |
| Persistence | Spring Data JPA · PostgreSQL 17 · Flyway |
| Mapping | MapStruct 1.6.3 |
| Security | Spring Security · JWT (jjwt 0.12.6) |
| API documentation | springdoc-openapi 2.9.1 (Swagger UI) |
| Architecture tests | ArchUnit 1.4.1 |

Lombok, Flyway, and the PostgreSQL driver are managed by the `spring-boot-starter-parent` BOM.

## Project Structure

```
com.formai.api
├── iam/                  Identity and Access Management (generic subdomain)
│   ├── domain/           User aggregate, ActivationCode and PasswordResetToken entities,
│   │                     value objects, commands, queries, events, exceptions
│   ├── application/      UserCommandServiceImpl, UserQueryServiceImpl, hashing and JWT
│   │                     outbound services, IamContextFacadeImpl (OHS implementation)
│   ├── infrastructure/   UserJpaEntity with embeddables, Spring Data repository, MapStruct mapper
│   └── interfaces/       REST controllers, resources, assembler, advices, IamContextFacade (OHS)
├── clients/              Clients (supporting subdomain) — trainers, clients, body profiles
├── planning/             Planning (core subdomain) — exercises, routines, assignments
├── tracking/             Tracking (core subdomain) — active routines, workout sessions, progress
└── shared/               Cross-cutting: security, JWT cookie, CORS, Flyway per module,
                          OpenAPI, global exception handler, inter-module contracts
```

Dependencies only point one way: `clients` consumes `iam`, `planning` consumes `clients`, and
`tracking` consumes `planning` and `clients`, always through an OHS facade or a domain event.

## Getting Started

### Prerequisites

- JDK 25
- Docker (PostgreSQL 17)
- Maven 3.9+

### Configuration

```bash
cp .env.example .env   # set DB_PASSWORD and JWT_SECRET (openssl rand -base64 64)
```

### Running the application

```bash
docker compose up -d   # starts PostgreSQL 17 only
mvn spring-boot:run    # or run FormaiApplication from the IDE
```

The API listens on `http://localhost:8080`; `GET /actuator/health` answers `{"status":"UP"}`
once it is ready.

### Exploring the API with Swagger UI

Open `http://localhost:8080/swagger-ui/index.html` (raw spec at `/v3/api-docs`). Operations
are grouped by tag:

| Tag | Operations |
|---|---|
| Authentication | sign-up, sign-in, sign-out |
| Clients | client registration, activation codes, deactivation, body profile; account activation |
| Exercises | the trainer's exercise catalog |
| Routines | routines, versions, duplicates, assignments |
| Workouts | active routine, today's session, sets, completion, history |
| Client overviews | the trainer's client list with routine and last workout |
| Progress reports | adherence and exercise metrics for a period |
| Progress charts | load and volume evolution for 4, 8 or 12 weeks |
| Password Recovery | request a reset link, reset the password |

Sign in with **Try it out** on `POST /api/v1/authentication/sign-in` (use
`"application": "WEB_PLATFORM"` for a trainer). The browser stores the httpOnly JWT cookie
and sends it automatically on every later call; it does not appear in Swagger UI, but it is
visible in the browser DevTools under Application → Cookies. Use Chrome or Firefox: both
accept `Secure` cookies on `http://localhost`.

## Git Workflow

<p align="justify">
This project follows a lightweight Git Flow. <code>main</code> only ever holds
deployable code — nobody pushes to it directly, and no work happens on it beyond
merging a finished <code>release/*</code> (or an urgent <code>hotfix/*</code>). Every
merge into <code>main</code> is a deploy trigger, so it stays tagged with the version
it represents (e.g. <code>v1.2.0</code>).
</p>

<p align="justify">
<code>develop</code> is the integration branch. It branches off <code>main</code> once,
at the start of the project, and every <code>feature/*</code> branch is cut from it and
merged back into it via pull request. This is where several features live and get
tested together before anything is considered for release, without ever touching
<code>main</code>.
</p>

<p align="justify">
<code>feature/*</code> branches are short-lived and scoped to a single task. They branch
off <code>develop</code> and merge back into <code>develop</code> once the change is
done and CI passes. Naming convention: <code>feature/&lt;short-description&gt;</code>
(e.g. <code>feature/refresh-token</code>).
</p>

<p align="justify">
<code>release/*</code> branches are cut from <code>develop</code> once a set of features
is ready to ship. No new features land here — only the stabilization fixes needed to get
that specific batch production-ready (config tweaks, last-mile bug fixes). Naming
convention: <code>release/&lt;version&gt;</code> (e.g. <code>release/1.2.0</code>). When
it's ready, it merges into both <code>main</code> (tagged and deployed) and
<code>develop</code> (so the stabilization fixes aren't lost).
</p>

<p align="justify">
<code>hotfix/*</code> branches are the emergency exception: cut directly from
<code>main</code> to patch a production issue that can't wait for the next release,
then merged back into both <code>main</code> and <code>develop</code>.
</p>

```
main        ●───────────────────────────●─────────────●
                                          \ (tag v1.1)   \ (tag v1.2)
develop      ●───●───────●───●───●───●────●─────────●────●
                  \       \          \    release/1.2.0
              feature/x  feature/y  feature/z
```

<p align="justify">
This repository currently ships with only <code>main</code>. Create <code>develop</code>
right away — <code>git checkout -b develop && git push -u origin develop</code> — before
opening the first <code>feature/*</code> branch. <code>main</code> is protected: it only
accepts pull requests, each requiring the CI <code>build</code> job to pass.
</p>

## Security

JWT authentication is enabled by default (`shared/config/SecurityConfig`), in every
environment — there is no permit-all development mode. `/api/v1/authentication/**`
stays public (sign-up/sign-in/sign-out), as do `POST` on `/api/v1/account-activations`,
`/api/v1/password-reset-requests` and `/api/v1/password-resets`; every other endpoint
requires a valid JWT. Trainer routes (`/clients/**`, `/exercises/**`, `/routines/**`,
`/client-overviews/**`) also require `ROLE_TRAINER`, and client routes (`/active-routines/**`,
`/workout-sessions/**`, `/progress-charts/**`) require `ROLE_CLIENT`; trainer data is always
scoped to the signed-in trainer (`holderId` = JWT `sub`).

The JWT never travels in the response body or a header the client sets manually: on
sign-in, it's set as an **httpOnly, `SameSite=Lax` cookie** (`shared/config/JwtCookieFactory`),
so client-side JavaScript — and therefore XSS — can never read or exfiltrate it. The
browser attaches it automatically on later requests, and `JwtAuthenticationFilter`
reads it from the cookie rather than an `Authorization` header. Because JS cannot
delete an httpOnly cookie itself, `/sign-out` clears it server-side.

A browser frontend on a different origin (e.g. a Vite/React dev server) needs CORS
configured with credentials (`shared/config/CorsConfig`, `CORS_ALLOWED_ORIGIN` in
`.env`) and must call this API with `credentials: 'include'` (fetch) or
`withCredentials: true` (axios) for the cookie to be sent. Set `JWT_SECRET` in `.env`
(required, no default) before starting the application.

### OWASP coverage (SSDLC)

- **A02 (Cryptographic Failures):** BCrypt password hashing (SHA-256 pre-hash so passwords up
  to 128 characters fit BCrypt's 72-byte limit), signed JWT (never `alg: none`). Password
  reset tokens are stored only as SHA-256 hashes.
- **A03 (Injection):** Spring Data JPA plus Bean Validation at the edge, no concatenated SQL.
- **A04 (Insecure Design):** sign-in returns a single generic error, never revealing whether the
  email exists or the password was wrong, and a password reset request always answers the
  same message (prevents user enumeration).
- **A08 (Logging Failures):** failed sign-in attempts are logged without the password.
- **A10 (Authentication Attacks):** stateless, short-lived JWT, and account lockout for 15
  minutes after 5 consecutive failed sign-ins. MFA is out of scope for now.

## API Endpoints

| Method | Path | Swagger tag | Success | Errors | Auth |
|---|---|---|---|---|---|
| `POST` | `/api/v1/authentication/sign-up` | Authentication | `201` | `400` `409` `422` | No |
| `POST` | `/api/v1/authentication/sign-in` | Authentication | `200` + JWT cookie | `400` `401` `403` `429` | No |
| `POST` | `/api/v1/authentication/sign-out` | Authentication | `204` | — | No |
| `POST` | `/api/v1/account-activations` | Clients | `201` | `400` `422` | No |
| `POST` | `/api/v1/password-reset-requests` | Password Recovery | `201` | `400` | No |
| `POST` | `/api/v1/password-resets` | Password Recovery | `201` | `400` `422` | No |
| `POST` | `/api/v1/clients` | Clients | `201` | `400` `403` `409` | Trainer |
| `GET` | `/api/v1/clients?search&status&page&size` | Clients | `200` | `400` `403` | Trainer |
| `GET` | `/api/v1/clients/{id}` | Clients | `200` | `403` `404` | Trainer |
| `PUT` | `/api/v1/clients/{id}` | Clients | `200` | `400` `403` `404` | Trainer |
| `POST` | `/api/v1/clients/{id}/deactivations` | Clients | `201` | `403` `404` | Trainer |
| `POST` | `/api/v1/clients/{id}/activation-codes` | Clients | `201` | `403` `404` `409` | Trainer |
| `GET` | `/api/v1/clients/{id}/body-profile` | Clients | `200` | `403` `404` | Trainer |
| `PUT` | `/api/v1/clients/{id}/body-profile` | Clients | `200` | `400` `403` `404` `422` | Trainer |
| `GET` | `/api/v1/clients/{id}/assignments` | Routines | `200` | `403` | Trainer |
| `GET` | `/api/v1/clients/{id}/workout-sessions?from&to&page&size` | Workouts | `200` | `400` `403` | Trainer |
| `GET` | `/api/v1/clients/{id}/progress-reports?from&to` | Progress reports | `200` | `400` `403` | Trainer |
| `GET` | `/api/v1/clients/{id}/progress-charts?exerciseId&weeks` | Progress charts | `200` | `400` `403` | Trainer |
| `POST` | `/api/v1/exercises` | Exercises | `201` | `400` `403` `409` | Trainer |
| `GET` | `/api/v1/exercises?search&status&page&size` | Exercises | `200` | `400` `403` | Trainer |
| `GET` | `/api/v1/exercises/{id}` | Exercises | `200` | `403` `404` | Trainer |
| `DELETE` | `/api/v1/exercises/{id}` | Exercises | `204` | `403` `404` `409` | Trainer |
| `POST` | `/api/v1/exercises/{id}/archivals` | Exercises | `201` | `403` `404` | Trainer |
| `POST` | `/api/v1/exercises/{id}/restorations` | Exercises | `201` | `403` `404` | Trainer |
| `POST` | `/api/v1/routines` | Routines | `201` | `400` `403` `404` `422` | Trainer |
| `GET` | `/api/v1/routines?page&size` | Routines | `200` | `400` `403` | Trainer |
| `GET` | `/api/v1/routines/{id}` | Routines | `200` | `403` `404` | Trainer |
| `PUT` | `/api/v1/routines/{id}` | Routines | `200` | `400` `403` `404` `422` | Trainer |
| `GET` | `/api/v1/routines/{id}/versions` | Routines | `200` | `403` `404` | Trainer |
| `POST` | `/api/v1/routines/{id}/duplicates` | Routines | `201` | `400` `403` `404` | Trainer |
| `POST` | `/api/v1/routines/{id}/assignments` | Routines | `201` | `400` `403` `404` `422` | Trainer |
| `GET` | `/api/v1/client-overviews?search&status&page&size` | Client overviews | `200` | `400` `403` | Trainer |
| `GET` | `/api/v1/active-routines/me` | Workouts | `200` | `403` `404` | Client |
| `GET` | `/api/v1/workout-sessions?from&to&page&size` | Workouts | `200` | `400` `403` | Client |
| `GET` | `/api/v1/workout-sessions/{id}` | Workouts | `200` | `403` `404` | Client |
| `POST` | `/api/v1/workout-sessions/{id}/sets` | Workouts | `201` | `400` `403` `404` `409` `422` | Client |
| `POST` | `/api/v1/workout-sessions/{id}/corrections` | Workouts | `201` | `400` `403` `404` `409` `422` | Client |
| `POST` | `/api/v1/workout-sessions/{id}/completions` | Workouts | `201` | `403` `404` `409` | Client |
| `GET` | `/api/v1/progress-charts/me?exerciseId&weeks` | Progress charts | `200` | `400` `403` | Client |
| `GET`  | `/actuator/health` | — | `200` | — | No |

## Error Handling

Each module maps its own domain exceptions to `ProblemDetail` responses in a
`ControllerAdvice` with `@Order(Ordered.HIGHEST_PRECEDENCE)`. Unexpected exceptions
(anything not mapped by a module's own `ControllerAdvice`) are caught by
`shared/interfaces/rest/GlobalExceptionHandler` (`@Order(Ordered.LOWEST_PRECEDENCE)`),
which returns a generic `500` body —
never the exception message or stack trace — while logging the real cause server-side.
Spring MVC's own well-known exceptions (malformed JSON, validation errors, wrong HTTP
method) keep their correct `4xx` status untouched.

## Testing

```bash
mvn test -Dtest=ArchitectureTest   # module boundaries (ArchUnit)
mvn test                           # full suite
```

No test needs a running database. Every module has a test per layer; the IAM module is the
reference to copy when adding a new one: `UserTest` (domain), `UserCommandServiceImplTest` and
`UserQueryServiceImplTest` (application), `IamContextFacadeImplTest` (OHS facade),
`UserRepositoryImplTest` (persistence) and one `@WebMvcTest` per controller, which import
the real `SecurityConfig`.

The suite has 340 tests across every layer of `iam`, `clients`, `planning` and `tracking`, plus
the ArchUnit boundary rules.

CI (`.github/workflows/ci.yml`) runs the full suite against an ephemeral PostgreSQL on
every push and pull request to `main`, `develop`, and `release/**` — see
[Git Workflow](#git-workflow).
