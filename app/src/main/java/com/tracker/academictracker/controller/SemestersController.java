package com.tracker.academictracker.controller;

import com.tracker.academictracker.model.Semester;
import com.tracker.academictracker.model.Student;
import com.tracker.academictracker.model.CourseStatus;
import com.tracker.academictracker.service.DataManager;
import com.tracker.academictracker.service.InputValidator;
import com.tracker.academictracker.ui.UiComponents;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class SemestersController {

    // --- FXML Elements ---
    @FXML private VBox semesterListContainer; // Container chứa danh sách thẻ học kỳ
    @FXML private VBox addSemesterForm;      // VBox chứa form nhập liệu (ẩn/hiện)
    @FXML private TextField yearInput;        // Ô nhập năm
    @FXML private ComboBox<String> termInput; // Menu chọn mùa (Season)
    private Runnable onActiveSemesterChanged;
    private Student student;

    public void initData(Student student, Runnable onActiveSemesterChanged) {
        this.student = student;
        this.onActiveSemesterChanged = onActiveSemesterChanged; // Store the callback

        if (termInput != null) {
            termInput.setItems(FXCollections.observableArrayList("Sem 1", "Sem 2", "Summer"));
            termInput.getSelectionModel().selectFirst();
        }

        loadCards();
    }

    @FXML
    private void handleAddSemester() {
        addSemesterForm.setVisible(true);
        addSemesterForm.setManaged(true);
    }

    @FXML
    private void hideAddForm() {
        addSemesterForm.setVisible(false);
        addSemesterForm.setManaged(false);
        yearInput.clear();
    }

    @FXML
    private void saveNewSemester() {
        try {
            String yearText = yearInput.getText().trim();
            if (yearText.isEmpty()) return;

            int year = InputValidator.parsePositiveInt(yearText, "Year");
            String term = termInput.getValue();
            if (year < 2000 || year > 2100) {
                showError("Year must be between 2000 and 2100.");
                return;
            }
            boolean duplicateSemester = student.getSemesters().stream()
                    .anyMatch(semester -> semester.getYear() == year
                            && semester.getTermType() != null
                            && semester.getTermType().equalsIgnoreCase(term));
            if (duplicateSemester) {
                showError("This semester already exists.");
                return;
            }

            Semester newSemester = new Semester(year, term);
            student.addSemester(newSemester);
            DataManager.saveStudent(student);
            HBox newCard = createCard(newSemester);
            semesterListContainer.getChildren().add(0, newCard);
            hideAddForm();
            System.out.println("Semester added and saved successfully.");

        } catch (IllegalArgumentException | IOException e) {
            showError("Error saving semester: " + e.getMessage());
        }
    }
    private void loadCards() {
        if (semesterListContainer == null) return;
        semesterListContainer.getChildren().clear();

        if (student != null && student.getSemesters() != null && !student.getSemesters().isEmpty()) {
            for (int i = student.getSemesters().size() - 1; i >= 0; i--) {
                HBox card = createCard(student.getSemesters().get(i));
                semesterListContainer.getChildren().add(card);
            }
        } else {
            semesterListContainer.getChildren().add(UiComponents.emptyState(
                    "No semesters yet",
                    "Create a semester to organize courses and activate dashboard tracking."
            ));
        }
    }

    private HBox createCard(Semester semester) {
        HBox card = new HBox(18);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinHeight(Region.USE_PREF_SIZE);

        VBox leftInfo = new VBox(5);
        leftInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(leftInfo, Priority.ALWAYS);

        Label nameLabel = new Label(semester.getDisplayName());
        nameLabel.getStyleClass().add("section-title");
        nameLabel.setWrapText(true);

        String subText = semester.getTermType() + " " + semester.getYear();
        Label termLabel = new Label(subText);
        termLabel.getStyleClass().add("muted-text");
        termLabel.setWrapText(true);

        var courses = semester.getCourses();
        int courseCount = courses.size();
        int credits = courses.stream().mapToInt(course -> course.getCredits()).sum();
        long completed = courses.stream().filter(course -> course.isOfficiallyComplete()).count();
        long active = courses.stream().filter(course -> course.getStatus() == CourseStatus.ACTIVE).count();
        Label coursesLabel = new Label(courseCount + " courses | " + credits + " credits | "
                + completed + " completed | " + active + " active");
        coursesLabel.getStyleClass().add("section-caption");
        coursesLabel.setWrapText(true);

        leftInfo.getChildren().addAll(nameLabel, termLabel, coursesLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox rightInfo = new VBox(6);
        rightInfo.setAlignment(Pos.CENTER_RIGHT);

        Label gpaTitle = new Label("Term GPA");
        gpaTitle.getStyleClass().add("metric-label");

        double gpa = semester.getGPA();
        Label gpaValue = new Label(gpa == 0.0 ? "N/A" : String.format("%.2f", gpa));
        gpaValue.getStyleClass().add("section-title");

        Button setBtn = new Button();
        if (semester.getDisplayName().equals(student.getActiveSemesterName())) {
            setBtn.setText("Active");
            setBtn.getStyleClass().add("success-button");
            setBtn.setDisable(true);
        } else {
            setBtn.setText("Set as Active");
            setBtn.getStyleClass().add("secondary-button");
            setBtn.setOnAction(e -> setActiveSemester(semester));
        }

        rightInfo.getChildren().addAll(gpaTitle, gpaValue, setBtn);
        card.getChildren().addAll(leftInfo, spacer, rightInfo);
        return card;
    }
    private void setActiveSemester(Semester sem) {
        String previousActiveSemester = student.getActiveSemesterName();
        student.setActiveSemesterName(sem.getDisplayName());

        try {
            DataManager.saveStudent(student);
        } catch (IOException e) {
            student.setActiveSemesterName(previousActiveSemester);
            showError("Failed to save active semester: " + e.getMessage());
            loadCards();
            return;
        }

        // Rebuild the card list to instantly show which one is active.
        loadCards();
        
        // Trigger the callback to tell Dashboard to refresh the header.
        if (onActiveSemesterChanged != null) {
            onActiveSemesterChanged.run();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
