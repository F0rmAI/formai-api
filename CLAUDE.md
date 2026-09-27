# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Requires JDK 25, Maven 3.9+ and Docker (PostgreSQL 17). `.env` is gitignored: `cp .env.example .env` and set `DB_PASSWORD` and `JWT_SECRET` (`openssl rand -base64 64`, no default, the app will not start without it).

```bash
docker compose up -d                              # PostgreSQL only (db: formai_db)
mvn spring-boot:run                               # API on :8080, GET /actuator/health
mvn test                                          # full suite (no database needed)
mvn test -Dtest=ArchitectureTest                  # ArchUnit module-boundary rules
mvn test -Dtest=UserCommandServiceImplTest#<name> # single test method
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`. There is no lint/format tool configured.

## Architecture

Modular monolith with DDD: one Spring Boot deployable, base package `com.formai.api`. Each Bounded Context is a top-level package (`iam`, `clients`, `planning` and `tracking` today; `notifications` is planned) with four layers: `domain` / `application` / `infrastructure` / `interfaces`. `shared` is cross-cutting infrastructure, not a Bounded Context.

Boundaries are enforced by `ArchitectureTest` (ArchUnit) and fail the build:
- `domain` must not depend on Spring, JPA or Hibernate.
- A context may reach another one only through its `interfaces.acl` facade (Open Host Service, e.g. `IamContextFacade`) or its `domain.model.events`. Neutral cross-context types go in `shared.contracts.<context>`.
- No cycles between contexts; `shared` must not depend on any context.

Conventions that span several files:
- **Double repository**: `domain.repositories.UserRepository` (domain interface) is implemented by `infrastructure.persistence.repositories.UserRepositoryImpl`, which wraps the Spring Data `UserJpaRepository` and maps with MapStruct (`UserJpaMapper`) between domain aggregates and `*JpaEntity`. JPA entities never leave `infrastructure`.
- **CQRS-lite**: `application.internal.commandservices` and `queryservices`, driven by `domain.model.commands` / `queries`. REST resources are converted by assemblers in `interfaces.rest.transform`.
- **Domain events** are in-process, published with Spring's `ApplicationEventPublisher` from `@Transactional` command services (no broker). Cross-context listeners are `@TransactionalEventListener(AFTER_COMMIT)` + `@Transactional(REQUIRES_NEW)`: one transaction, one aggregate. A listener only fires if the publisher runs in a transaction; `iam` publishes without one, so the `clients` listeners on `iam` events set `fallbackExecution = true`.
- **Flyway per module**: Spring's Flyway is disabled in `application.yml`; `shared/config/FlywayConfig` registers one Flyway per module with its own schema (e.g. `iam`), history table (`flyway_iam_users`) and location (`db/migration/iam`). A new module needs its own bean there. `ddl-auto` is `validate`, so entities must match the migrations.
- **Auth**: own JWT issued by `iam`, carried in an httpOnly `SameSite=Lax` cookie (never in the body or an `Authorization` header). `JwtAuthenticationFilter` reads the cookie and maps the `roles` claim to `ROLE_<name>` authorities; `shared/config/SecurityConfig` restricts the trainer routes (`/clients/**`, `/exercises/**`, `/routines/**`, `/client-overviews/**`) to `ROLE_TRAINER` and the client routes (`/active-routines/**`, `/workout-sessions/**`) to `ROLE_CLIENT`; anything else just needs a JWT. Trainer data is always scoped by `holderId` = `Authentication.getName()` (the JWT `sub`); a client's id is the id of their iam account, so it is also their `sub`. Public: `/api/v1/authentication/**` plus `POST` on `/api/v1/account-activations`, `/api/v1/password-reset-requests`, `/api/v1/password-resets`. Everything else needs a JWT; there is no permit-all dev mode.
- **Errors**: each module has `@ControllerAdvice` classes at `HIGHEST_PRECEDENCE` that map domain exceptions to RFC 7807 `ProblemDetail`; `shared/interfaces/rest/GlobalExceptionHandler` is the `LOWEST_PRECEDENCE` fallback and returns a generic 500 without the exception message. Sign-in and password-reset-request answers are deliberately identical whether or not the email exists (anti-enumeration).
- **Sign-in is channel-aware**: clients only from `MOBILE_APP`, trainers and administrators only from `WEB_PLATFORM`; any other combination is `403`. Five consecutive failures lock the account for 15 minutes (`429`).

## Testing

Tests run without a database. The `iam` module has a worked example per layer to copy for new modules: `UserTest` (domain), `UserCommandServiceImplTest` / `UserQueryServiceImplTest` (application), `IamContextFacadeImplTest` (facade), `UserRepositoryImplTest` (persistence), and one `@WebMvcTest` per controller that imports the real `SecurityConfig` and uses `@WithMockUser(roles = ...)`. `clients`, `planning` and `tracking` follow the same layout, with a `*TestData` fixture class per module.

## Git

Git Flow: `main` is protected (PRs only, CI `build` job must pass, tagged per release), `develop` is the integration branch, and work goes in `feature/<short-description>` branches cut from `develop`. Commits follow the Quedena Studio convention: `type(scope): subject`, English, all lowercase, imperative, no body and no `Co-Authored-By` (use the `qs-conv-commit` skill).
