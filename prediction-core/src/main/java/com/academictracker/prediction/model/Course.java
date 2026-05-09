package com.academictracker.prediction.model;

import java.util.ArrayList;
import java.util.List;

public class Course {
    private String id;
    private String courseName;
    private String category;
    private int credits;
    private Boolean majorCourse;
    private CourseStatus status;
    private Double targetScore;
    private List<Assessment> assessments;

    public Course() {
        this.assessments = new ArrayList<>();
        this.status = CourseStatus.IN_PROGRESS;
    }

    public Course(String id, String courseName, String category, int credits, Boolean majorCourse, CourseStatus status) {
        this();
        this.id = id;
        this.courseName = courseName;
        this.category = category;
        setCredits(credits);
        this.majorCourse = majorCourse;
        this.status = status;
    }

    public String getId() {
        return id == null ? "" : id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCourseName() {
        return courseName == null ? "" : courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCategory() {
        return category == null || category.isBlank() ? "General" : category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        if (credits <= 0) {
            throw new IllegalArgumentException("Course credits must be greater than 0.");
        }
        this.credits = credits;
    }

    public Boolean getMajorCourse() {
        return majorCourse;
    }

    public void setMajorCourse(Boolean majorCourse) {
        this.majorCourse = majorCourse;
    }

    public CourseStatus getStatus() {
        return status == null ? CourseStatus.IN_PROGRESS : status;
    }

    public void setStatus(CourseStatus status) {
        this.status = status;
    }

    public Double getTargetScore() {
        return targetScore;
    }

    public void setTargetScore(Double targetScore) {
        if (targetScore != null && (!Double.isFinite(targetScore) || targetScore < 0.0 || targetScore > 100.0)) {
            throw new IllegalArgumentException("Course target score must be a finite number between 0 and 100.");
        }
        this.targetScore = targetScore;
    }

    public List<Assessment> getAssessments() {
        if (assessments == null) {
            assessments = new ArrayList<>();
        }
        return assessments;
    }

    public void setAssessments(List<Assessment> assessments) {
        this.assessments = assessments == null ? new ArrayList<>() : new ArrayList<>(assessments);
    }

    public void addAssessment(Assessment assessment) {
        getAssessments().add(assessment);
    }
}
