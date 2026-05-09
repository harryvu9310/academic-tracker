package com.academictracker.prediction.core;

import com.academictracker.prediction.constants.GradingScale;
import com.academictracker.prediction.model.Assessment;
import com.academictracker.prediction.model.Course;
import com.academictracker.prediction.model.PassRiskLevel;
import com.academictracker.prediction.model.PredictionMode;
import com.academictracker.prediction.util.MathUtils;
import com.academictracker.prediction.util.ValidationUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GradeCalculator {
    private final GpaConverter gpaConverter;

    public GradeCalculator() {
        this(new GpaConverter());
    }

    public GradeCalculator(GpaConverter gpaConverter) {
        this.gpaConverter = gpaConverter;
    }

    /**
     * Calculates course-level grade state without UI assumptions.
     */
    public Calculation calculate(Course course, Double targetScore, PredictionMode mode, double expectedMissingScore) {
        List<String> errors = new ArrayList<>(ValidationUtils.validateCourse(course));
        List<String> warnings = new ArrayList<>();
        if (course == null) {
            return Calculation.invalid(errors);
        }
        if (targetScore != null) {
            if (!Double.isFinite(targetScore)) {
                errors.add("Target score must be a finite number.");
            } else if (targetScore < GradingScale.MIN_SCORE || targetScore > GradingScale.MAX_SCORE) {
                errors.add("Target score must be between 0 and 100.");
            }
        }
        if (!Double.isFinite(expectedMissingScore)) {
            errors.add("Expected missing score must be a finite number.");
            expectedMissingScore = GradingScale.PASSING_SCORE;
        }

        double totalAssignedWeight = 0.0;
        double completedWeight = 0.0;
        double missingWeight = 0.0;
        double currentWeightedScore = 0.0;

        for (Assessment assessment : course.getAssessments()) {
            if (assessment == null || !Double.isFinite(assessment.getWeight())) {
                continue;
            }
            totalAssignedWeight += assessment.getWeight();
            if (assessment.isGraded()) {
                if (assessment.getScore() == null || !Double.isFinite(assessment.getScore())) {
                    continue;
                }
                completedWeight += assessment.getWeight();
                currentWeightedScore += assessment.getScore() * assessment.getWeight();
            } else {
                missingWeight += assessment.getWeight();
            }
        }

        if (totalAssignedWeight > 1.0 + GradingScale.WEIGHT_TOLERANCE) {
            errors.add("Course assessment weight exceeds 100%.");
        }
        if (mode == PredictionMode.STRICT && Math.abs(totalAssignedWeight - 1.0) > GradingScale.WEIGHT_TOLERANCE) {
            errors.add("Strict mode requires total assessment weight to be 100%.");
        }

        double unassignedWeight = Math.max(0.0, 1.0 - totalAssignedWeight);
        if (mode == PredictionMode.DRAFT && unassignedWeight > GradingScale.WEIGHT_TOLERANCE) {
            warnings.add("Course weight is incomplete. Prediction is a draft based on assigned components only.");
        }

        double remainingCapacity = errors.isEmpty()
                ? missingWeight + (mode == PredictionMode.DRAFT ? unassignedWeight : 0.0)
                : 0.0;
        double currentPaceScore = completedWeight > 0.0 ? currentWeightedScore / completedWeight : 0.0;
        double projectedFinalScore = MathUtils.clamp(
                currentWeightedScore + remainingCapacity * expectedMissingScore,
                0.0,
                100.0
        );

        double requiredScoreForPass = requiredAverageForTarget(currentWeightedScore, remainingCapacity, GradingScale.PASSING_SCORE);
        double requiredScoreForTarget = targetScore == null
                ? 0.0
                : requiredAverageForTarget(currentWeightedScore, remainingCapacity, targetScore);
        Map<String, Double> boundaryRequirements = new LinkedHashMap<>();
        for (var band : gpaConverter.bandsDescending()) {
            boundaryRequirements.put(band.letter(), requiredAverageForTarget(
                    currentWeightedScore,
                    remainingCapacity,
                    band.minInclusive()
            ));
        }

        double maxPossible = currentWeightedScore + remainingCapacity * 100.0;
        PassRiskLevel passRiskLevel = passRiskLevel(projectedFinalScore, requiredScoreForPass, maxPossible);

        return new Calculation(
                errors.isEmpty(),
                warnings,
                errors,
                totalAssignedWeight,
                completedWeight,
                missingWeight,
                unassignedWeight,
                currentWeightedScore,
                currentPaceScore,
                projectedFinalScore,
                requiredScoreForPass,
                requiredScoreForTarget,
                boundaryRequirements,
                passRiskLevel,
                maxPossible
        );
    }

    public double requiredAverageForTarget(double currentWeightedScore, double remainingCapacity, double targetScore) {
        if (!Double.isFinite(currentWeightedScore) || !Double.isFinite(remainingCapacity) || !Double.isFinite(targetScore)) {
            return 101.0;
        }
        if (remainingCapacity <= GradingScale.WEIGHT_TOLERANCE) {
            return currentWeightedScore >= targetScore ? 0.0 : 101.0;
        }
        return (targetScore - currentWeightedScore) / remainingCapacity;
    }

    public boolean isOfficiallyComplete(Course course) {
        if (course == null || course.getAssessments().isEmpty()) {
            return false;
        }
        Calculation calculation = calculate(course, null, PredictionMode.STRICT, 0.0);
        return calculation.valid() && calculation.missingWeight() <= GradingScale.WEIGHT_TOLERANCE;
    }

    private PassRiskLevel passRiskLevel(double projectedFinalScore, double requiredToPass, double maxPossible) {
        if (maxPossible < GradingScale.PASSING_SCORE || requiredToPass > 100.0) {
            return PassRiskLevel.CRITICAL;
        }
        if (projectedFinalScore < GradingScale.PASSING_SCORE || requiredToPass > 75.0) {
            return PassRiskLevel.RISKY;
        }
        if (requiredToPass > 50.0 || projectedFinalScore < 60.0) {
            return PassRiskLevel.WATCH;
        }
        return PassRiskLevel.SAFE;
    }

    public record Calculation(
            boolean valid,
            List<String> warnings,
            List<String> errors,
            double totalAssignedWeight,
            double completedWeight,
            double missingWeight,
            double unassignedWeight,
            double currentWeightedScore,
            double currentPaceScore,
            double projectedFinalScore,
            double requiredScoreForPass,
            double requiredScoreForTarget,
            Map<String, Double> requiredScoreForEachGradeBoundary,
            PassRiskLevel passRiskLevel,
            double maxPossibleScore
    ) {
        public Calculation {
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
            errors = errors == null ? List.of() : List.copyOf(errors);
            requiredScoreForEachGradeBoundary = requiredScoreForEachGradeBoundary == null
                    ? Map.of()
                    : Map.copyOf(requiredScoreForEachGradeBoundary);
        }

        static Calculation invalid(List<String> errors) {
            return new Calculation(false, List.of(), errors, 0, 0, 0, 0, 0, 0, 0,
                    101.0, 101.0, Map.of(), PassRiskLevel.CRITICAL, 0);
        }
    }
}
