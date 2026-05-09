package com.academictracker.prediction.model;

public class Assessment {
    private String name;
    private double weight;
    private Double score;
    private String assessmentType;

    public Assessment() {
    }

    public Assessment(String name, double weight, Double score) {
        this(name, weight, score, null);
    }

    public Assessment(String name, double weight, Double score, String assessmentType) {
        this.name = name;
        setWeight(weight);
        setScore(score);
        this.assessmentType = assessmentType;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        if (!Double.isFinite(weight)) {
            throw new IllegalArgumentException("Assessment weight must be a finite number.");
        }
        this.weight = weight;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        if (score != null && !Double.isFinite(score)) {
            throw new IllegalArgumentException("Assessment score must be a finite number.");
        }
        this.score = score;
    }

    public String getAssessmentType() {
        return assessmentType;
    }

    public void setAssessmentType(String assessmentType) {
        this.assessmentType = assessmentType;
    }

    public boolean isGraded() {
        return score != null;
    }
}
