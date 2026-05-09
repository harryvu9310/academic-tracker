package com.tracker.academictracker.model; // 1. Đã sửa lại package

import java.util.ArrayList;
import java.util.List;

public class Course {
    private String courseCode;
    private String courseName;
    private String category;
    private Boolean majorCourse;
    private Double targetScore;
    private int credits;

    // 2. Đổi tên từ courseStatus thành status
    private CourseStatus status;

    private List<Assessment> assessments;

    public Course() {
        // 3. Khởi tạo ArrayList để tránh lỗi NullPointerException khi Gson đọc file hoặc khi tạo mới từ Form
        this.assessments = new ArrayList<>();
    }

    public Course(String courseCode, String courseName, int credits) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        setCredits(credits);
        this.status = CourseStatus.ACTIVE; // Đã cập nhật tên biến
        this.assessments = new ArrayList<>();
    }

    // --- Getters and Setters ---
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCategory() {
        return category == null || category.isBlank() ? "General" : category;
    }
    public void setCategory(String category) { this.category = category; }

    public Boolean getMajorCourse() { return majorCourse; }
    public void setMajorCourse(Boolean majorCourse) { this.majorCourse = majorCourse; }

    public Double getTargetScore() { return targetScore; }
    public void setTargetScore(Double targetScore) {
        if (targetScore != null && (!Double.isFinite(targetScore) || targetScore < 0.0 || targetScore > 100.0)) {
            throw new IllegalArgumentException("Target score must be a finite number between 0 and 100.");
        }
        this.targetScore = targetScore;
    }

    public int getCredits() { return credits; }
    public void setCredits(int credits) {
        if (credits <= 0) {
            throw new IllegalArgumentException("Credits must be greater than 0.");
        }
        this.credits = credits;
    }

    // Đã đổi thành getStatus() và setStatus() để JavaFX TableView có thể nhận diện được
    public CourseStatus getStatus() { return status == null ? CourseStatus.ACTIVE : status; }
    public void setStatus(CourseStatus status) { this.status = status; }

    public List<Assessment> getAssessments() {
        if (assessments == null) {
            assessments = new ArrayList<>();
        }
        return assessments;
    }

    public void setAssessments(List<Assessment> assessments) {
        this.assessments = assessments == null ? new ArrayList<>() : new ArrayList<>(assessments);
    }

    // --- Methods ---
    public void addAssessment(Assessment assessment) {
        if (assessment != null) {
            getAssessments().add(assessment);
        }
    }

    public void removeAssessment(Assessment assessment) {
        getAssessments().remove(assessment);
    }

    public double getTotalWeight() {
        double totalWeight = 0;
        for (Assessment a : getAssessments()) {
            if (a != null && Double.isFinite(a.getWeight())) {
                totalWeight += a.getWeight();
            }
        }
        return totalWeight;
    }

    public boolean isWeightTotalValid() {
        return Math.abs(getTotalWeight() - 100.0) < 0.001;
    }

    public double getCurrentGrade() {
        double earnedPoints = 0;
        double gradedWeightTotal = 0;

        for (Assessment a : getAssessments()) {
            if (a.isGraded()) {
                earnedPoints += a.getWeightScore();
                gradedWeightTotal += a.getWeight();
            }
        }
        if (gradedWeightTotal == 0) {
            return 0.0;
        }

        return (earnedPoints / gradedWeightTotal) * 100;
    }

    public double getFinalScorePercent() {
        double earnedPoints = 0;
        for (Assessment a : getAssessments()) {
            if (a.isGraded()) {
                earnedPoints += a.getWeightScore();
            }
        }
        return Math.max(0.0, Math.min(100.0, earnedPoints));
    }

    public boolean hasGradedAssessment() {
        return getAssessments().stream().anyMatch(Assessment::isGraded);
    }

    public boolean isOfficiallyComplete() {
        return getStatus() == CourseStatus.COMPLETED
                && getCredits() > 0
                && isWeightTotalValid()
                && !getAssessments().isEmpty()
                && getAssessments().stream().allMatch(this::isValidGradedAssessment);
    }

    public boolean needsFinalization() {
        return getStatus() == CourseStatus.COMPLETED && !isOfficiallyComplete();
    }

    private boolean isValidGradedAssessment(Assessment assessment) {
        return assessment != null
                && assessment.isGraded()
                && Double.isFinite(assessment.getWeight())
                && assessment.getWeight() >= 0.0
                && assessment.getWeight() <= 100.0
                && Double.isFinite(assessment.getMaxScore())
                && assessment.getMaxScore() > 0.0
                && assessment.getScore() != null
                && Double.isFinite(assessment.getScore())
                && assessment.getScore() >= 0.0
                && assessment.getScore() <= assessment.getMaxScore();
    }

    public void normalizeAfterLoad() {
        if (courseCode == null) {
            courseCode = "";
        }
        if (courseName == null) {
            courseName = "";
        }
        if (category == null || category.isBlank()) {
            category = "General";
        }
        if (credits <= 0) {
            throw new IllegalArgumentException("Course credits must be greater than 0.");
        }
        if (targetScore != null && (!Double.isFinite(targetScore) || targetScore < 0.0 || targetScore > 100.0)) {
            throw new IllegalArgumentException("Course target score must be a finite number between 0 and 100.");
        }
        if (status == null) {
            status = CourseStatus.ACTIVE;
        }
        getAssessments().forEach(Assessment::normalizeAfterLoad);
    }
}
