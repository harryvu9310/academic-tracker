package com.academictracker.prediction.model;

import java.util.ArrayList;
import java.util.List;

public class StudentPerformanceHistory {
    private String category;
    private String subjectGroup;
    private List<Double> pastScores;
    private Double volatility;
    private List<CoursePerformanceRecord> courseRecords;

    public StudentPerformanceHistory() {
        this.pastScores = new ArrayList<>();
        this.courseRecords = new ArrayList<>();
    }

    public StudentPerformanceHistory(String category, List<Double> pastScores, Double volatility) {
        this.category = category;
        this.pastScores = pastScores == null ? new ArrayList<>() : new ArrayList<>(pastScores);
        this.volatility = volatility;
        this.courseRecords = new ArrayList<>();
    }

    public StudentPerformanceHistory(
            String category,
            String subjectGroup,
            List<Double> pastScores,
            Double volatility,
            List<CoursePerformanceRecord> courseRecords
    ) {
        this.category = category;
        this.subjectGroup = subjectGroup;
        this.pastScores = pastScores == null ? new ArrayList<>() : new ArrayList<>(pastScores);
        this.volatility = volatility;
        this.courseRecords = courseRecords == null ? new ArrayList<>() : new ArrayList<>(courseRecords);
    }

    public String getCategory() {
        return category == null || category.isBlank() ? "General" : category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubjectGroup() {
        return subjectGroup == null || subjectGroup.isBlank() ? getCategory() : subjectGroup;
    }

    public void setSubjectGroup(String subjectGroup) {
        this.subjectGroup = subjectGroup;
    }

    public List<Double> getPastScores() {
        if (pastScores == null) {
            pastScores = new ArrayList<>();
        }
        return pastScores;
    }

    public void setPastScores(List<Double> pastScores) {
        this.pastScores = pastScores == null ? new ArrayList<>() : new ArrayList<>(pastScores);
    }

    public Double getVolatility() {
        return volatility;
    }

    public void setVolatility(Double volatility) {
        this.volatility = volatility;
    }

    public List<CoursePerformanceRecord> getCourseRecords() {
        if (courseRecords == null) {
            courseRecords = new ArrayList<>();
        }
        return courseRecords;
    }

    public void setCourseRecords(List<CoursePerformanceRecord> courseRecords) {
        this.courseRecords = courseRecords == null ? new ArrayList<>() : new ArrayList<>(courseRecords);
    }

    public void addCourseRecord(CoursePerformanceRecord record) {
        if (record != null) {
            getCourseRecords().add(record);
        }
    }

    public record CoursePerformanceRecord(
            String courseCode,
            String courseName,
            int credits,
            double finalPercentageScore,
            double gpaPoints,
            String letterGrade,
            String category,
            String subjectGroup,
            Boolean majorCourse,
            int year,
            String term,
            int chronologicalOrder
    ) {
    }
}
