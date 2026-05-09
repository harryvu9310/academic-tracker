package com.tracker.academictracker.controller;

import com.academictracker.prediction.result.CoursePredictionResult;
import com.academictracker.prediction.result.GlobalPredictionResult;
import com.tracker.academictracker.model.Course;
import com.tracker.academictracker.model.Semester;
import com.tracker.academictracker.model.Student;
import com.tracker.academictracker.service.AcademicPredictionService;
import com.tracker.academictracker.service.DashboardTargetService;
import com.tracker.academictracker.service.DataManager;
import com.tracker.academictracker.ui.UiComponents;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.Comparator;

public class DashboardController {
    @FXML private Label studentNameLabel;
    @FXML private Label studentIdLabel;
    @FXML private Label academicStandingLabel;
    @FXML private ComboBox<Semester> activeSemesterComboBox;
    @FXML private Label termGpaLabel;
    @FXML private Label overallGpaLabel;
    @FXML private Label projectedGpaLabel;
    @FXML private Label creditsProgressLabel;
    @FXML private Label targetGapLabel;
    @FXML private TextField targetGpaInput;
    @FXML private TextField targetMajorGpaInput;
    @FXML private Label predictionResultLabel;
    @FXML private Label predictionRiskBadge;
    @FXML private Label predictionConfidenceBadge;
    @FXML private VBox predictionInsightContainer;
    @FXML private VBox priorityCoursesContainer;
    @FXML private VBox activeCoursesContainer;
    @FXML private BorderPane mainContentPane;
    @FXML private VBox dashboardContent;
    @FXML private Button btnDashboard;
    @FXML private Button btnSemesters;
    @FXML private Button btnCourseRoster;
    @FXML private Button btnSettings;

    private Student student;
    private Node dashboardHomeView;
    private final AcademicPredictionService predictionService = new AcademicPredictionService();
    private final DashboardTargetService dashboardTargetService = new DashboardTargetService(predictionService);
    private boolean activeSemesterListenerAttached;

    public void initData(Student student) {
        this.student = student;
        this.dashboardHomeView = mainContentPane == null ? null : mainContentPane.getCenter();
        selectNav(btnDashboard);

        if (student != null) {
            String fullName = student.getFullName() == null ? "" : student.getFullName();
            String studentId = student.getStudentId() == null ? "" : student.getStudentId();
            studentNameLabel.setText(fullName.isBlank() ? "Student" : fullName);
            studentIdLabel.setText("ID: " + (studentId.isBlank() ? "N/A" : studentId));
            updateAcademicStanding();
            refreshAcademicSummary();
            setupActiveSemesterCombo();
        }
    }

    private void updateAcademicStanding() {
        String standing = student.getAcademicStanding();
        academicStandingLabel.setText(standing);
        if ("Good Standing".equals(standing)) {
            UiComponents.setSingleTone(academicStandingLabel, "badge", "success-badge");
        } else if ("Satisfactory".equals(standing)) {
            UiComponents.setSingleTone(academicStandingLabel, "badge", "warning-badge");
        } else if ("New Student".equals(standing)) {
            UiComponents.setSingleTone(academicStandingLabel, "badge", "neutral-badge");
        } else {
            UiComponents.setSingleTone(academicStandingLabel, "badge", "danger-badge");
        }
    }

    private void setupActiveSemesterCombo() {
        if (student.getSemesters().isEmpty()) {
            termGpaLabel.setText("N/A");
            activeCoursesContainer.getChildren().setAll(UiComponents.emptyState(
                    "No active semester yet",
                    "Create a semester first, then add courses to start tracking GPA."
            ));
            return;
        }

        ObservableList<Semester> semesters = FXCollections.observableArrayList(student.getSemesters());
        activeSemesterComboBox.setItems(semesters);
        activeSemesterComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Semester semester) {
                return semester != null ? semester.getDisplayName() : "";
            }

            @Override
            public Semester fromString(String string) {
                return null;
            }
        });

        String savedActiveName = student.getActiveSemesterName();
        Semester semesterToSelect = semesters.stream()
                .filter(semester -> semester.getDisplayName().equals(savedActiveName))
                .findFirst()
                .orElse(semesters.getFirst());

        activeSemesterComboBox.getSelectionModel().select(semesterToSelect);
        if (student.getActiveSemesterName() == null || student.getActiveSemesterName().isBlank()) {
            student.setActiveSemesterName(semesterToSelect.getDisplayName());
        }

        if (!activeSemesterListenerAttached) {
            activeSemesterComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    student.setActiveSemesterName(newVal.getDisplayName());
                    try {
                        DataManager.saveStudent(student);
                    } catch (IOException e) {
                        showError("Cannot save active semester: " + e.getMessage());
                    }
                    updateTermWidgets(newVal);
                    refreshAcademicSummary();
                }
            });
            activeSemesterListenerAttached = true;
        }

        updateTermWidgets(activeSemesterComboBox.getValue());
    }

    private void updateTermWidgets(Semester activeSemester) {
        if (activeSemester == null) {
            return;
        }
        double termGpa = activeSemester.getGPA();
        termGpaLabel.setText(termGpa == 0.0 ? "N/A" : String.format("%.2f", termGpa));
        activeCoursesContainer.getChildren().clear();

        if (activeSemester.getCourses().isEmpty()) {
            activeCoursesContainer.getChildren().add(UiComponents.emptyState(
                    "No courses in this semester",
                    "Add courses from the Courses screen to unlock projected GPA and priority guidance."
            ));
            return;
        }

        activeSemester.getCourses().forEach(course -> activeCoursesContainer.getChildren().add(createCourseMiniCard(course)));
    }

    private HBox createCourseMiniCard(Course course) {
        HBox card = new HBox(12);
        card.getStyleClass().add("priority-card");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox textInfo = new VBox(3);
        Label codeLabel = new Label(course.getCourseCode());
        codeLabel.getStyleClass().add("section-title");
        codeLabel.setWrapText(true);
        Label nameLabel = new Label(course.getCourseName());
        nameLabel.getStyleClass().add("muted-text");
        nameLabel.setWrapText(true);
        Label gradeLabel = new Label(course.hasGradedAssessment()
                ? String.format("Current pace %.1f%%", course.getCurrentGrade())
                : "No graded assessments yet");
        gradeLabel.getStyleClass().add("section-caption");
        gradeLabel.setWrapText(true);
        textInfo.getChildren().addAll(codeLabel, nameLabel, gradeLabel);
        UiComponents.makeNodeGrow(textInfo);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label badge = UiComponents.badge(
                course.needsFinalization() ? "Needs finalization" : course.getStatus().toString(),
                course.needsFinalization() ? "warning-badge" : statusTone(course.getStatus().toString())
        );
        card.getChildren().addAll(textInfo, spacer, badge);
        return card;
    }

    @FXML
    private void calculateTargetGrades() {
        DashboardTargetService.TargetCalculationResult result = dashboardTargetService.calculate(
                student,
                targetGpaInput == null ? null : targetGpaInput.getText(),
                targetMajorGpaInput == null ? null : targetMajorGpaInput.getText()
        );

        predictionInsightContainer.getChildren().clear();
        predictionResultLabel.setText(result.output());
        predictionResultLabel.getStyleClass().removeAll("text-danger", "text-warning", "text-info");
        if (!result.valid()) {
            predictionResultLabel.getStyleClass().add("text-danger");
        } else if (result.requiresConfirmation()) {
            predictionResultLabel.getStyleClass().add("text-warning");
        } else {
            predictionResultLabel.getStyleClass().add("text-info");
        }

        if (result.cumulativeResult() != null) {
            predictionInsightContainer.getChildren().add(predictionCard(result.cumulativeResult()));
            updateTargetGap(result.cumulativeResult());
        }
        if (result.majorResult() != null) {
            predictionInsightContainer.getChildren().add(predictionCard(result.majorResult()));
            if (result.cumulativeResult() == null) {
                updateTargetGap(result.majorResult());
            }
        }
        if (result.cumulativeResult() == null && result.majorResult() == null) {
            predictionInsightContainer.getChildren().add(predictionResultLabel);
            UiComponents.setSingleTone(predictionRiskBadge, "badge", "danger-badge");
            predictionRiskBadge.setText("Needs input");
            UiComponents.setSingleTone(predictionConfidenceBadge, "badge", "neutral-badge");
            predictionConfidenceBadge.setText("Confidence unknown");
        }
    }

    private VBox predictionCard(GlobalPredictionResult result) {
        VBox card = new VBox(10);
        card.getStyleClass().add("insight-card");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(result.scope() == com.academictracker.prediction.model.PredictionScope.MAJOR
                ? "Major GPA Target"
                : "Cumulative GPA Target");
        title.getStyleClass().add("section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label risk = UiComponents.badge(result.riskLevel(), riskTone(result.riskLevel()));
        Label confidence = UiComponents.badge(result.confidenceLevel(), confidenceTone(result.confidenceLevel()));
        header.getChildren().addAll(title, spacer, risk, confidence);

        Label metrics = new Label(String.format(
                "Official %.2f | Projected %.2f | Target %s | Required avg %.2f",
                result.officialGPA(),
                result.currentProjectedGPA(),
                result.targetGPA() == null ? "N/A" : String.format("%.2f", result.targetGPA()),
                result.requiredAverageScale4()
        ));
        metrics.getStyleClass().add("body-text");
        metrics.setWrapText(true);

        Label feasibility = new Label(result.realisticFeasibility());
        feasibility.getStyleClass().add("muted-text");
        feasibility.setWrapText(true);

        card.getChildren().addAll(header, metrics, feasibility);
        if (result.impossibleReason() != null) {
            Label impossible = new Label(result.impossibleReason());
            impossible.getStyleClass().add("text-danger");
            impossible.setWrapText(true);
            card.getChildren().add(impossible);
        }
        return card;
    }

    @FXML
    private void handleNavigation(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        selectNav(clickedButton);
        String buttonId = clickedButton.getId();

        try {
            if ("btnDashboard".equals(buttonId)) {
                if (dashboardHomeView != null) {
                    mainContentPane.setCenter(dashboardHomeView);
                }
                setupActiveSemesterCombo();
                refreshAcademicSummary();
                updateAcademicStanding();
            } else if ("btnSemesters".equals(buttonId)) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/tracker/academictracker/Semesters.fxml"));
                Node view = loader.load();
                SemestersController controller = loader.getController();
                controller.initData(student, this::setupActiveSemesterCombo);
                mainContentPane.setCenter(view);
            } else if ("btnCourseRoster".equals(buttonId)) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/tracker/academictracker/CourseRoster.fxml"));
                Node view = loader.load();
                CourseRosterController controller = loader.getController();
                controller.initData(student, findActiveSemester(), mainContentPane);
                mainContentPane.setCenter(view);
            } else if ("btnSettings".equals(buttonId)) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/tracker/academictracker/Settings.fxml"));
                Node view = loader.load();
                SettingsController controller = loader.getController();
                controller.initData(student);
                mainContentPane.setCenter(view);
            }
        } catch (Exception e) {
            showError("Cannot load view: " + e.getMessage());
        }
    }

    private void selectNav(Button selected) {
        for (Button button : new Button[]{btnDashboard, btnSemesters, btnCourseRoster, btnSettings}) {
            if (button != null) {
                button.getStyleClass().remove("sidebar-button-active");
            }
        }
        if (selected != null && !selected.getStyleClass().contains("sidebar-button-active")) {
            selected.getStyleClass().add("sidebar-button-active");
        }
    }

    private void refreshAcademicSummary() {
        GlobalPredictionResult result = predictionService.predictCumulative(student, null);
        overallGpaLabel.setText(result.officialGPA() == 0.0 ? "N/A" : String.format("%.2f", result.officialGPA()));
        projectedGpaLabel.setText(result.currentProjectedGPA() == 0.0 ? "N/A" : String.format("%.2f", result.currentProjectedGPA()));
        creditsProgressLabel.setText(result.progress() == null
                ? String.valueOf(student.getTotalCreditsCompleted())
                : result.progress().completedCredits() + " / " + result.progress().totalCreditsForProgress());
        targetGapLabel.setText("Expected scenario GPA: " + String.format("%.2f", result.expectedScenarioGPA()));

        if (predictionResultLabel != null) {
            predictionResultLabel.setText("Official GPA uses completed courses only. Projected GPA is a scenario estimate based on current entered data.");
        }
        if (predictionInsightContainer != null && predictionResultLabel != null) {
            predictionInsightContainer.getChildren().setAll(predictionResultLabel);
        }
        UiComponents.setSingleTone(predictionRiskBadge, "badge", "neutral-badge");
        predictionRiskBadge.setText("No target");
        UiComponents.setSingleTone(predictionConfidenceBadge, "badge", confidenceTone(result.confidenceLevel()));
        predictionConfidenceBadge.setText("Confidence " + result.confidenceLevel().toLowerCase());
        renderPriorityCourses(result);
    }

    private void renderPriorityCourses(GlobalPredictionResult result) {
        priorityCoursesContainer.getChildren().clear();
        var predictions = result.coursePredictions().stream()
                .sorted(Comparator.comparingInt(this::priorityRank))
                .limit(4)
                .toList();
        if (predictions.isEmpty()) {
            priorityCoursesContainer.getChildren().add(UiComponents.emptyState(
                    "No priority courses yet",
                    "Add active or planned courses to see GPA-impact priorities."
            ));
            return;
        }
        predictions.forEach(prediction -> priorityCoursesContainer.getChildren().add(priorityCard(prediction)));
    }

    private HBox priorityCard(CoursePredictionResult prediction) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("priority-card");
        VBox info = new VBox(3);
        Label title = new Label(prediction.courseName());
        title.getStyleClass().add("section-title");
        title.setWrapText(true);
        Label subtitle = new Label(String.format("%d credits | target %.1f | projected %.1f",
                prediction.credits(),
                prediction.recommendedTargetScore(),
                prediction.projectedFinalScore()));
        subtitle.getStyleClass().add("muted-text");
        subtitle.setWrapText(true);
        info.getChildren().addAll(title, subtitle);
        UiComponents.makeNodeGrow(info);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label badge = UiComponents.badge(prediction.priorityLevel().name(), priorityTone(prediction.priorityLevel().name()));
        card.getChildren().addAll(info, spacer, badge);
        return card;
    }

    private void updateTargetGap(GlobalPredictionResult result) {
        if (result.targetGPA() == null) {
            return;
        }
        double gap = result.targetGPA() - result.officialGPA();
        targetGapLabel.setText(String.format("Target gap: %+,.2f GPA points", gap));
        predictionRiskBadge.setText(result.riskLevel());
        UiComponents.setSingleTone(predictionRiskBadge, "badge", riskTone(result.riskLevel()));
        predictionConfidenceBadge.setText("Confidence " + result.confidenceLevel().toLowerCase());
        UiComponents.setSingleTone(predictionConfidenceBadge, "badge", confidenceTone(result.confidenceLevel()));
    }

    private Semester findActiveSemester() {
        return student.getSemesters().stream()
                .filter(s -> s.getDisplayName().equals(student.getActiveSemesterName()))
                .findFirst()
                .orElse(null);
    }

    private int priorityRank(CoursePredictionResult result) {
        return switch (result.priorityLevel()) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    private String riskTone(String risk) {
        if (risk == null) {
            return "neutral-badge";
        }
        return switch (risk.toUpperCase()) {
            case "LOW", "SAFE" -> "low-risk-badge";
            case "MEDIUM", "WATCH" -> "medium-risk-badge";
            case "HIGH", "RISKY" -> "high-risk-badge";
            case "EXTREME", "CRITICAL" -> "danger-badge";
            default -> "neutral-badge";
        };
    }

    private String priorityTone(String priority) {
        return switch (priority == null ? "" : priority.toUpperCase()) {
            case "CRITICAL" -> "danger-badge";
            case "HIGH" -> "high-risk-badge";
            case "MEDIUM" -> "medium-risk-badge";
            default -> "neutral-badge";
        };
    }

    private String statusTone(String status) {
        String normalized = status == null ? "" : status.toUpperCase();
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

    private String confidenceTone(String confidence) {
        if (confidence == null) {
            return "neutral-badge";
        }
        return switch (confidence.toUpperCase()) {
            case "HIGH" -> "confidence-high";
            case "MEDIUM" -> "confidence-medium";
            case "LOW" -> "confidence-low";
            default -> "neutral-badge";
        };
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
