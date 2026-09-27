package com.formai.api.tracking.domain.model.valueobjects;

public record Pagination(int page, int size) {

    public Pagination {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Page must be 0 or more and size 1 or more");
        }
    }
}
