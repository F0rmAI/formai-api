# MEMORY.md - FormAI API

Inter-session project memory. This file contains about 50 lines: summarize or remove content that no longer adds value.

## Current status (2026-10-01)
- The TP (MVP, Sprints 1–2) backend is complete: US-001…US-017 plus US-033/US-034 are delivered. US-005
  sends the reset email through Brevo's SMTP relay (branch `feature/brevo-email-delivery`, verified with a
  real send). US-004 also has `POST /activation-code-verifications` (FE-MOB-003): the app checks the code
  before asking for a password, without redeeming it. The trainer registers a client with the name only;
  the client sets the email on activation (branch `fix/register-client-email`). `GET /client-profiles/me` gives the
  signed-in client their own name and email (the app greets by name). Suite: 424 tests. Audit against the
  `qs-monolith-serv` canon: ~85 % (2026-10-01).
- Ahead of TB2 (not required for the TP): US-018 progress charts; US-030 machine link (answers 422 until the
  machine catalog exists).
- Class diagrams come from one model, `Diagramas/Clases/_modelo/formai-api.yaml` (outside the repo, package
  `com.formai.api`); never edit the `.puml`. `FORMAI_API_REPO=<this repo> ./build.sh` rebuilds TP and TF and
  compares them with the code (TF 414/414; TP lacks only the 5 US-030 classes, `[TB2]` on purpose).

## Decisions (and why)
- Swagger tags by purpose, defined once in `shared/interfaces/rest/ApiTags`: controllers of several modules
  share a tag, and differing descriptions made springdoc emit conflicting duplicates.
- Today's `WorkoutSession` is scheduled on assignment, by the 00:05 job and on startup: with only the cron, a
  routine starting today, or a server that was down at 00:05, left the client with nothing to record (FR-010).
- Sessions: 30-min access JWT + 7-day rotating refresh cookie with theft detection (NFR-007); bcrypt cost 12
  (NFR-005); consent stored with the text version (NFR-021; accounts activated before are marked `legacy`).
- `/clients/{id}/assignments` answers 403 for another trainer's client, like tracking's routes (FR-016).
- `AGENTS.md` is the single instruction file; `CLAUDE.md` only imports it and this file.
- The client's email is chosen by the client on activation, not typed by the trainer: `iam.users.email` and
  `clients.clients.email` are nullable until then (iam V6, clients V2) and `AccountActivated` carries the email.
  The web form must stop asking for it (the API ignores it if sent).
- Email goes through Brevo over SMTP (not its HTTP API, as the C4 said): the team's Brevo setup is SMTP and
  Spring's `JavaMailSender` needs no vendor SDK. Failed emails are retried up to 5 times, to protect the
  daily quota and because the reset link expires in 30 minutes anyway. The email body is redacted once the
  delivery is closed, so the reset link is not kept in the database.

## Lessons learned and mistakes to avoid
- Controller tests: use `.with(user(<real id>).roles(...))`, not `@WithMockUser` (holderId = `getName()`).
- An exception in an `ApplicationReadyEvent` listener aborts startup: catch it.
- Two eager `List` collections on one entity need `@OrderColumn` (multiple bags).
- Same simple class name in two modules clashes as a bean name: name the bean explicitly.
- Local runs need Docker Desktop up; restarting it may leave the database empty (Flyway rebuilds it).
- A `try/catch` inside a `@Transactional` listener does not stop the failure: the rollback resurfaces as
  `UnexpectedRollbackException`. Catch outside the transaction (`TransactionTemplate`).

## Open product questions (need a team decision)
- Rest days: a session is scheduled every calendar day, so unrecorded rest days count as SKIPPED and lower
  adherence. Option: training days per assignment.
- COMPLETED counts an exercise as done with one recorded set; should it require every prescribed set?
- A session with records that is never finished stays PENDING; FR-014 does not say how to close it.
- Handlers without self-healing (stated in their comments): `UserRegistered`→Trainer (iam does not keep the
  name), `ClientDeactivated`→close assignment, and tracking's `RoutineAssigned` sync (no sessions until reassigned).

## Next steps (to operate the MVP)
- Set the `SMTP_*` variables in the demo environment and in every teammate's `.env`.
- Deployment: Dockerfile + `backend` service in compose, Caddy with TLS and a subdomain, on the Linux VM.
- Daily Postgres backup with 7-day retention and one tested restore; external monitor on `/actuator/health`.
- Quality evidence: k6 (P95 < 300 ms), OWASP ZAP baseline, JaCoCo ≥ 70 % on domain/application.
- Audit backlog: clients↔iam share one transaction on client registration/deactivation; iam and tracking
  command services lack `@Transactional` (sign-in must keep its failed-attempt count on error); ArchUnit
  layered rule; tests with a real JWT and a context-load test; SMTP send inside the DB transaction.
- The web front end needs a `/password-reset` page: the emailed link points to `PASSWORD_RESET_URL`.
