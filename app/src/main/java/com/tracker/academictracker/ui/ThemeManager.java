package com.tracker.academictracker.ui;

import javafx.scene.Scene;

public final class ThemeManager {
    private static final String COMMON_CSS = "/styles/common.css";
    private static final String LIGHT_CSS = "/styles/apple-light.css";
    private static final String DARK_CSS = "/styles/apple-dark.css";
    private static AppTheme currentTheme = AppTheme.LIGHT;

    private ThemeManager() {
    }

    public static AppTheme getCurrentTheme() {
        return currentTheme;
    }

    public static void setCurrentTheme(AppTheme theme) {
        currentTheme = theme == null ? AppTheme.LIGHT : theme;
    }

    public static void applyToScene(Scene scene) {
        if (scene == null) {
            return;
        }
        scene.getStylesheets().removeIf(item ->
                item.endsWith(COMMON_CSS)
                        || item.endsWith(LIGHT_CSS)
                        || item.endsWith(DARK_CSS)
        );
        scene.getStylesheets().add(resource(COMMON_CSS));
        scene.getStylesheets().add(resource(currentTheme == AppTheme.DARK ? DARK_CSS : LIGHT_CSS));
    }

    private static String resource(String path) {
        var url = ThemeManager.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("Missing stylesheet: " + path);
        }
        return url.toExternalForm();
    }
}
