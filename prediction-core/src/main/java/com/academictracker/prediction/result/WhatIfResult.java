package com.academictracker.prediction.result;

import java.util.List;
import java.util.Map;

public record WhatIfResult(
        String scenarioName,
        double finalCourseScore,
        GpaResult grade,
        Map<String, Double> assessmentScores,
        List<String> warnings,
        String explanationVi
) {
    public WhatIfResult {
        assessmentScores = assessmentScores == null ? Map.of() : Map.copyOf(assessmentScores);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
