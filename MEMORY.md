# MEMORY.md - FormAI API

Inter-session project memory. This file contains about 50 lines: summarize or remove content that no longer adds value.

## Current status (2026-10-02)
- The TP (MVP, Sprints 1–2) backend is complete on branch `feature/mvp-closure` (not merged yet): US-001…US-017
  and US-033…US-036 are delivered. Suite: 459 tests. Spec is at version 0.4.0.
- Ahead of TB2 (not required for the TP): US-018 progress charts; US-030 machine link (answers 422 until the
  machine catalog exists).
- Class diagrams come from one model, `Diagramas/Clases/_modelo/formai-api.yaml` (outside the repo, package
  `com.formai.api`); never edit the `.puml`. `FORMAI_API_REPO=<this repo> ./build.sh` rebuilds TP and TF and
  compares them with the code.

## Decisions (and why)
- The trainer registers a client with the name only; the client chooses the email on activation. The pending
  account only carries the activation code (iam V6, clients V2).
- Changing trainer keeps the account: from the second redemption on, the code of another trainer redeemed
  with the client's email and current password moves the client record, and so all its history, to that
  trainer. The password is the proof of ownership; without it the answer is the usual 409.
- Training days per assignment (planning V3, tracking V2): a session exists only on those days, so rest days
  no longer count as skipped and adherence means what the trainer expects.
- COMPLETED needs every prescribed set; the daily closing ends sessions with records as PARTIAL or COMPLETED,
  so every past session has a final status.
- Swagger tags by purpose in `shared/interfaces/rest/ApiTags`; `AGENTS.md` is the single instruction file.
- Sessions: 30-min JWT + 7-day rotating refresh cookie; bcrypt cost 12; consent stored with its text version.
- Email through Brevo over SMTP, retried up to 5 times; the body is redacted once the delivery is closed.

## Lessons learned and mistakes to avoid
- Controller tests: use `.with(user(<real id>).roles(...))`, not `@WithMockUser` (holderId = `getName()`).
- An exception in an `ApplicationReadyEvent` listener aborts startup: catch it.
- A `try/catch` inside a `@Transactional` listener does not stop the failure: catch outside the transaction.
- Two eager `List` collections on one entity need `@OrderColumn`; same class name in two modules needs an
  explicit bean name.
- Unit tests did not catch two date bugs that a real run did: an assignment closed "today" still applies
  today, and the first assignment in effect is not the latest. Run the flow against the API after such changes.
- Stop the running API before `mvn test`: both write `target/` and the suite fails to discover tests.
- Local runs need Docker Desktop up. To test without sending emails: `NOTIFICATIONS_DISPATCHER_DELAY=PT2H`.

## Known limits (accepted)
- After a transfer, the previous trainer's routines appear without a name in the assignment history.
- A routine assigned the same day as a transfer stays visible until the new trainer assigns one.
- Handlers without self-healing (see their comments): `UserRegistered`→Trainer, `ClientDeactivated`→close
  assignment, tracking's `RoutineAssigned` sync, and `ClientAccountTransferred` (the trainer invites again).

## Next steps (to operate the MVP)
- Front ends: the web must send `trainingDays` when assigning and stop asking for the client's email; it
  needs a `/password-reset` page. The app must send email and `consentVersion` on activation.
- Set the `SMTP_*` variables in the demo environment and in every teammate's `.env`.
- Deployment: Dockerfile + `backend` service in compose, Caddy with TLS and a subdomain, on the Linux VM.
- Daily Postgres backup with 7-day retention and one tested restore; external monitor on `/actuator/health`.
- Quality evidence: k6 (P95 < 300 ms), OWASP ZAP baseline, JaCoCo ≥ 70 % on domain/application.
- Audit backlog: clients↔iam share one transaction; iam and tracking command services lack `@Transactional`;
  ArchUnit layered rule; tests with a real JWT and a context-load test; SMTP send inside the DB transaction.
