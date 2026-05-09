package com.tracker.academictracker.ui;

public enum AppTheme {
    LIGHT("Light"),
    DARK("Dark");

    private final String displayName;

    AppTheme(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AppTheme fromStorage(String value) {
        if (value == null || value.isBlank()) {
            return LIGHT;
        }
        for (AppTheme theme : values()) {
            if (theme.name().equalsIgnoreCase(value) || theme.displayName.equalsIgnoreCase(value)) {
                return theme;
            }
        }
        return LIGHT;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
