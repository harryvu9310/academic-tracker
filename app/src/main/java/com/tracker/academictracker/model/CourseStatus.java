package com.tracker.academictracker.model;

public enum CourseStatus {
    ACTIVE("Active"),
    COMPLETED("Completed"),
    PLANNED("Planned"),
    DROPPED("Dropped");

    private final String displayName;
    CourseStatus(final String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return displayName;
    }
    @Override
    public String toString() {
        return displayName;
    }
}
