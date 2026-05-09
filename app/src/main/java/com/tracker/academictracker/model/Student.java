package com.tracker.academictracker.model; // 1. Sửa lại tên package cho đồng bộ

import java.util.ArrayList;
import java.util.List;

public class Student {
    private String fullName;
    private String studentId;
    private List<Semester> semesters;
    private String activeSemesterName;
    private Integer degreeTotalCredits;
    private Integer majorRequiredCredits;
    private String appTheme;

    // Required for Gson
    public Student() {
        this.semesters = new ArrayList<>();
    }

    public Student(String fullName, String studentId) {
        this.fullName = fullName;
        this.studentId = studentId;
        this.semesters = new ArrayList<>();
    }

    //Getters and Setters
    public String getActiveSemesterName() {
        return activeSemesterName;
    }

    public void setActiveSemesterName(String activeSemesterName) {
        this.activeSemesterName = activeSemesterName;
    }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public Integer getDegreeTotalCredits() { return degreeTotalCredits; }
    public void setDegreeTotalCredits(Integer degreeTotalCredits) {
        if (degreeTotalCredits != null && degreeTotalCredits <= 0) {
            throw new IllegalArgumentException("Degree total credits must be greater than 0.");
        }
        this.degreeTotalCredits = degreeTotalCredits;
    }

    public Integer getMajorRequiredCredits() { return majorRequiredCredits; }
    public void setMajorRequiredCredits(Integer majorRequiredCredits) {
        if (majorRequiredCredits != null && majorRequiredCredits <= 0) {
            throw new IllegalArgumentException("Major required credits must be greater than 0.");
        }
        this.majorRequiredCredits = majorRequiredCredits;
    }

    public String getAppTheme() {
        return appTheme == null || appTheme.isBlank() ? "LIGHT" : appTheme;
    }

    public void setAppTheme(String appTheme) {
        this.appTheme = appTheme == null || appTheme.isBlank() ? "LIGHT" : appTheme.trim().toUpperCase();
    }

    public List<Semester> getSemesters() {
        if (semesters == null) {
            semesters = new ArrayList<>();
        }
        return semesters;
    }

    public void setSemesters(List<Semester> semesters) {
        this.semesters = semesters == null ? new ArrayList<>() : new ArrayList<>(semesters);
    }

    public void addSemester(Semester semester) {
        if (semester != null) {
            getSemesters().add(semester);
        }
    }

    public void removeSemester(Semester semester) {
        getSemesters().remove(semester);
    }

    public double getCumulativeGPA() {
        double totalPoints = 0;
        int totalCredits = 0;

        for (Semester s : getSemesters()) {
            for (Course c : s.getCourses()) { // Or `courses` in Semester.java
                if (!c.isOfficiallyComplete()) continue;

                double gpaPoints = convertToGPAPoints(c.getFinalScorePercent());
                totalPoints += gpaPoints * c.getCredits();
                totalCredits += c.getCredits();
            }
        }
        return totalCredits == 0 ? 0.0 : totalPoints / totalCredits;
    }

    public double getCompletedGPA() {
        double totalPoints = 0;
        int totalCredits = 0;

        for (Semester s : getSemesters()) {
            for (Course c : s.getCourses()) {
                if (!c.isOfficiallyComplete()) continue;

                double gpaPoints = convertToGPAPoints(c.getFinalScorePercent());
                totalPoints += gpaPoints * c.getCredits();
                totalCredits += c.getCredits();
            }
        }
        return totalCredits == 0 ? 0.0 : totalPoints / totalCredits;
    }

    public int getTotalCreditsCompleted() {
        int total = 0;
        for (Semester s : getSemesters()) {
            for (Course c : s.getCourses()) {
                if (c.isOfficiallyComplete()) {
                    total += c.getCredits();
                }
            }
        }
        return total;
    }

    public String getAcademicStanding() {
        if (getSemesters().isEmpty() || getTotalCreditsCompleted() == 0) return "New Student";

        double gpa = getCumulativeGPA();
        if (gpa >= 3.0) return "Good Standing";
        if (gpa >= 2.0) return "Satisfactory";
        return "Academic Probation";
    }

    private double convertToGPAPoints(double grade) {
        if (grade >= 90) return 4.0; // Xuất sắc (A+)
        if (grade >= 80) return 3.5; // Giỏi (A)
        if (grade >= 70) return 3.0; // Khá (B+)
        if (grade >= 60) return 2.5; // Trung bình Khá (B)
        if (grade >= 50) return 2.0; // Trung bình (C)
        if (grade >= 40) return 1.5; // Yếu (D+)
        if (grade >= 30) return 1.0; // Kém (D)
        return 0.0;                  // Rất kém (F)
    }

    public void normalizeAfterLoad() {
        if (fullName == null) {
            fullName = "";
        }
        if (studentId == null) {
            studentId = "";
        }
        if (appTheme == null || appTheme.isBlank()) {
            appTheme = "LIGHT";
        }
        if (degreeTotalCredits != null && degreeTotalCredits <= 0) {
            throw new IllegalArgumentException("Degree total credits must be greater than 0.");
        }
        if (majorRequiredCredits != null && majorRequiredCredits <= 0) {
            throw new IllegalArgumentException("Major required credits must be greater than 0.");
        }
        getSemesters().forEach(Semester::normalizeAfterLoad);
    }
}
