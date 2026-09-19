package studio.quedena.template.iam.domain.model.aggregates;

import studio.quedena.template.iam.domain.model.valueobjects.Email;
import studio.quedena.template.iam.domain.model.valueobjects.HashedPassword;
import studio.quedena.template.iam.domain.model.valueobjects.Role;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class User {

    private UUID id;
    private Email email;
    private HashedPassword hashedPassword;
    private Set<Role> roles;
    private Instant createdAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public User() {
    }

    private User(Email email, HashedPassword hashedPassword) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.roles = EnumSet.of(Role.REGISTERED_USER);
        this.createdAt = Instant.now();
    }

    public static User register(Email email, HashedPassword hashedPassword) {
        return new User(email, hashedPassword);
    }

    // Additive: grants ADMINISTRATOR without ever removing REGISTERED_USER, so an
    // operator keeps using the app normally while also holding elevated access.
    public void grantAdministratorRole() {
        this.roles.add(Role.ADMINISTRATOR);
    }

    public UUID getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public HashedPassword getHashedPassword() {
        return hashedPassword;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public void setHashedPassword(HashedPassword hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
