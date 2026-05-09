package com.academictracker.prediction.result;

import java.util.List;

public record CombinedPredictionResult(
        GlobalPredictionResult cumulativeResult,
        GlobalPredictionResult majorResult,
        boolean cumulativeTargetProvided,
        boolean majorTargetProvided,
        boolean cumulativeFeasible,
        boolean majorFeasible,
        String cumulativeImpossibleReason,
        String majorImpossibleReason,
        Double cumulativeSuggestedTarget,
        Double majorSuggestedTarget,
        List<String> warnings,
        String summary
) {
    public CombinedPredictionResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
