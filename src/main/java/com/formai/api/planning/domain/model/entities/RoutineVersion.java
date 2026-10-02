package com.formai.api.planning.domain.model.entities;

import java.time.Instant;
import java.util.List;

public class RoutineVersion {

    private final int number;
    private final Instant changedAt;
    private final String author;
    private final List<RoutineSession> sessions;

    public RoutineVersion(int number, Instant changedAt, String author, List<RoutineSession> sessions) {
        this.number = number;
        this.changedAt = changedAt;
        this.author = author;
        this.sessions = List.copyOf(sessions);
    }

    public int getNumber() {
        return number;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public String getAuthor() {
        return author;
    }

    public List<RoutineSession> getSessions() {
        return sessions;
    }
}
