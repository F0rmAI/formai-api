# AGENTS.md

This file provides guidance to AI coding agents (Claude Code, Codex, Cursor and others) when working with code in this repository. `CLAUDE.md` only imports this file and `MEMORY.md`, so this is the single source to edit.

## Commands

Requires JDK 25, Maven 3.9+ and Docker (PostgreSQL 17). `.env` is gitignored: `cp .env.example .env` and set `DB_PASSWORD` and `JWT_SECRET` (`openssl rand -base64 64`, no default, the app will not start without it). `spring-dotenv` loads `.env` automatically, so nothing needs exporting. `TRACKING_DAILY_JOB_CRON` overrides the daily job schedule (default `0 5 0 * * *`); `JWT_EXPIRATION_MINUTES` (30) and `JWT_REFRESH_EXPIRATION_DAYS` (7) set the session lifetimes. `JWT_COOKIE_SECURE=false` drops the `Secure` flag of the session cookies, only for local development over HTTP with the mobile app (iOS and Android do not send `Secure` cookies to `http://localhost`).

```bash
docker compose up -d                              # PostgreSQL only (db: formai_db)
mvn spring-boot:run                               # API on :8080, GET /actuator/health
mvn test                                          # full suite (no database needed)
mvn test -Dtest=ArchitectureTest                  # ArchUnit module-boundary rules
mvn test -Dtest=UserCommandServiceImplTest#<name> # single test method
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`. There is no lint/format tool configured.

## Architecture

Modular monolith with DDD: one Spring Boot deployable, base package `com.formai.api`. Each Bounded Context is a top-level package (`iam`, `clients`, `planning`, `tracking` and `notifications`) with four layers: `domain` / `application` / `infrastructure` / `interfaces`. `shared` is cross-cutting infrastructure, not a Bounded Context.

Boundaries are enforced by `ArchitectureTest` (ArchUnit) and fail the build:
- `domain` must not depend on Spring, JPA or Hibernate.
- A context may reach another one only through its `interfaces.acl` facade (Open Host Service, e.g. `IamContextFacade`) or its `domain.model.events`. Neutral cross-context types go in `shared.contracts.<context>`.
- No cycles between contexts; `shared` must not depend on any context.

Conventions that span several files:
- **Double repository**: `domain.repositories.UserRepository` (domain interface) is implemented by `infrastructure.persistence.repositories.UserRepositoryImpl`, which wraps the Spring Data `UserJpaRepository` and maps with MapStruct (`UserJpaMapper`) between domain aggregates and `*JpaEntity`. JPA entities never leave `infrastructure`.
- **CQRS-lite**: `application.internal.commandservices` and `queryservices`, driven by `domain.model.commands` / `queries`. REST resources are converted by assemblers in `interfaces.rest.transform`.
- **Domain events** are in-process, published with Spring's `ApplicationEventPublisher` from `@Transactional` command services (no broker). Cross-context listeners are `@TransactionalEventListener(AFTER_COMMIT)` + `@Transactional(REQUIRES_NEW)`: one transaction, one aggregate. A listener only fires if the publisher runs in a transaction; `iam` publishes without one, so the `clients` listeners on `iam` events set `fallbackExecution = true`.
- **Flyway per module**: Spring's Flyway is disabled in `application.yml`; `shared/config/FlywayConfig` registers one Flyway per module with its own schema (e.g. `iam`), history table (`flyway_iam_users`) and location (`db/migration/iam`). A new module needs its own bean there. `ddl-auto` is `validate`, so entities must match the migrations.
- **No open-in-view** (`open-in-view: false`) and command services often run without a transaction, so `*RepositoryImpl` maps entities to domain inside the repository call and entity collections are `EAGER`. Two eager `List` collections on one entity need `@OrderColumn` (see `WorkoutSessionJpaEntity`) or Hibernate fails with multiple bags.
- **Same simple class name in two modules** (e.g. `RoutineAssignedEventHandler`, `ExternalClientsService`) clashes as a Spring bean name: give it an explicit prefixed name, e.g. `@Component("trackingRoutineAssignedEventHandler")`.
- **REST layout**: `/…/me` routes serve the signed-in client (mobile app); `/clients/{id}/…` routes serve the trainer looking at one of their clients. Those `/clients/{id}/…` sub-resources are *composition controllers* living in the module that owns the data (`planning` for assignments, `tracking` for workout sessions, progress reports and charts), because serving them from `clients` would create a dependency cycle.
- **Swagger tags** group operations by purpose, not by module. Names and descriptions are constants in `shared/interfaces/rest/ApiTags`; every `@Tag` must use both (`@Tag(name = ApiTags.X, description = ApiTags.X_DESCRIPTION)`) or springdoc emits conflicting duplicate tags. A new tag also goes in the ordered list of `OpenApiConfiguration.tagsInJourneyOrder()`.
- **Scheduled jobs** (`@EnableScheduling` on `FormaiApplication`): `tracking` `WorkoutSessionDailyJob` skips yesterday's unrecorded sessions and schedules today's, one `WorkoutSession` per client per **training day** of the assignment (`ActiveRoutine.trainsOn`), rotating through the routine's days, so rest days are never skipped sessions. The daily closing gives every past session a final status: `SKIPPED` without records, otherwise `COMPLETED` (every prescribed set recorded) or `PARTIAL`. The job also runs on `ApplicationReadyEvent`, and tracking's `RoutineAssignedEventHandler` schedules today's session right after an assignment that starts today; scheduling drops an untouched session whose routine stopped applying that day. `notifications` `PendingNotificationDispatcherJob` sends the outbox (`Notification` rows) every `NOTIFICATIONS_DISPATCHER_DELAY`, retrying a failed one up to `Notification.MAX_ATTEMPTS` (5).
- **Email**: `BrevoApiEmailDeliveryService` sends through Brevo's transactional HTTP API (`POST /v3/smtp/email`, `BREVO_API_KEY`; sender from `SMTP_FROM_EMAIL` / `SMTP_FROM_NAME`) because Railway blocks outbound SMTP below the Pro plan. Never log the destination, the body or the provider response: the body carries the reset token and the response can echo the address, which is why `Notification` replaces it with `REDACTED_BODY` once the delivery is closed (sent, cancelled or out of attempts). Without the variables the app still starts and deliveries stay `FAILED`. Tests use `MockRestServiceServer`: the suite must never send a real email.
- **Auth**: own JWT issued by `iam`, carried in an httpOnly `SameSite=Lax` cookie (never in the body or an `Authorization` header). `JwtAuthenticationFilter` reads the cookie and maps the `roles` claim to `ROLE_<name>` authorities; `shared/config/SecurityConfig` restricts the trainer routes (`/clients/**`, `/exercises/**`, `/routines/**`, `/client-overviews/**`) to `ROLE_TRAINER` and the client routes (`/active-routines/**`, `/workout-sessions/**`, `/progress-charts/**`, `/client-profiles/**`) to `ROLE_CLIENT`; anything else just needs a JWT. Trainer data is always scoped by `holderId` = `Authentication.getName()` (the JWT `sub`); a client's id is the id of their iam account, so it is also their `sub`. Public: `/api/v1/authentication/**` plus `POST` on `/api/v1/activation-code-verifications`, `/api/v1/account-activations`, `/api/v1/password-reset-requests`, `/api/v1/password-resets`. Everything else needs a JWT; there is no permit-all dev mode.
- **Sessions (NFR-007)**: sign-in also sets a `refresh_token` cookie (httpOnly, 7 days, `path=/api/v1/authentication` so it only travels to sign-in, refresh and sign-out). `POST /api/v1/authentication/refresh` (a verb, like `sign-in` and `sign-out`, as a documented exception to the noun sub-resources) rotates it: the used token is revoked and a new pair of cookies is issued; presenting an already-revoked token revokes every session of the account (theft detection). Sign-out revokes it, and `IamContextFacade.disableAccount` revokes all of the account's tokens. Like the password reset token, only its SHA-256 hash is stored (`iam.refresh_tokens`). Passwords use bcrypt cost 12 (NFR-005), and account activation requires the `consentVersion` of the text shown, stored with the acceptance date (NFR-021). A client account has no email until that activation: the trainer registers the client with the name only and the client chooses the email in the mobile app (`409` if another account has it).
- **Changing trainer**: from the second redemption on the email already has an account. Redeeming another trainer's code with that email and the account's current password moves the client instead of creating one: `iam` reactivates the existing account and discards the pending one (`ClientAccountTransferred`), `clients` gives the existing record to the new trainer and deletes the invited one (`ClientTransferred`), and `planning` closes the current assignment and hands the plan over. History follows because everything is keyed by the client id, which never changes. A wrong password answers the same `409` as a taken email; a deactivated client may reset the password first.
- **Event handlers** delegate to a command service (never a repository) and state in a class comment what happens if they fail after the publisher's commit: self-healing, outbox, or none and why. A listener of an `iam` event runs inside the request (no transaction there), so a failure that must not reach the caller is caught outside the transaction, with `TransactionTemplate` (see `PasswordResetRequestedEventHandler`).
- **Errors**: each module has `@ControllerAdvice` classes at `HIGHEST_PRECEDENCE` that map domain exceptions to RFC 7807 `ProblemDetail`; `shared/interfaces/rest/GlobalExceptionHandler` is the `LOWEST_PRECEDENCE` fallback and returns a generic 500 without the exception message. Sign-in and password-reset-request answers are deliberately identical whether or not the email exists (anti-enumeration).
- **Sign-in is channel-aware**: clients only from `MOBILE_APP`, trainers and administrators only from `WEB_PLATFORM`; any other combination is `403`. Five consecutive failures lock the account for 15 minutes (`429`).

## Testing

Tests run without a database. The `iam` module has a worked example per layer to copy for new modules: `UserTest` (domain), `UserCommandServiceImplTest` / `UserQueryServiceImplTest` (application), `IamContextFacadeImplTest` (facade), `UserRepositoryImplTest` (persistence), and one `@WebMvcTest` per controller that imports the real `SecurityConfig`. Controller tests authenticate with a request post-processor, `.with(user(HOLDER_ID).roles("TRAINER"))`, not `@WithMockUser`: controllers read the trainer or client id from `Authentication.getName()`, so the username must be a real id. Every module (`clients`, `planning`, `tracking`, `notifications`) follows the same layout, with a `*TestData` fixture class per module. Scheduled jobs and event handlers are plain Mockito tests that call the date-parameterised method (e.g. `runFor(TODAY)`), not the `LocalDate.now()` entry point.

## Git

Git Flow: `main` is protected (PRs only, CI `build` job must pass, tagged per release), `develop` is the integration branch, and work goes in `feature/<short-description>` branches cut from `develop`. Commits follow the conventional commits: `type(scope): subject`, English, all lowercase, imperative, no body and no `Co-Authored-By` (uses the `qs-conv-commit` skill if available).

## Optional skills (Quedena Studio)

This is team work. Some members use private Claude Code skills from Quedena Studio; they cannot be
shared, so they are optional helpers, never a requirement. The rules they encode are already written
in this file, which is what the whole team follows. An agent without them must follow `AGENTS.md`.

| Skill | What it adds when available |
|---|---|
| `qs-monolith-serv` | The modular-monolith canon behind this architecture: module anatomy, OHS/ACL, in-process events, ArchUnit, persistence and testing per layer |
| `qs-framework` | The shared DDD canon (naming, aggregates, value objects, CQRS-lite, MapStruct, assemblers) |
| `qs-conv-commit` | The commit convention described in *Git* above |

## Memory
- Start by reading `MEMORY.md` to understand the project's status and decisions made.
- Upon completing a task, update: current status, key decisions (and the reasoning behind them), and pitfalls to avoid.
- Keep it concise (max. ~50 lines): summarize or remove information that is no longer relevant.
- If something becomes a permanent rule, propose moving it to `AGENTS.md` instead of keeping it in the memory file.
- Never store sensitive data (keys, tokens, personal information).

## Delivered user stories

`README.md` § *User Stories Coverage* lists every user story that is ready to use, with its
endpoints. Start any new feature from that table: build on what it lists and change it only
when the new story requires it, then add the new rows there. The acceptance criteria (FR-xxx /
US-xxx) live outside the repo in `FormAI_Requirements_Specification.md` (also loaded in the
NotebookLM notebook "FormAI"); check a story's criteria there before implementing or validating it.