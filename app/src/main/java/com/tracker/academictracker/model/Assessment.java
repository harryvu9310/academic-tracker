package com.tracker.academictracker.model;

public class Assessment {
    private String assessmentName;
    private String category;
    private double weight;     // Percentage out of 100
    private Double score;      // Points achieved
    private double maxScore;   // Total possible points

    // For Gson
    public Assessment() {
    }

    public Assessment(String assessmentName, String category, double weight, double maxScore) {
        this.assessmentName = assessmentName;
        this.category = category;
        setWeight(weight); // Use setter to enforce validation
        setMaxScore(maxScore);
        this.score = null;
    }

    // Getters and Setters
    public String getAssessmentName() { return assessmentName; }
    public void setAssessmentName(String assessmentName) { this.assessmentName = assessmentName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getMaxScore() { return maxScore; }
    public void setMaxScore(double maxScore) {
        if (!Double.isFinite(maxScore) || maxScore <= 0) {
            throw new IllegalArgumentException("Max score must be a finite number greater than 0");
        }
        this.maxScore = maxScore;
    }
    public double getWeight() {
        return weight;
    }
    public void setWeight(double weight) {
        if (!Double.isFinite(weight) || weight < 0 || weight > 100) {
            throw new IllegalArgumentException("Weight must be a finite number between 0 and 100%");
        }
        this.weight = weight;
    }
    public Double getScore() { return score; }
    public void setScore(Double score) {
        if (score != null && (!Double.isFinite(score) || score < 0 || score > getMaxScore())) {
            throw new IllegalArgumentException("Score must be a finite number between 0 and maxScore");
        }
        this.score = score;
    }

    public boolean isGraded() {
        return score != null;
    }

    public double getScorePercent() {
        if (!isGraded()) {
            return 0.0;
        }
        if (!Double.isFinite(score) || !Double.isFinite(maxScore) || maxScore <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(100.0, (score / getMaxScore()) * 100.0));
    }

    public double getWeightScore() {
        return isGraded() && Double.isFinite(weight) ? (getScorePercent() / 100.0) * getWeight() : 0.0;
    }

    public void normalizeAfterLoad() {
        if (assessmentName == null) {
            assessmentName = "";
        }
        if (category == null || category.isBlank()) {
            category = "General";
        }
        if (!Double.isFinite(weight) || weight < 0.0 || weight > 100.0) {
            throw new IllegalArgumentException("Assessment weight must be a finite number between 0 and 100%.");
        }
        if (!Double.isFinite(maxScore) || maxScore <= 0) {
            throw new IllegalArgumentException("Assessment max score must be a finite number greater than 0.");
        }
        if (score != null) {
            if (!Double.isFinite(score) || score < 0.0 || score > getMaxScore()) {
                throw new IllegalArgumentException("Assessment score must be a finite number between 0 and max score.");
            }
        }
    }
}
