package com.tracker.academictracker.model; // 1. Đã sửa lại package cho đồng bộ

import java.util.ArrayList;
import java.util.List;

public class Semester {
    private int year;
    private String termType;
    private List<Course> courses;

    public Semester(){
        this.courses = new ArrayList<>();
    }

    // Constructor
    public Semester(int year , String termType){
        this.year = year;
        this.termType = termType;
        this.courses = new ArrayList<>();
    }

    // Getter & Setter
    public int getYear() {return year;}
    public void setYear(int year) {this.year = year;}

    public String getTermType() {return termType;}
    public void setTermType(String termType) {this.termType = termType;}

    public List<Course> getCourses(){
        if (courses == null) {
            courses = new ArrayList<>();
        }
        return this.courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses == null ? new ArrayList<>() : new ArrayList<>(courses);
    }

    // Helper method
    public void addCourse(Course course){
        getCourses().add(course);
    }

    public void removeCourse(Course course){
        getCourses().remove(course);
    }

    public String getDisplayName(){
        return year + " - " + (termType == null ? "" : termType);
    }

    public double getGPA() {
        if (getCourses().isEmpty()) return 0.0;

        double totalPoints = 0;
        int totalCredits = 0;

        for (Course c : getCourses()) {
            if (!c.isOfficiallyComplete()) continue;

            double gpaPoints = convertToGPAPoints(c.getFinalScorePercent());
            totalPoints += gpaPoints * c.getCredits();
            totalCredits += c.getCredits();
        }

        return totalCredits == 0 ? 0.0 : totalPoints / totalCredits;
    }

    public double getAverageScore() {
        if (getCourses().isEmpty()) return 0.0;

        double totalScore = 0;
        int totalCredits = 0;

        for (Course c : getCourses()) {
            if (!c.isOfficiallyComplete()) continue;

            totalScore += c.getFinalScorePercent() * c.getCredits();
            totalCredits += c.getCredits();
        }
        return totalCredits == 0 ? 0.0 : totalScore / totalCredits;
        
    }

    public void normalizeAfterLoad() {
        if (termType == null || termType.isBlank()) {
            termType = "Sem 1";
        }
        getCourses().forEach(Course::normalizeAfterLoad);
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
}
