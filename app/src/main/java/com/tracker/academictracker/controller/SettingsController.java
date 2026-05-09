package com.tracker.academictracker.controller;

import com.tracker.academictracker.Main;
import com.tracker.academictracker.model.Student;
import com.tracker.academictracker.service.DataManager;
import com.tracker.academictracker.service.InputValidator;
import com.tracker.academictracker.ui.AppTheme;
import com.tracker.academictracker.ui.ThemeManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class SettingsController {

    private Student student;
    @FXML private TextField degreeTotalCreditsInput;
    @FXML private TextField majorRequiredCreditsInput;
    @FXML private ComboBox<AppTheme> themeInput;

    public void initData(Student student) {
        this.student = student;
        if (student != null && degreeTotalCreditsInput != null) {
            degreeTotalCreditsInput.setText(student.getDegreeTotalCredits() == null ? "" : String.valueOf(student.getDegreeTotalCredits()));
            majorRequiredCreditsInput.setText(student.getMajorRequiredCredits() == null ? "" : String.valueOf(student.getMajorRequiredCredits()));
        }
        if (themeInput != null) {
            themeInput.setItems(FXCollections.observableArrayList(AppTheme.values()));
            themeInput.getSelectionModel().select(AppTheme.fromStorage(student == null ? null : student.getAppTheme()));
        }
    }

    @FXML
    private void handleSaveProgramConfig() {
        try {
            student.setDegreeTotalCredits(InputValidator.parseOptionalPositiveInt(degreeTotalCreditsInput.getText(), "Degree total credits"));
            student.setMajorRequiredCredits(InputValidator.parseOptionalPositiveInt(majorRequiredCreditsInput.getText(), "Major required credits"));
            DataManager.saveStudent(student);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Program credit settings saved.");
        } catch (IllegalArgumentException | IOException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Program Config", e.getMessage());
        }
    }

    @FXML
    private void handleSaveTheme() {
        if (student == null) {
            showAlert(Alert.AlertType.ERROR, "Theme Error", "No student profile is loaded.");
            return;
        }
        AppTheme selectedTheme = themeInput == null ? AppTheme.LIGHT : themeInput.getValue();
        if (selectedTheme == null) {
            selectedTheme = AppTheme.LIGHT;
        }

        try {
            student.setAppTheme(selectedTheme.name());
            ThemeManager.setCurrentTheme(selectedTheme);
            if (themeInput != null && themeInput.getScene() != null) {
                ThemeManager.applyToScene(themeInput.getScene());
            }
            DataManager.saveStudent(student);
            showAlert(Alert.AlertType.INFORMATION, "Theme Saved", selectedTheme.getDisplayName() + " theme has been applied.");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Theme Error", "Failed to save theme: " + e.getMessage());
        }
    }

    @FXML
    private void handleSaveData() {
        try {
            DataManager.saveStudent(student);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Data saved successfully to your local drive!");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save data: " + e.getMessage());
        }
    }

    @FXML
    private void handleLoadData() {
        try {
            Student loaded = DataManager.loadStudent();
            if (loaded != null) {
                this.student = loaded;
                Main.showDashboard(loaded);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Data loaded from disk and the dashboard was refreshed.");
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load data: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Data File");
        fileChooser.setInitialFileName("academic_tracker_backup.json");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        File file = fileChooser.showSaveDialog(new Stage());
        if (file != null) {
            try {
                DataManager.exportStudent(file.toPath());
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Your data was exported to:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Export Failed", e.getMessage());
            }
        }
    }

    @FXML
    private void handleImportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Data File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            try {
                Student imported = DataManager.previewImport(file.toPath());
                DataManager.importStudent(file.toPath());
                this.student = imported;
                Main.showDashboard(imported);
                showAlert(Alert.AlertType.INFORMATION, "Import Successful", "Data imported and dashboard refreshed.");
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Import Failed", e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}
