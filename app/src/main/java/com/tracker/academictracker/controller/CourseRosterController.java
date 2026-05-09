package com.tracker.academictracker.controller;

import com.tracker.academictracker.model.Course;
import com.tracker.academictracker.model.CourseStatus;
import com.tracker.academictracker.model.Semester;
import com.tracker.academictracker.model.Student;
import com.tracker.academictracker.service.DataManager;
import com.tracker.academictracker.service.InputValidator;
import com.tracker.academictracker.ui.UiComponents;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.Optional;

public class CourseRosterController {

    @FXML private VBox addCourseForm;
    @FXML private TextField courseCodeInput;
    @FXML private TextField courseNameInput;
    @FXML private TextField categoryInput;
    @FXML private TextField creditsInput;
    @FXML private ComboBox<CourseStatus> statusInput; // Thay đổi thành CourseStatus
    @FXML private CheckBox majorCourseInput;
    @FXML private TextField searchInput;
    @FXML private ComboBox<String> statusFilterInput;
    @FXML private ComboBox<String> majorFilterInput;

    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, String> colCode;
    @FXML private TableColumn<Course, String> colName;
    @FXML private TableColumn<Course, String> colCategory;
    @FXML private TableColumn<Course, Integer> colCredits;
    @FXML private TableColumn<Course, String> colMajor;
    @FXML private TableColumn<Course, CourseStatus> colStatus;
    @FXML private TableColumn<Course, String> colGrade; // Cột mới
    @FXML private TableColumn<Course, Void> colAction;

    private Student student;
    private Semester activeSemester; // Học kỳ đang được chọn từ Dashboard truyền sang
    private BorderPane mainContentPane;
    /**
     * Nhận dữ liệu Student và Học kỳ đang Active
     */
    public void initData(Student student, Semester activeSemester, BorderPane mainContentPane) {
        this.student = student;
        this.activeSemester = activeSemester;
        this.mainContentPane = mainContentPane;

        // Khởi tạo ComboBox Status
        if (statusInput != null) {
            statusInput.setItems(FXCollections.observableArrayList(CourseStatus.values()));
            statusInput.getSelectionModel().selectFirst();
        }
        setupFilters();

        setupTable();
        courseTable.setRowFactory(tv -> {
            TableRow<Course> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                // Nhấn đúp chuột (Click Count == 2) vào hàng không rỗng
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Course clickedCourse = row.getItem();
                    openCourseDetails(clickedCourse);
                }
            });
            return row;
        });
        updateTable();
    }

    private void setupFilters() {
        if (statusFilterInput != null) {
            statusFilterInput.setItems(FXCollections.observableArrayList(
                    "All statuses", CourseStatus.ACTIVE.toString(), CourseStatus.COMPLETED.toString(),
                    CourseStatus.PLANNED.toString(), CourseStatus.DROPPED.toString()
            ));
            statusFilterInput.getSelectionModel().selectFirst();
            statusFilterInput.valueProperty().addListener((obs, oldValue, newValue) -> updateTable());
        }
        if (majorFilterInput != null) {
            majorFilterInput.setItems(FXCollections.observableArrayList("All courses", "Major only", "General only"));
            majorFilterInput.getSelectionModel().selectFirst();
            majorFilterInput.valueProperty().addListener((obs, oldValue, newValue) -> updateTable());
        }
        if (searchInput != null) {
            searchInput.textProperty().addListener((obs, oldValue, newValue) -> updateTable());
        }
    }
    private void openCourseDetails(Course course) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/tracker/academictracker/CourseDetails.fxml"));
            javafx.scene.Node view = loader.load();

            CourseDetailsController controller = loader.getController();
            controller.initData(student, activeSemester, course, mainContentPane);

            mainContentPane.setCenter(view);
        } catch (IOException e) {
            showError("Cannot open Course Details view: " + e.getMessage());
        }
    }

    private void setupTable() {
        UiComponents.configureResponsiveTable(courseTable, 520);

        // Ánh xạ dữ liệu cơ bản
        colCode.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        colName.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        colCategory.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCategory()));
        colCredits.setCellValueFactory(new PropertyValueFactory<>("credits"));
        colMajor.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                Boolean.TRUE.equals(data.getValue().getMajorCourse()) ? "Major" : "General"
        ));
        colMajor.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                } else {
                    setGraphic(UiComponents.badge(value, "Major".equals(value) ? "major-badge" : "neutral-badge"));
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(CourseStatus status, boolean empty) {
                super.updateItem(status, empty);
                Course course = getTableRow() == null ? null : getTableRow().getItem();
                if (empty || status == null || course == null) {
                    setGraphic(null);
                } else {
                    String label = course.needsFinalization() ? "Needs finalization" : status.toString();
                    String tone = course.needsFinalization() ? "warning-badge" : statusTone(status);
                    setGraphic(UiComponents.badge(label, tone));
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // 2. Tùy chỉnh cột CURRENT GRADE 

        // 2. Tùy chỉnh cột CURRENT GRADE
        colGrade.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(""));

        colGrade.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String grade, boolean empty) {
                super.updateItem(grade, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Course course = (Course) getTableRow().getItem();
                    double currentGrade = course.getCurrentGrade();

                    boolean hasGraded = course.hasGradedAssessment();

                    if (currentGrade == 0.0 && !hasGraded) {
                        setText("N/A");
                    } else {
                        setText(String.format("%.1f%%", currentGrade));
                    }
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // 3. Tùy chỉnh cột ACTION (Nút Xóa hình thùng rác)
        colAction.setCellFactory(column -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");

            {
                deleteBtn.getStyleClass().add("danger-button");
                deleteBtn.setOnAction(event -> {
                    Course course = getTableRow() == null ? null : getTableRow().getItem();
                    if (course == null) return;
                    handleDeleteCourse(course);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void updateTable() {
        if (activeSemester != null && activeSemester.getCourses() != null) {
            courseTable.setItems(FXCollections.observableArrayList(activeSemester.getCourses().stream()
                    .filter(this::matchesFilters)
                    .toList()));
        } else {
            courseTable.getItems().clear();
        }
    }

    private boolean matchesFilters(Course course) {
        if (course == null) {
            return false;
        }
        String query = searchInput == null || searchInput.getText() == null
                ? ""
                : searchInput.getText().trim().toLowerCase();
        if (!query.isEmpty()) {
            String haystack = String.join(" ",
                    course.getCourseCode() == null ? "" : course.getCourseCode(),
                    course.getCourseName() == null ? "" : course.getCourseName(),
                    course.getCategory() == null ? "" : course.getCategory()
            ).toLowerCase();
            if (!haystack.contains(query)) {
                return false;
            }
        }
        String statusFilter = statusFilterInput == null ? "All statuses" : statusFilterInput.getValue();
        if (statusFilter != null && !"All statuses".equals(statusFilter)
                && !course.getStatus().toString().equals(statusFilter)) {
            return false;
        }
        String majorFilter = majorFilterInput == null ? "All courses" : majorFilterInput.getValue();
        if ("Major only".equals(majorFilter) && !Boolean.TRUE.equals(course.getMajorCourse())) {
            return false;
        }
        return !"General only".equals(majorFilter) || !Boolean.TRUE.equals(course.getMajorCourse());
    }

    // --- FORM ACTIONS ---

    @FXML
    private void handleAddCourseClick() {
        addCourseForm.setVisible(true);
        addCourseForm.setManaged(true);
    }

    @FXML
    private void hideAddForm() {
        addCourseForm.setVisible(false);
        addCourseForm.setManaged(false);
        courseCodeInput.clear();
        courseNameInput.clear();
        categoryInput.clear();
        creditsInput.clear();
        majorCourseInput.setSelected(false);
    }

    @FXML
    private void saveNewCourse() {
        if (activeSemester == null) {
            showError("Please create or select an active semester before adding courses.");
            return;
        }

        try {
            String code = courseCodeInput.getText().trim();
            String name = courseNameInput.getText().trim();
            String category = categoryInput.getText().trim();
            int credits = InputValidator.parsePositiveInt(creditsInput.getText(), "Credits");
            CourseStatus status = statusInput.getValue(); // Lấy enum status[cite: 3]

            if (code.isEmpty() || name.isEmpty()) {
                showError("Course code and course name are required.");
                return;
            }
            boolean duplicateCode = activeSemester.getCourses().stream()
                    .anyMatch(course -> course.getCourseCode() != null
                            && course.getCourseCode().equalsIgnoreCase(code));
            if (duplicateCode) {
                showError("This course code already exists in the active semester.");
                return;
            }

            // Điều chỉnh Constructor theo thiết kế Course.java của bạn
            Course newCourse = new Course();
            newCourse.setCourseCode(code);
            newCourse.setCourseName(name);
            newCourse.setCategory(category.isBlank() ? "General" : category);
            newCourse.setMajorCourse(majorCourseInput.isSelected());
            newCourse.setCredits(credits);
            newCourse.setStatus(status == null ? CourseStatus.ACTIVE : status);

            activeSemester.addCourse(newCourse);
            try {
                DataManager.saveStudent(student);
            } catch (IOException e) {
                activeSemester.getCourses().remove(newCourse);
                throw e;
            }

            updateTable();
            hideAddForm();
            System.out.println("Course added.");

        } catch (IllegalArgumentException | IOException e) {
            showError("Invalid course data: " + e.getMessage());
        }
    }

    private void handleDeleteCourse(Course course) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Course");
        alert.setHeaderText("Are you sure you want to delete " + course.getCourseCode() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int originalIndex = activeSemester.getCourses().indexOf(course);
            activeSemester.getCourses().remove(course);
            try {
                DataManager.saveStudent(student);
                updateTable();
            } catch (IOException e) {
                if (originalIndex >= 0 && originalIndex <= activeSemester.getCourses().size()) {
                    activeSemester.getCourses().add(originalIndex, course);
                } else {
                    activeSemester.getCourses().add(course);
                }
                updateTable();
                showError("Failed to save after deleting course: " + e.getMessage());
            }
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private String statusTone(CourseStatus status) {
        if (status == CourseStatus.COMPLETED) {
            return "completed-badge";
        }
        if (status == CourseStatus.ACTIVE) {
            return "in-progress-badge";
        }
        if (status == CourseStatus.PLANNED) {
            return "planned-badge";
        }
        return "neutral-badge";
    }
}
