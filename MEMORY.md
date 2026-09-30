# MEMORY.md - FormAI API

Inter-session project memory. This file contains about 50 lines: summarize or remove content that no longer adds value.

## Current status (2026-09-30)
- `develop` holds the TP (MVP, Sprints 1–2) backend: US-001…US-017 plus US-033/US-034 are delivered; US-005 is
  partial (the reset email is only logged until an email provider is chosen). Suite: 399 tests, CI green.
- Ahead of TB2 (not required for the TP): US-018 progress charts; US-030 machine link (answers 422 until the
  machine catalog exists).
- Code matches the TP class diagrams at ~90–100 % per context. The diagrams still lack `RefreshToken`,
  `ConsentAcceptance.version`, the refresh endpoint and the US-030 slice.

## Decisions (and why)
- Swagger tags by purpose, defined once in `shared/interfaces/rest/ApiTags`: controllers of several modules
  share a tag, and differing descriptions made springdoc emit conflicting duplicates.
- Today's `WorkoutSession` is scheduled on assignment, by the 00:05 job and on startup: with only the cron, a
  routine starting today, or a server that was down at 00:05, left the client with nothing to record (FR-010).
- Sessions: 30-min access JWT + 7-day rotating refresh cookie with theft detection (NFR-007); bcrypt cost 12
  (NFR-005); consent stored with the text version (NFR-021; accounts activated before are marked `legacy`).
- `/clients/{id}/assignments` answers 403 for another trainer's client, like tracking's routes (FR-016).
- `AGENTS.md` is the single instruction file; `CLAUDE.md` only imports it and this file.

## Lessons learned and mistakes to avoid
- Controller tests: use `.with(user(<real id>).roles(...))`, not `@WithMockUser` (holderId = `getName()`).
- An exception in an `ApplicationReadyEvent` listener aborts startup: catch it.
- Two eager `List` collections on one entity need `@OrderColumn` (multiple bags).
- Same simple class name in two modules clashes as a bean name: name the bean explicitly.
- Local runs need Docker Desktop up; restarting it may leave the database empty (Flyway rebuilds it).

## Open product questions (need a team decision)
- Rest days: a session is scheduled every calendar day, so unrecorded rest days count as SKIPPED and lower
  adherence. Option: training days per assignment.
- COMPLETED counts an exercise as done with one recorded set; should it require every prescribed set?
- A session with records that is never finished stays PENDING; FR-014 does not say how to close it.
- `notifications.body` stores the reset link with the raw token and keeps it after sending; retries have no cap.

## Next steps (to operate the MVP)
- Choose the email provider (HTTPS API) and replace `SmtpEmailDeliveryService` (US-005).
- Deployment: Dockerfile + `backend` service in compose, Caddy with TLS and a subdomain, on the Linux VM.
- Daily Postgres backup with 7-day retention and one tested restore; external monitor on `/actuator/health`.
- Quality evidence: k6 (P95 < 300 ms), OWASP ZAP baseline, JaCoCo ≥ 70 % on domain/application.
- Update the TP class diagrams with the changes listed above.
