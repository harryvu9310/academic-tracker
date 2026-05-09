package com.academictracker.prediction.model;

import java.util.ArrayList;
import java.util.List;

public class PredictionContext {
    private List<Course> courses;
    private ProgramConfig programConfig;
    private List<StudentPerformanceHistory> studentPerformanceHistory;
    private Double targetCumulativeGPA;
    private Double targetMajorGPA;

    public PredictionContext() {
        this.courses = new ArrayList<>();
        this.programConfig = new ProgramConfig();
        this.studentPerformanceHistory = new ArrayList<>();
    }

    public List<Course> getCourses() {
        if (courses == null) {
            courses = new ArrayList<>();
        }
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses == null ? new ArrayList<>() : new ArrayList<>(courses);
    }

    public ProgramConfig getProgramConfig() {
        if (programConfig == null) {
            programConfig = new ProgramConfig();
        }
        return programConfig;
    }

    public void setProgramConfig(ProgramConfig programConfig) {
        this.programConfig = programConfig == null ? new ProgramConfig() : programConfig;
    }

    public List<StudentPerformanceHistory> getStudentPerformanceHistory() {
        if (studentPerformanceHistory == null) {
            studentPerformanceHistory = new ArrayList<>();
        }
        return studentPerformanceHistory;
    }

    public void setStudentPerformanceHistory(List<StudentPerformanceHistory> studentPerformanceHistory) {
        this.studentPerformanceHistory = studentPerformanceHistory == null
                ? new ArrayList<>()
                : new ArrayList<>(studentPerformanceHistory);
    }

    public Double getTargetCumulativeGPA() {
        return targetCumulativeGPA;
    }

    public void setTargetCumulativeGPA(Double targetCumulativeGPA) {
        this.targetCumulativeGPA = targetCumulativeGPA;
    }

    public Double getTargetMajorGPA() {
        return targetMajorGPA;
    }

    public void setTargetMajorGPA(Double targetMajorGPA) {
        this.targetMajorGPA = targetMajorGPA;
    }
}
