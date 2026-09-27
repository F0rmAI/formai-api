package com.formai.api.planning.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public class RoutineVersionEmbeddable {

    @Column(name = "number", nullable = false)
    private int number;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "author", nullable = false, length = 64)
    private String author;

    // A version is immutable and always read as a whole, so its sessions are one JSON document.
    @Column(name = "sessions_json", nullable = false, columnDefinition = "text")
    private String sessionsJson;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public RoutineVersionEmbeddable() {
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSessionsJson() {
        return sessionsJson;
    }

    public void setSessionsJson(String sessionsJson) {
        this.sessionsJson = sessionsJson;
    }
}
