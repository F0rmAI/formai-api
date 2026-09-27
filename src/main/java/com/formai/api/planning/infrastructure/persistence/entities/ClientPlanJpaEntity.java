package com.formai.api.planning.infrastructure.persistence.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "client_plans", schema = "planning")
public class ClientPlanJpaEntity {

    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false, unique = true)
    private UUID clientId;

    @Column(name = "holder_id", nullable = false, length = 64)
    private String holderId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_plan_assignments", schema = "planning",
            joinColumns = @JoinColumn(name = "client_plan_id"))
    @OrderColumn(name = "position")
    private List<AssignmentEmbeddable> assignments = new ArrayList<>();

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public ClientPlanJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public String getHolderId() {
        return holderId;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public List<AssignmentEmbeddable> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<AssignmentEmbeddable> assignments) {
        this.assignments = assignments;
    }
}
