package studio.quedena.template.profiles.domain.model.aggregates;

import java.time.Instant;
import java.util.UUID;

public class Profile {

    private UUID id;
    private String holderId;
    private String firstName;
    private String lastName;
    private Instant createdAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public Profile() {
    }

    private Profile(String holderId) {
        this.id = UUID.randomUUID();
        this.holderId = holderId;
        this.createdAt = Instant.now();
    }

    public static Profile createEmptyFor(String holderId) {
        return new Profile(holderId);
    }

    public void updatePersonalData(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UUID getId() {
        return id;
    }

    public String getHolderId() {
        return holderId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
