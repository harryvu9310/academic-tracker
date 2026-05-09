package com.academictracker.prediction.model;

public record GradeBand(
        double minInclusive,
        double maxExclusive,
        String letter,
        double scale4,
        String classification,
        boolean passing
) {
    public boolean contains(double score) {
        return score >= minInclusive && score < maxExclusive;
    }
}
