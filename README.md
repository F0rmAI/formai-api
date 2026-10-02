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
- Own JWT authentication (locally issued by the IAM module, carried in an httpOnly cookie) with a rotating refresh token
- Role- and channel-aware sign-in (trainers on the web platform, clients on the mobile app)
- Account lockout, client activation codes and password recovery
- Trainer client management with body profiles and weight history
- Exercise catalog, versioned routines and routine assignments
- Workout logging with compliance status, history, progress reports and progress charts
- Today's session scheduled on assignment, by a daily job and on startup; overdue ones skipped
- Outbox of notifications (password reset email) delivered by a scheduled dispatcher through Brevo's SMTP relay
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
  a BCrypt hash (cost 12). The account is created `ACTIVE` with the `REGISTERED_USER` and `TRAINER`
  roles.
- **Sign-in per client application.** Clients sign in only from the mobile app
  (`MOBILE_APP`); trainers and administrators only from the web platform (`WEB_PLATFORM`).
  Any other combination answers `403`. Wrong credentials always answer the same `401`,
  whether the email exists or not.
- **Account lockout.** The fifth consecutive failed sign-in locks the account for 15
  minutes (`429`). A successful sign-in resets the counter.
- **Session renewal.** Sign-in also sets a 7-day `refresh_token` cookie. `POST
  /authentication/refresh` swaps it for a new JWT and a new refresh token; the one used stops
  working, and reusing a revoked one revokes every session of the account. Only its SHA-256
  hash is stored.
- **Sign-out** revokes the refresh token and clears both httpOnly cookies server-side.
  Disabling an account revokes all of its refresh tokens.
- **Client accounts with activation codes.** A trainer creates a client account
  `PENDING_ACTIVATION`, without an email, with a one-time, 8-character code valid for 72 hours,
  shown on screen and shared by hand. Reissuing a code replaces the previous one. The client
  activates the account from the mobile app with the code, the email they will sign in with
  (unique across accounts, `409` if taken), a password and the personal data processing consent,
  recorded with the version of the consent text and the acceptance date (Law No. 29733).
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
| `createClientAccount()` | Create a pending client account, without an email, and return its activation code |
| `reissueActivationCode(userId)` | Replace the activation code of a pending client |
| `disableAccount(userId)` | Disable an account so it can no longer sign in |
| `fetchAccountStatus(userId)` | Read the account status (`PENDING_ACTIVATION`, `ACTIVE`, `DISABLED`) |
| `fetchAccountEmail(userId)` | Read the email of an account; empty while a client account is pending |

IAM publishes these in-process domain events:

| Event | Published when | Intended consumer |
|---|---|---|
| `UserRegistered` | A trainer signs up (carries full name and email) | clients context |
| `AccountActivated` | A client activates the account (carries the email the client chose) | clients context |
| `PasswordResetRequested` | A password reset link is issued (carries the raw token) | notifications context |
| `ActivationCodeIssued` | An activation code is created or reissued | audit only |
| `AccountLocked` | An account gets locked after failed sign-ins | audit only |

### Clients Context

The Clients Context is responsible for the trainer's clients and their data for planning. It
includes the following features:

- Register a client as `INVITED` with the name only and show a 72-hour activation code on
  screen. The client's email stays empty until the client activates the account and chooses it.
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
- Link an exercise to a published machine of the catalog, so its usage guide goes with the
  exercise in the clients' routines. The machine catalog belongs to the final increment and
  does not exist yet: `ExternalCatalogService` answers that no machine is published, so every
  link answers `422` until that context is built.
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
- Schedule today's session as soon as a routine starting today is assigned, then every day with
  `WorkoutSessionDailyJob`, which also runs once on startup to catch up a cron missed while the
  application was down.
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

### Notifications Context

The Notifications Context delivers the messages other contexts ask for. Today it sends the
password reset email (the only transactional email of the product):

- `PasswordResetRequestedEventHandler` reacts, after commit, to the `PasswordResetRequested`
  event published by IAM and schedules a `Notification` with the reset link
  (`PASSWORD_RESET_URL` + token). A newer request cancels the email still waiting for the
  previous link, since IAM keeps only the latest token.
- `PendingNotificationDispatcherJob` sends the due notifications every minute
  (`NOTIFICATIONS_DISPATCHER_DELAY`) and retries the failed ones, up to 5 attempts. Once an
  email is sent, cancelled or out of attempts its body is replaced by a neutral text, so the
  reset link is in the database only while the email is pending.
- Resilience is outbox + reconciliation: the `Notification` row is the outbox. If the handler
  fails after IAM's commit no email goes out; the reset request still answers the same neutral
  message and the user can ask for a new link.
- `SmtpEmailDeliveryService` sends through Brevo's SMTP relay over STARTTLS (`SMTP_HOST`,
  `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`) from the sender verified in Brevo (`SMTP_FROM_EMAIL`,
  `SMTP_FROM_NAME`). It never logs the address, the body (it carries the token) or the relay's
  error message. Without those variables the application still starts and the email stays
  `FAILED`.

It has no REST endpoints and depends on no other context's facade.

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
├── notifications/        Notifications (generic subdomain) — notification outbox and dispatcher
└── shared/               Cross-cutting: security, JWT cookie, CORS, Flyway per module,
                          OpenAPI, global exception handler, inter-module contracts
```

Dependencies only point one way: `clients` consumes `iam`, `planning` consumes `clients`, and
`tracking` consumes `planning` and `clients`, and `notifications` reacts to an `iam` event,
always through an OHS facade or a domain event.

## Getting Started

### Prerequisites

- JDK 25
- Docker (PostgreSQL 17)
- Maven 3.9+

### Configuration

```bash
cp .env.example .env   # set DB_PASSWORD and JWT_SECRET (openssl rand -base64 64)
```

`PASSWORD_RESET_URL` (the front-end page that receives the reset token),
`NOTIFICATIONS_DISPATCHER_DELAY`, `JWT_EXPIRATION_MINUTES` (30) and `JWT_REFRESH_EXPIRATION_DAYS` (7)
are optional; `.env.example` shows their defaults. To send the password reset email, set the
Brevo SMTP variables (`SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM_EMAIL`, `SMTP_FROM_NAME`).

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
| Account access | sign-up, sign-in, sign-out, client account activation, password recovery |
| Clients | trainer: client registration, activation codes, client list with routine and last workout, deactivation, body profile |
| Exercises | trainer: the exercise catalog |
| Routines | trainer: routines, versions, duplicates, assignments |
| Workouts | client: active routine, today's session, sets, completion, history; trainer: a client's history |
| Progress | trainer: a client's progress report and charts; client: own progress charts |

Tag names and descriptions live in `shared/interfaces/rest/ApiTags`, and `OpenApiConfiguration`
lists them in this order. Paths ending in `/me` are the client's (mobile app); paths under
`/clients/{id}/…` are the trainer's view of one of their clients (web platform).

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
stays public (sign-up/sign-in/refresh/sign-out), as do `POST` on `/api/v1/activation-code-verifications`, `/api/v1/account-activations`,
`/api/v1/password-reset-requests` and `/api/v1/password-resets`; every other endpoint
requires a valid JWT. Trainer routes (`/clients/**`, `/exercises/**`, `/routines/**`,
`/client-overviews/**`) also require `ROLE_TRAINER`, and client routes (`/active-routines/**`,
`/workout-sessions/**`, `/progress-charts/**`) require `ROLE_CLIENT`; trainer data is always
scoped to the signed-in trainer (`holderId` = JWT `sub`).

The JWT never travels in the response body or a header the client sets manually: on
sign-in, it's set as an **httpOnly, `SameSite=Lax` cookie** (`shared/config/JwtCookieFactory`),
so client-side JavaScript — and therefore XSS — can never read or exfiltrate it. The
browser attaches it automatically on later requests, and `JwtAuthenticationFilter`
reads it from the cookie rather than an `Authorization` header. The JWT lasts 30 minutes;
a second httpOnly cookie, `refresh_token` (7 days, sent only to `/api/v1/authentication`),
renews it through `POST /api/v1/authentication/refresh` and rotates on every use. Because JS
cannot delete an httpOnly cookie itself, `/sign-out` revokes the refresh token and clears both
cookies server-side.

A browser frontend on a different origin (e.g. a Vite/React dev server) needs CORS
configured with credentials (`shared/config/CorsConfig`, `CORS_ALLOWED_ORIGIN` in
`.env`) and must call this API with `credentials: 'include'` (fetch) or
`withCredentials: true` (axios) for the cookie to be sent. Set `JWT_SECRET` in `.env`
(required, no default) before starting the application.

### OWASP coverage (SSDLC)

- **A02 (Cryptographic Failures):** BCrypt password hashing with cost 12 (SHA-256 pre-hash so
  passwords up to 128 characters fit BCrypt's 72-byte limit), signed JWT (never `alg: none`).
  Password reset and refresh tokens are stored only as SHA-256 hashes; the reset link kept in the
  notification outbox is removed as soon as its delivery is closed.
- **A03 (Injection):** Spring Data JPA plus Bean Validation at the edge, no concatenated SQL.
- **A04 (Insecure Design):** sign-in returns a single generic error, never revealing whether the
  email exists or the password was wrong, and a password reset request always answers the
  same message (prevents user enumeration).
- **A08 (Logging Failures):** failed sign-in attempts are logged without the password.
- **A10 (Authentication Attacks):** stateless, 30-minute JWT; 7-day refresh token rotated on
  every use, revoked on sign-out and on account deactivation, with reuse detection; account
  lockout for 15 minutes after 5 consecutive failed sign-ins. MFA is out of scope for now.

## User Stories Coverage

User stories of the requirements specification (`FormAI_Requirements_Specification.md`) that are
ready to use. New development starts from this table.

The partial delivery (TP, the MVP of Sprints 1–2) covers US-001 to US-017 plus US-033 and US-034;
all of them are delivered. US-018 and US-030 belong to the final increment (TB2) and are delivered
ahead of it, US-030 partially (see below).

| US | User story | Endpoint | What it does |
|---|---|---|---|
| US-001 | As a personal trainer, sign up with my email and password | `POST /api/v1/authentication/sign-up` | Creates an active trainer account with a BCrypt-hashed password; a taken email answers `409` |
| US-002 | As a trainer or client, sign in and sign out securely | `POST /api/v1/authentication/sign-in` · `POST /api/v1/authentication/refresh` · `POST /api/v1/authentication/sign-out` | Issues the JWT and a rotating refresh token in httpOnly cookies per channel (trainers on the web, clients on the app), locks the account after 5 failures, and revokes the session on sign-out |
| US-003 | As a trainer, register a client and show their activation code on screen | `POST /api/v1/clients` · `POST /api/v1/clients/{id}/activation-codes` | Registers the client as `INVITED` with the name only (no email) and a 72-hour activation code, and renews it, invalidating the previous one |
| US-004 | As a client, activate my account with the code from my trainer | `POST /api/v1/activation-code-verifications` · `POST /api/v1/account-activations` | Checks that the code exists and has not expired, then activates the account with a valid code, the email to sign in with, a password and the data processing consent with the version of its text |
| US-005 | As a trainer or client, reset my password from my email | `POST /api/v1/password-reset-requests` · `POST /api/v1/password-resets` | Emails a one-time link valid for 30 minutes through Brevo, answers the same message whether the email exists or not, and rejects a used or expired link |
| US-006 | As a trainer, list, search and deactivate my clients | `GET /api/v1/clients?search&status&page&size` · `GET /api/v1/clients/{id}` · `PUT /api/v1/clients/{id}` · `POST /api/v1/clients/{id}/deactivations` · `GET /api/v1/client-overviews?search&status&page&size` | Lists and filters only my clients with their current routine and last workout, renames them, and deactivates them keeping their history |
| US-007 | As a trainer, record each client's body profile | `GET /api/v1/clients/{id}/body-profile` · `PUT /api/v1/clients/{id}/body-profile` | Stores goal, height, weight and restrictions, rejecting out-of-range values and keeping every weight change with its date |
| US-008 | As a trainer, create routines with sessions, exercises, sets, reps and loads | `POST /api/v1/routines` · `GET /api/v1/routines?page&size` · `GET /api/v1/routines/{id}` · `POST /api/v1/routines/{id}/duplicates` | Creates routines as `DRAFT`, rejects invalid prescriptions and duplicates a routine without its clients |
| US-009 | As a trainer, keep my own exercise catalog | `POST /api/v1/exercises` · `GET /api/v1/exercises?search&status&page&size` · `GET /api/v1/exercises/{id}` · `DELETE /api/v1/exercises/{id}` · `POST /api/v1/exercises/{id}/archivals` | Creates exercises without duplicate names, and only archives an exercise a routine uses |
| US-010 | As a trainer, assign a routine to one or several clients | `POST /api/v1/routines/{id}/assignments` · `GET /api/v1/clients/{id}/assignments` | Assigns the routine to active clients from a start date, closing their previous assignment |
| US-011 | As a trainer, modify the routine assigned to a client | `PUT /api/v1/routines/{id}` · `GET /api/v1/routines/{id}/versions` | Saves every change as a new version with its date and author, leaving past workouts untouched |
| US-012 | As a client, see my current routine and pick today's session or any other | `GET /api/v1/active-routines/me` | Shows today's session and every session of the current routine, or `404` when none is assigned |
| US-013 | As a client, record the load and reps of each set | `POST /api/v1/workout-sessions/{id}/sets` · `POST /api/v1/workout-sessions/{id}/corrections` | Records each set with its date and time, rejects invalid values and corrects a set without duplicating it |
| US-014 | As a client, finish my session and see its compliance status | `POST /api/v1/workout-sessions/{id}/completions` | Finishes the session as `COMPLETED` or, once confirmed, `PARTIAL`; the daily job marks unrecorded sessions `SKIPPED` |
| US-015 | As a client, check my workout history | `GET /api/v1/workout-sessions?from&to&page&size` · `GET /api/v1/workout-sessions/{id}` | Lists my sessions newest first with status and volume, filters by dates and shows each set |
| US-016 | As a trainer, review the workouts each client recorded | `GET /api/v1/clients/{id}/workout-sessions?from&to&page&size` | Shows a client's sessions with status and per-set detail; another trainer's client answers `403` |
| US-017 | As a trainer, see each client's adherence and basic metrics | `GET /api/v1/clients/{id}/progress-reports?from&to` | Returns adherence, sessions by status, and each exercise's heaviest load and volume in its first and last session; 0 % when there is no data |
| US-018 | As a trainer or client, see load and volume progress charts | `GET /api/v1/progress-charts/me?exerciseId&weeks` · `GET /api/v1/clients/{id}/progress-charts?exerciseId&weeks` | Returns heaviest load and volume per date over 4, 8 or 12 weeks, flagging when there is not enough data |
| US-033 | As a trainer, restore an archived exercise of my catalog | `POST /api/v1/exercises/{id}/restorations` | Makes the exercise available again for new routines |
| US-034 | As a trainer, assign a closed routine again, adjusting it if needed | `POST /api/v1/routines/{id}/assignments` · `PUT /api/v1/routines/{id}` | A routine with no open assignment becomes `CLOSED`; assigning it again reopens it without duplicating it, and editing it first adds a version |

Partially delivered: **US-030** (link exercises to published machines,
`PUT /api/v1/exercises/{id}/machine-link`) answers `422` until the machine catalog exists.

## API Endpoints

| Method | Path | Swagger tag | Success | Errors | Auth |
|---|---|---|---|---|---|
| `POST` | `/api/v1/authentication/sign-up` | Account access | `201` | `400` `409` `422` | No |
| `POST` | `/api/v1/authentication/sign-in` | Account access | `200` + JWT cookie | `400` `401` `403` `429` | No |
| `POST` | `/api/v1/authentication/refresh` | Account access | `200` + new cookies | `401` | No (refresh cookie) |
| `POST` | `/api/v1/authentication/sign-out` | Account access | `204` | — | No |
| `POST` | `/api/v1/activation-code-verifications` | Account access | `201` | `400` `422` | No |
| `POST` | `/api/v1/account-activations` | Account access | `201` | `400` `409` `422` | No |
| `POST` | `/api/v1/password-reset-requests` | Account access | `201` | `400` | No |
| `POST` | `/api/v1/password-resets` | Account access | `201` | `400` `422` | No |
| `POST` | `/api/v1/clients` | Clients | `201` | `400` `403` | Trainer |
| `GET` | `/api/v1/clients?search&status&page&size` | Clients | `200` | `400` `403` | Trainer |
| `GET` | `/api/v1/clients/{id}` | Clients | `200` | `403` `404` | Trainer |
| `PUT` | `/api/v1/clients/{id}` | Clients | `200` | `400` `403` `404` | Trainer |
| `POST` | `/api/v1/clients/{id}/deactivations` | Clients | `201` | `403` `404` | Trainer |
| `POST` | `/api/v1/clients/{id}/activation-codes` | Clients | `201` | `403` `404` `409` | Trainer |
| `GET` | `/api/v1/clients/{id}/body-profile` | Clients | `200` | `403` `404` | Trainer |
| `PUT` | `/api/v1/clients/{id}/body-profile` | Clients | `200` | `400` `403` `404` `422` | Trainer |
| `GET` | `/api/v1/clients/{id}/assignments` | Routines | `200` | `403` | Trainer |
| `GET` | `/api/v1/clients/{id}/workout-sessions?from&to&page&size` | Workouts | `200` | `400` `403` | Trainer |
| `GET` | `/api/v1/clients/{id}/progress-reports?from&to` | Progress | `200` | `400` `403` | Trainer |
| `GET` | `/api/v1/clients/{id}/progress-charts?exerciseId&weeks` | Progress | `200` | `400` `403` | Trainer |
| `POST` | `/api/v1/exercises` | Exercises | `201` | `400` `403` `409` | Trainer |
| `GET` | `/api/v1/exercises?search&status&page&size` | Exercises | `200` | `400` `403` | Trainer |
| `GET` | `/api/v1/exercises/{id}` | Exercises | `200` | `403` `404` | Trainer |
| `DELETE` | `/api/v1/exercises/{id}` | Exercises | `204` | `403` `404` `409` | Trainer |
| `POST` | `/api/v1/exercises/{id}/archivals` | Exercises | `201` | `403` `404` | Trainer |
| `POST` | `/api/v1/exercises/{id}/restorations` | Exercises | `201` | `403` `404` | Trainer |
| `PUT` | `/api/v1/exercises/{id}/machine-link` | Exercises | `200` | `400` `403` `404` `422` | Trainer |
| `POST` | `/api/v1/routines` | Routines | `201` | `400` `403` `404` `422` | Trainer |
| `GET` | `/api/v1/routines?page&size` | Routines | `200` | `400` `403` | Trainer |
| `GET` | `/api/v1/routines/{id}` | Routines | `200` | `403` `404` | Trainer |
| `PUT` | `/api/v1/routines/{id}` | Routines | `200` | `400` `403` `404` `422` | Trainer |
| `GET` | `/api/v1/routines/{id}/versions` | Routines | `200` | `403` `404` | Trainer |
| `POST` | `/api/v1/routines/{id}/duplicates` | Routines | `201` | `400` `403` `404` | Trainer |
| `POST` | `/api/v1/routines/{id}/assignments` | Routines | `201` | `400` `403` `404` `422` | Trainer |
| `GET` | `/api/v1/client-overviews?search&status&page&size` | Clients | `200` | `400` `403` | Trainer |
| `GET` | `/api/v1/active-routines/me` | Workouts | `200` | `403` `404` | Client |
| `GET` | `/api/v1/workout-sessions?from&to&page&size` | Workouts | `200` | `400` `403` | Client |
| `GET` | `/api/v1/workout-sessions/{id}` | Workouts | `200` | `403` `404` | Client |
| `POST` | `/api/v1/workout-sessions/{id}/sets` | Workouts | `201` | `400` `403` `404` `409` `422` | Client |
| `POST` | `/api/v1/workout-sessions/{id}/corrections` | Workouts | `201` | `400` `403` `404` `409` `422` | Client |
| `POST` | `/api/v1/workout-sessions/{id}/completions` | Workouts | `201` | `403` `404` `409` | Client |
| `GET` | `/api/v1/progress-charts/me?exerciseId&weeks` | Progress | `200` | `400` `403` | Client |
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

The suite has 417 tests across every layer of `iam`, `clients`, `planning`, `tracking` and
`notifications`, plus the ArchUnit boundary rules.

CI (`.github/workflows/ci.yml`) runs the full suite against an ephemeral PostgreSQL on
every push and pull request to `main`, `develop`, and `release/**` — see
[Git Workflow](#git-workflow).
