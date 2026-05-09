package com.academictracker.prediction.core;

import com.academictracker.prediction.constants.GradingScale;
import com.academictracker.prediction.model.GradeBand;
import com.academictracker.prediction.result.GpaResult;
import com.academictracker.prediction.util.ValidationUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class GpaConverter {
    private final List<GradeBand> bands;

    public GpaConverter() {
        this(GradingScale.defaultBands());
    }

    public GpaConverter(List<GradeBand> bands) {
        this.bands = bands == null || bands.isEmpty() ? GradingScale.defaultBands() : List.copyOf(bands);
    }

    /** Converts a 100-point score to the official letter/4-point band. */
    public GpaResult fromScore(double score100) {
        ValidationUtils.requireScoreRange(score100);
        GradeBand band = bands.stream()
                .filter(item -> item.contains(score100))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No GPA band found for score: " + score100));
        return new GpaResult(score100, band.letter(), band.scale4(), band.classification(), band.passing());
    }

    public double minimumScoreForLetter(String letter) {
        return bandForLetter(letter).minInclusive();
    }

    public GradeBand bandForLetter(String letter) {
        return bands.stream()
                .filter(item -> item.letter().equalsIgnoreCase(letter))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown letter grade: " + letter));
    }

    public double minimumScoreForScale(double targetScale4) {
        ValidationUtils.requireGpaRange(targetScale4);
        return minimumBandForScaleAtLeast(targetScale4).minInclusive();
    }

    public GradeBand minimumBandForScaleAtLeast(double targetScale4) {
        ValidationUtils.requireGpaRange(targetScale4);
        return bands.stream()
                .filter(item -> item.scale4() >= targetScale4)
                .min(Comparator.comparingDouble(GradeBand::scale4))
                .orElseThrow(() -> new IllegalArgumentException("No GPA band can satisfy scale: " + targetScale4));
    }

    public Optional<GradeBand> nextHigherBand(double score100) {
        ValidationUtils.requireScoreRange(score100);
        GpaResult current = fromScore(Math.min(100.0, Math.max(0.0, score100)));
        return bands.stream()
                .filter(item -> item.scale4() > current.scale4())
                .min(Comparator.comparingDouble(GradeBand::scale4));
    }

    public List<GradeBand> possibleBandsForScoreRange(double minScore100, double maxScore100) {
        if (!Double.isFinite(minScore100) || !Double.isFinite(maxScore100)) {
            return List.of();
        }
        double min = Math.max(0.0, minScore100);
        double max = Math.min(100.0, maxScore100);
        return bands.stream()
                .filter(item -> max + 0.0000001 >= item.minInclusive() && min < item.maxExclusive())
                .sorted(Comparator.comparingDouble(GradeBand::scale4))
                .toList();
    }

    public List<GradeBand> bandsAscending() {
        return bands.stream()
                .sorted(Comparator.comparingDouble(GradeBand::scale4))
                .toList();
    }

    public List<GradeBand> bandsDescending() {
        return bands.stream()
                .sorted(Comparator.comparingDouble(GradeBand::scale4).reversed())
                .toList();
    }
}
