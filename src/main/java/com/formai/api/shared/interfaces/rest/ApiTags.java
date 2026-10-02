package com.formai.api.shared.interfaces.rest;

// Swagger tags, defined once: several controllers share each tag, and springdoc keeps a separate
// entry per distinct description, so every @Tag must take both name and description from here.
// Tags group operations by purpose, not by bounded context; each description says which
// application uses it: the trainer's web platform or the client's mobile app.
public final class ApiTags {

    public static final String ACCOUNT_ACCESS = "Account access";
    public static final String ACCOUNT_ACCESS_DESCRIPTION = "Sign-up, sign-in and sign-out, client account "
            + "activation with the trainer's code, and password recovery. The issued JWT travels only in an "
            + "httpOnly cookie (see JwtCookieFactory), never in the response body.";

    public static final String CLIENTS = "Clients";
    public static final String CLIENTS_DESCRIPTION = "Trainer (web): register clients and renew their activation "
            + "code, list them with their current routine and last workout, rename or deactivate them, and keep "
            + "their body profile. Client (mobile app): own name and email.";

    public static final String EXERCISES = "Exercises";
    public static final String EXERCISES_DESCRIPTION = "Trainer (web): own catalog of reusable exercises, archive "
            + "and restore, and the link to a published machine.";

    public static final String ROUTINES = "Routines";
    public static final String ROUTINES_DESCRIPTION = "Trainer (web): routines with sessions and prescribed "
            + "exercises, their version history and duplicates, and their assignment to clients.";

    public static final String WORKOUTS = "Workouts";
    public static final String WORKOUTS_DESCRIPTION = "Client (mobile app): current routine, today's session, "
            + "record and correct sets, finish a session, and own workout history. Trainer (web): a client's "
            + "workout history.";

    public static final String PROGRESS = "Progress";
    public static final String PROGRESS_DESCRIPTION = "Trainer (web): a client's adherence report and progress "
            + "charts. Client (mobile app): own progress charts over 4, 8 or 12 weeks.";

    private ApiTags() {
    }
}
