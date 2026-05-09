package com.academictracker.prediction.util;

import com.academictracker.prediction.constants.GradingScale;
import com.academictracker.prediction.model.Assessment;
import com.academictracker.prediction.model.Course;

import java.util.ArrayList;
import java.util.List;

public final class ValidationUtils {
    private ValidationUtils() {
    }

    public static List<String> validateCourse(Course course) {
        List<String> errors = new ArrayList<>();
        if (course == null) {
            errors.add("Course is required.");
            return errors;
        }
        if (course.getCredits() <= 0) {
            errors.add("Course credits must be greater than 0.");
        }
        if (course.getTargetScore() != null) {
            if (!Double.isFinite(course.getTargetScore())) {
                errors.add("Course target score must be a finite number.");
            } else if (course.getTargetScore() < GradingScale.MIN_SCORE
                    || course.getTargetScore() > GradingScale.MAX_SCORE) {
                errors.add("Course target score must be between 0 and 100.");
            }
        }
        for (Assessment assessment : course.getAssessments()) {
            if (assessment == null) {
                errors.add("Assessment cannot be null.");
                continue;
            }
            if (!Double.isFinite(assessment.getWeight())) {
                errors.add("Assessment weight must be a finite number: " + assessment.getName());
            } else if (assessment.getWeight() < 0) {
                errors.add("Assessment weight cannot be negative: " + assessment.getName());
            } else if (assessment.getWeight() > 1.0 + GradingScale.WEIGHT_TOLERANCE) {
                errors.add("Assessment weight cannot exceed 100%: " + assessment.getName());
            }
            if (assessment.getScore() != null) {
                if (!Double.isFinite(assessment.getScore())) {
                    errors.add("Assessment score must be a finite number: " + assessment.getName());
                } else if (assessment.getScore() < GradingScale.MIN_SCORE
                        || assessment.getScore() > GradingScale.MAX_SCORE) {
                    errors.add("Assessment score must be between 0 and 100: " + assessment.getName());
                }
            }
        }
        return errors;
    }

    public static void requireScoreRange(double score) {
        if (!Double.isFinite(score) || score < GradingScale.MIN_SCORE || score > GradingScale.MAX_SCORE) {
            throw new IllegalArgumentException("score must be a finite number between 0 and 100");
        }
    }

    public static void requireGpaRange(double gpa) {
        if (!Double.isFinite(gpa) || gpa < 0.0 || gpa > 4.0) {
            throw new IllegalArgumentException("GPA target must be a finite number between 0.0 and 4.0");
        }
    }

    public static void requireFinite(String label, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be a finite number.");
        }
    }

    public static void requireWeightUnitRange(double weight) {
        if (!Double.isFinite(weight) || weight < 0.0 || weight > 1.0 + GradingScale.WEIGHT_TOLERANCE) {
            throw new IllegalArgumentException("Assessment weight must be finite and between 0% and 100%.");
        }
    }
}
