package studio.quedena.template.iam.domain.model.valueobjects;

/**
 * Closed set of platform-wide roles a User can accumulate.
 * <p>
 * REGISTERED_USER is the baseline every account gets on sign-up. ADMINISTRATOR is
 * additive, not a replacement — an account can hold both at once (an operator who
 * also uses the app as a regular user). Roles are additive by design: granting one
 * never removes another.
 * <p>
 * TEMPLATE NOTE: when deriving a project from this template, add new values here for
 * any additional access tier the domain needs (e.g. SUPPORT_AGENT, BILLING_MANAGER),
 * and wire the corresponding SecurityConfig matcher — see the example there.
 */
public enum Role {
    REGISTERED_USER,
    ADMINISTRATOR
}
