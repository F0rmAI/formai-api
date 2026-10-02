package com.formai.api.tracking.domain.model.valueobjects;

public enum ProgressWindow {
    WEEKS_4(4),
    WEEKS_8(8),
    WEEKS_12(12);

    private final int weeks;

    ProgressWindow(int weeks) {
        this.weeks = weeks;
    }

    public int weeks() {
        return weeks;
    }
}
