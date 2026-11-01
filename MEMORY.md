# MEMORY.md - FormAI API

Inter-session project memory. Keep this file concise (about 50 lines); never store credentials.

## Current status (2026-10-08)
- Branch `feature/global-exercises-and-assignment-personalization`, based on develop aligned with main (0.2.0). Work is local, without commits, push or deployment.
- Global + own catalog, muscle groups, assignment load/rest overrides and trainer notes are implemented. Planning V4/V5 and tracking V4 applied successfully to local PostgreSQL.
- Full suite: 511 tests, no failures/errors/skips, including ArchitectureTest. Local API integration: 23 checks pass.
- Web–API–Android flow completed: registration, activation, assignment, personalization and a recorded set visible to the trainer. iOS 27 displays the personalized routine too.
- Requirements are v0.5.0. OneDrive class model is v3.1.0; TP/TF validation, REST and FR/US trace audits pass without warnings. TF matches all 446 backend classes.
- Evidence is outside repositories under `formai/validation/2026-10-08-global-exercises`.

## Decisions
- Global exercises have a null holder; mutations resolve only owned exercises and return 404 for globals/foreign entries. Existing exercises remain owned; duplicate checks include global and own names regardless of case.
- New exercises require a standard muscle group; legacy values still load. `GET /muscle-groups` supplies the finite list; exercise resources include origin and routineCount.
- Each assignment owns its optional note (500 chars) and overrides keyed by session order/exercise id. Blank fields remove overrides; zero is valid. Loads allow decimals; rest is an integer.
- Jackson rejects fractional JSON values for integer fields rather than silently truncating them (400).
- Planning snapshots apply only overrides whose exercise/session still exists in the latest version. Stale overrides remain harmless after routine changes.
- Personalization and routine-update handlers first sync ActiveRoutine, then schedule/refresh today's workout in a separate ordered transaction. Only untouched pending sessions change; started/closed sessions keep their prescription and version.
- The trainer registers only a name; the client chooses email/password and accepts versioned consent on activation. Another trainer's code plus the existing password transfers the client and history.
- A day belongs to the client's IANA zone, stored on ActiveRoutine. Daily processing runs every 15 minutes and catches up on startup; training days must match routine session count.
- COMPLETED requires all prescribed sets. Daily closing finishes sessions with records as PARTIAL/COMPLETED; missing sessions become SKIPPED.
- JWT: 30-minute access cookie and 7-day rotating refresh cookie. Reusing a revoked refresh token revokes the account's sessions.
- Email uses Brevo HTTP, not SMTP. Keep its API key and sender private.

## Pitfalls
- Stop the running API before Maven tests: both write target/.
- Local native apps need JWT_COOKIE_SECURE=false. Disable real outbound mail with blank BREVO_API_KEY and SMTP_FROM_EMAIL; a long dispatcher delay alone does not prevent the initial run.
- Controller tests use user(realId).roles(...), not @WithMockUser, because the holder id is Authentication.getName().
- An ApplicationReadyEvent failure aborts startup; catch it. Catching inside a transactional method does not undo a rollback-only transaction.
- Two eager List collections need @OrderColumn. Same class name in different modules needs an explicit bean name.
- Class diagrams come only from OneDrive `Diagramas/Clases/_modelo/formai-api.yaml`; never hand-edit generated .puml. build.sh renders PNGs locally before copying to OneDrive to avoid streaming timeouts.
- REST profile permits only the finite `GET /api/v1/muscle-groups` List[String] response; negative audit checks still reject other list types.

## Known limits / next work
- A transferred client's previous trainer routine has no accessible name. Tracking RoutineAssigned synchronization lacks automatic recovery if its first sync fails.
- TB2 machine catalog/recognition remains outside this increment; only own exercises can be linked by trainers.
- Existing architecture backlog: clients/iam shared transaction, missing command-service transaction boundaries, real-JWT context tests and Brevo HTTP inside a DB transaction.
- Deployment, operational backup/restore, load/security checks and production signing require their own work; this session did not deploy.
