package com.academictracker.prediction.result;

public record TargetValidationResult(
        boolean valid,
        String warning,
        String impossibleReason,
        double maxAchievableGPA,
        double minAchievableGPA,
        boolean requiresConfirmation,
        Double suggestedTarget
) {
}
