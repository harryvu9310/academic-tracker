package com.academictracker.prediction.result;

public record GpaResult(
        double score100,
        String letterGrade,
        double scale4,
        String classification,
        boolean passing
) {
}
