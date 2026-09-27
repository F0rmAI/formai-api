package com.formai.api.clients.infrastructure.persistence.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clients", schema = "clients",
        uniqueConstraints = @UniqueConstraint(columnNames = {"holder_id", "email"}))
public class ClientJpaEntity {

    @Id
    private UUID id;

    @Column(name = "holder_id", nullable = false, length = 64)
    private String holderId;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    // Stored as a plain string, not the domain ClientStatus type: this entity stays
    // framework-only and has zero dependency on the domain package.
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Embedded
    private BodyProfileEmbeddable bodyProfile;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_weight_records", schema = "clients",
            joinColumns = @JoinColumn(name = "client_id"))
    @OrderColumn(name = "position")
    private List<BodyWeightRecordEmbeddable> weightHistory = new ArrayList<>();

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public ClientJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getHolderId() {
        return holderId;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BodyProfileEmbeddable getBodyProfile() {
        return bodyProfile;
    }

    public void setBodyProfile(BodyProfileEmbeddable bodyProfile) {
        this.bodyProfile = bodyProfile;
    }

    public List<BodyWeightRecordEmbeddable> getWeightHistory() {
        return weightHistory;
    }

    public void setWeightHistory(List<BodyWeightRecordEmbeddable> weightHistory) {
        this.weightHistory = weightHistory;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }
}
