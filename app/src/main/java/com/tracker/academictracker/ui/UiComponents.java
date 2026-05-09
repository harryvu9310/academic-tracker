package com.tracker.academictracker.ui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class UiComponents {
    private UiComponents() {
    }

    public static Label badge(String text, String toneClass) {
        Label label = new Label(text);
        label.getStyleClass().add("badge");
        if (toneClass != null && !toneClass.isBlank()) {
            label.getStyleClass().add(toneClass);
        }
        return label;
    }

    public static VBox emptyState(String title, String message) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");
        titleLabel.setWrapText(true);
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("muted-text");
        messageLabel.setWrapText(true);

        VBox box = new VBox(8, titleLabel, messageLabel);
        box.getStyleClass().add("table-empty-state");
        makeNodeGrow(box);
        return box;
    }

    public static ScrollPane createPageScrollPane(Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        configureSmoothScrollPane(scrollPane);
        return scrollPane;
    }

    public static ScrollPane createVerticalScrollPage(Node content) {
        return createPageScrollPane(content);
    }

    public static ScrollPane createBidirectionalScrollPane(Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("bidirectional-scroll");
        return scrollPane;
    }

    public static ScrollPane createTableContainer(Node table) {
        makeNodeGrow(table);
        ScrollPane scrollPane = createBidirectionalScrollPane(table);
        scrollPane.getStyleClass().add("table-scroll-container");
        return scrollPane;
    }

    public static void makeNodeGrow(Node node) {
        if (node == null) {
            return;
        }
        HBox.setHgrow(node, Priority.ALWAYS);
        VBox.setVgrow(node, Priority.ALWAYS);
        if (node instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
    }

    public static void configureSmoothScrollPane(ScrollPane scrollPane) {
        if (scrollPane == null) {
            return;
        }
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        if (!scrollPane.getStyleClass().contains("page-scroll")) {
            scrollPane.getStyleClass().add("page-scroll");
        }
    }

    public static void configureResponsiveTable(TableView<?> tableView, double preferredHeight) {
        if (tableView == null) {
            return;
        }
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tableView.setMinHeight(260);
        tableView.setPrefHeight(preferredHeight);
        tableView.setMaxWidth(Double.MAX_VALUE);
        tableView.setFixedCellSize(50);
        VBox.setVgrow(tableView, Priority.NEVER);
    }

    public static void setSingleTone(Label label, String baseClass, String toneClass) {
        if (label == null) {
            return;
        }
        label.getStyleClass().removeIf(item -> item.endsWith("-badge")
                || item.startsWith("risk-")
                || item.startsWith("confidence-")
                || item.startsWith("text-")
                || item.startsWith("feasibility-"));
        if (baseClass != null && !baseClass.isBlank() && !label.getStyleClass().contains(baseClass)) {
            label.getStyleClass().add(baseClass);
        }
        if (toneClass != null && !toneClass.isBlank()) {
            label.getStyleClass().add(toneClass);
        }
    }
}
