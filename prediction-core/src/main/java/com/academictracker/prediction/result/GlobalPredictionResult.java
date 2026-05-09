package com.academictracker.prediction.result;

import com.academictracker.prediction.model.PredictionScope;

import java.util.List;

public record GlobalPredictionResult(
        PredictionScope scope,
        double officialGPA,
        double currentProjectedGPA,
        double conservativeScenarioGPA,
        double expectedScenarioGPA,
        double ambitiousScenarioGPA,
        double minAchievableGPA,
        double maxAchievableGPA,
        boolean targetProvided,
        Double targetGPA,
        boolean targetFeasible,
        String impossibleReason,
        Double suggestedRealisticTarget,
        TargetValidationResult targetValidation,
        ProgressSnapshot progress,
        List<CourseBandPlan> feasibleBandPlan,
        List<CoursePredictionResult> coursePredictions,
        List<String> warnings,
        List<String> advisorMessages,
        boolean mathematicallyFeasible,
        boolean realisticBasedOnHistory,
        String realisticFeasibility,
        String riskLevel,
        String confidenceLevel,
        double requiredAverageScale4,
        String primaryAdvice
) {
    public GlobalPredictionResult {
        feasibleBandPlan = feasibleBandPlan == null ? List.of() : List.copyOf(feasibleBandPlan);
        coursePredictions = coursePredictions == null ? List.of() : List.copyOf(coursePredictions);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        advisorMessages = advisorMessages == null ? List.of() : List.copyOf(advisorMessages);
    }

    public record ProgressSnapshot(
            int completedCredits,
            int inProgressCredits,
            int plannedCredits,
            int totalKnownCredits,
            Integer degreeTotalCredits,
            int totalCreditsForProgress,
            double progressRatio,
            boolean degreeTotalCreditsKnown,
            String warning
    ) {
    }

    public record CourseBandPlan(
            String courseId,
            String courseName,
            String category,
            int credits,
            Boolean majorCourse,
            String recommendedLetter,
            double recommendedScale4,
            double minimumScore100,
            String reasonVi
    ) {
    }
}
