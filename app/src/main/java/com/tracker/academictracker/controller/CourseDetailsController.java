package com.tracker.academictracker.controller;

import com.academictracker.prediction.model.PredictionMode;
import com.academictracker.prediction.result.CoursePredictionResult;
import com.tracker.academictracker.model.Assessment;
import com.tracker.academictracker.model.Course;
import com.tracker.academictracker.model.Semester;
import com.tracker.academictracker.model.Student;
import com.tracker.academictracker.service.AcademicPredictionService;
import com.tracker.academictracker.service.CourseTargetService;
import com.tracker.academictracker.service.DataManager;
import com.tracker.academictracker.service.InputValidator;
import com.tracker.academictracker.ui.UiComponents;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class CourseDetailsController {

    // Labels
    @FXML private Label courseCodeLabel;
    @FXML private Label courseNameLabel;
    @FXML private Label creditsLabel;
    @FXML private Label categoryLabel;
    @FXML private Label statusLabel;
    @FXML private Label majorLabel;
    @FXML private Label currentGradeLabel;
    @FXML private Label totalWeightLabel;
    @FXML private TextField targetCourseGradeInput;
    @FXML private Label targetRequiredAverageLabel;
    @FXML private Label targetFeasibilityLabel;
    @FXML private Label targetRiskLabel;
    @FXML private Label targetConfidenceLabel;
    @FXML private Label coursePredictionResultLabel;

    // Inputs
    @FXML private TextField assessNameInput;
    @FXML private ComboBox<String> categoryInput;
    @FXML private TextField weightInput;
    @FXML private TextField maxScoreInput;
    @FXML private TextField scoreInput;

    // Table
    @FXML private TableView<Assessment> assessmentTable;
    @FXML private TableColumn<Assessment, String> colAssessName;
    @FXML private TableColumn<Assessment, String> colCategory;
    @FXML private TableColumn<Assessment, Double> colWeight;
    @FXML private TableColumn<Assessment, Assessment> colScore;
    @FXML private TableColumn<Assessment, Assessment> colAction;

    // Data references
    private Student student;
    private Semester activeSemester;
    private Course currentCourse;
    private BorderPane mainContentPane; // Dùng để đổi trang ngược lại
    private final AcademicPredictionService predictionService = new AcademicPredictionService();

    public void initData(Student student, Semester activeSemester, Course course, BorderPane mainContentPane) {
        this.student = student;
        this.activeSemester = activeSemester;
        this.currentCourse = course;
        this.mainContentPane = mainContentPane;

        categoryInput.setItems(FXCollections.observableArrayList("Exam", "Quiz", "Assignment", "Project", "Other"));
        categoryInput.getSelectionModel().selectFirst();

        updateCourseInfo();
        setupTable();
        refreshData();
    }

    private void updateCourseInfo() {
        courseCodeLabel.setText(currentCourse.getCourseCode());
        courseNameLabel.setText(currentCourse.getCourseName());
        creditsLabel.setText(String.valueOf(currentCourse.getCredits()));
        categoryLabel.setText(currentCourse.getCategory());
        statusLabel.setText(currentCourse.needsFinalization() ? "Needs finalization" : currentCourse.getStatus().toString());
        UiComponents.setSingleTone(statusLabel, "badge",
                currentCourse.needsFinalization() ? "warning-badge" : statusTone(currentCourse.getStatus().toString()));
        majorLabel.setText(Boolean.TRUE.equals(currentCourse.getMajorCourse()) ? "Major" : "General");
        UiComponents.setSingleTone(majorLabel, "badge", Boolean.TRUE.equals(currentCourse.getMajorCourse()) ? "major-badge" : "neutral-badge");
    }

    private void refreshData() {
        updateCourseInfo();
        // Cập nhật Table
        if (currentCourse.getAssessments() != null) {
            assessmentTable.setItems(FXCollections.observableArrayList(currentCourse.getAssessments()));
        }

        // Cập nhật Total Weight
        double totalWeight = currentCourse.getTotalWeight();
        totalWeightLabel.setText(String.format("%.1f%%", totalWeight));
        if (totalWeight > 100.0) {
            UiComponents.setSingleTone(totalWeightLabel, "section-title", "text-danger");
        } else {
            UiComponents.setSingleTone(totalWeightLabel, "section-title", "");
        }

        // Cập nhật Current Grade
        double grade = currentCourse.getCurrentGrade();
        if (grade == 0.0 && currentCourse.getAssessments().stream().noneMatch(Assessment::isGraded)) {
            currentGradeLabel.setText("N/A");
            UiComponents.setSingleTone(currentGradeLabel, "metric-value", "text-warning");
        } else {
            currentGradeLabel.setText(String.format("%.1f%%", grade));
            UiComponents.setSingleTone(currentGradeLabel, "metric-value", "text-info");
        }
    }

    private void setupTable() {
        UiComponents.configureResponsiveTable(assessmentTable, 340);

        colAssessName.setCellValueFactory(new PropertyValueFactory<>("assessmentName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colWeight.setCellValueFactory(new PropertyValueFactory<>("weight"));

        // Custom cột Score: Hiển thị dạng "85.0 / 100.0"
        colScore.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        colScore.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Assessment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    if (item.isGraded()) {
                        setText(item.getScore() + " / " + item.getMaxScore());
                        getStyleClass().remove("muted-text");
                    } else {
                        setText("N/A / " + item.getMaxScore());
                        if (!getStyleClass().contains("muted-text")) {
                            getStyleClass().add("muted-text");
                        }
                    }
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Custom cột Action: Nút Xóa
        colAction.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        colAction.setCellFactory(column -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(10, editBtn, deleteBtn);

            {
                pane.setAlignment(Pos.CENTER);
                editBtn.getStyleClass().add("secondary-button");
                deleteBtn.getStyleClass().add("danger-button");

                // --- NEW: EDIT BUTTON LOGIC ---
                editBtn.setOnAction(e -> {
                    Assessment a = getTableRow() == null ? null : getTableRow().getItem();
                    if (a == null) return;
                    TextInputDialog dialog = new TextInputDialog(a.isGraded() ? String.valueOf(a.getScore()) : "");
                    dialog.setTitle("Edit Score");
                    dialog.setHeaderText("Update score for: " + a.getAssessmentName());
                    dialog.setContentText("Enter new score (Max: " + a.getMaxScore() + ")\nLeave blank to mark as ungraded:");

                    dialog.showAndWait().ifPresent(newScore -> {
                        Double previousScore = a.getScore();
                        try {
                            if (newScore.trim().isEmpty()) {
                                a.setScore(null); // Removes the score so it goes back to N/A
                            } else {
                                double score = InputValidator.parseScoreAgainstMax(newScore, "Score", a.getMaxScore());
                                a.setScore(score);
                            }
                            // Save to hard drive and refresh UI
                            if (saveData()) {
                                refreshData();
                                getTableView().refresh();
                            } else {
                                a.setScore(previousScore);
                                refreshData();
                                getTableView().refresh();
                            }
                        } catch (Exception ex) {
                            a.setScore(previousScore);
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid score. It must be a number between 0 and " + a.getMaxScore() + ".");
                            alert.show();
                        }
                    });
                });

                // --- EXISTING: DELETE BUTTON LOGIC ---
                deleteBtn.setOnAction(e -> {
                    Assessment a = getTableRow() == null ? null : getTableRow().getItem();
                    if (a == null) return;
                    int originalIndex = currentCourse.getAssessments().indexOf(a);
                    currentCourse.removeAssessment(a);
                    if (!saveData()) {
                        if (originalIndex >= 0 && originalIndex <= currentCourse.getAssessments().size()) {
                            currentCourse.getAssessments().add(originalIndex, a);
                        } else {
                            currentCourse.addAssessment(a);
                        }
                    }
                    refreshData();
                });
            }

            @Override
            protected void updateItem(Assessment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    @FXML
    private void saveAssessment() {
        try {
            String name = assessNameInput.getText().trim();
            String category = categoryInput.getValue();
            double weight = InputValidator.parseWeightPercent(weightInput.getText());
            double maxScore = InputValidator.parseFiniteDouble(maxScoreInput.getText(), "Max score");

            if (name.isEmpty()) {
                showError("Assessment name is required.");
                return;
            }
            if (category == null || category.isBlank()) {
                category = "General";
            }
            if (maxScore <= 0.0) {
                showError("Max score must be greater than 0.");
                return;
            }
            if (currentCourse.getTotalWeight() + weight > 100.0 + 0.001) {
                showError("Total course weight cannot exceed 100%.");
                return;
            }

            Assessment newAss = new Assessment(name, category, weight, maxScore);

            // Xử lý điểm (Có thể để trống nếu chưa có điểm)
            String scoreTxt = scoreInput.getText().trim();
            if (!scoreTxt.isEmpty()) {
                newAss.setScore(InputValidator.parseScoreAgainstMax(scoreTxt, "Score", maxScore));
            }

            currentCourse.addAssessment(newAss);
            if (!saveData()) {
                currentCourse.removeAssessment(newAss);
                return;
            }
            refreshData();

            // Clear input
            assessNameInput.clear(); weightInput.clear(); maxScoreInput.clear(); scoreInput.clear();
        } catch (Exception e) {
            showError("Invalid assessment data: " + e.getMessage());
        }
    }

    private boolean saveData() {
        try {
            DataManager.saveStudent(student);
            return true;
        } catch (IOException e) {
            showError("Failed to save data: " + e.getMessage());
            return false;
        }
    }

    // Nút QUAY LẠI TRANG COURSE ROSTER
    @FXML
    private void handleBackToRoster() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/tracker/academictracker/CourseRoster.fxml"));
            Node view = loader.load();
            CourseRosterController controller = loader.getController();

            // Trả lại BorderPane cho Roster
            controller.initData(student, activeSemester, mainContentPane);
            mainContentPane.setCenter(view);
        } catch (IOException e) {
            showError("Cannot return to Courses view: " + e.getMessage());
        }
    }
    @FXML
    private void calculateCourseTarget() {
        try {
            String input = targetCourseGradeInput.getText().trim();
            if (input.isEmpty()) return;

            double targetGrade = InputValidator.parseScore100(input, "Course target score");
            CourseTargetService.updateTargetScore(currentCourse, targetGrade, () -> DataManager.saveStudent(student));
            CoursePredictionResult result = predictionService.predictCourse(student, currentCourse, targetGrade, PredictionMode.DRAFT);
            updateTargetSummary(result);
            coursePredictionResultLabel.setText(result.explanationVi() + "\n" + result.riskExplanation());
            if (!result.valid() || result.requiredScoreForTarget() > 100.0) {
                UiComponents.setSingleTone(coursePredictionResultLabel, "body-text", "text-danger");
            } else if (result.passRiskLevel().name().equals("CRITICAL") || result.passRiskLevel().name().equals("RISKY")) {
                UiComponents.setSingleTone(coursePredictionResultLabel, "body-text", "text-warning");
            } else {
                UiComponents.setSingleTone(coursePredictionResultLabel, "body-text", "text-info");
            }

        } catch (Exception e) {
            coursePredictionResultLabel.setText(e.getMessage() == null ? "Please enter a valid number (e.g., 85)." : e.getMessage());
            UiComponents.setSingleTone(coursePredictionResultLabel, "body-text", "text-danger");
        }
    }

    private void updateTargetSummary(CoursePredictionResult result) {
        targetRequiredAverageLabel.setText(formatRequired(result.requiredScoreForTarget()));
        targetFeasibilityLabel.setText(result.feasibilityLabel().replace("_", " "));
        UiComponents.setSingleTone(targetFeasibilityLabel, "badge", feasibilityTone(result.feasibilityLabel()));
        targetRiskLabel.setText(result.passRiskLevel().name());
        UiComponents.setSingleTone(targetRiskLabel, "badge", riskTone(result.passRiskLevel().name()));
        targetConfidenceLabel.setText(result.profileConfidence());
        UiComponents.setSingleTone(targetConfidenceLabel, "badge", confidenceTone(result.profileConfidence()));
    }

    private String formatRequired(double value) {
        if (!Double.isFinite(value)) {
            return "N/A";
        }
        if (value > 100.0) {
            return String.format("%.1f%% (not achievable)", value);
        }
        if (value < 0.0) {
            return "Above target pace";
        }
        return String.format("%.1f%%", value);
    }

    private String feasibilityTone(String value) {
        String normalized = value == null ? "" : value.toUpperCase();
        if (normalized.contains("IMPOSSIBLE")) {
            return "feasibility-danger";
        }
        if (normalized.contains("CHALLENGING") || normalized.contains("HIGH_RISK") || normalized.contains("PACE")) {
            return "feasibility-warning";
        }
        return "feasibility-positive";
    }

    private String riskTone(String value) {
        String normalized = value == null ? "" : value.toUpperCase();
        if (normalized.contains("CRITICAL") || normalized.contains("RISKY")) {
            return "danger-badge";
        }
        if (normalized.contains("WATCH")) {
            return "warning-badge";
        }
        return "success-badge";
    }

    private String confidenceTone(String value) {
        return switch (value == null ? "" : value.toUpperCase()) {
            case "HIGH" -> "confidence-high";
            case "MEDIUM" -> "confidence-medium";
            case "LOW" -> "confidence-low";
            default -> "neutral-badge";
        };
    }

    private String statusTone(String value) {
        String normalized = value == null ? "" : value.toUpperCase();
        if (normalized.contains("COMPLETED")) {
            return "completed-badge";
        }
        if (normalized.contains("ACTIVE") || normalized.contains("PROGRESS")) {
            return "in-progress-badge";
        }
        if (normalized.contains("PLANNED")) {
            return "planned-badge";
        }
        return "neutral-badge";
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
