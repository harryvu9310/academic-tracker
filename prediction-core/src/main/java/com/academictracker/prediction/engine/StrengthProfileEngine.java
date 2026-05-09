package com.academictracker.prediction.engine;

import com.academictracker.prediction.model.ConfidenceLevel;
import com.academictracker.prediction.model.ScenarioType;
import com.academictracker.prediction.model.StrengthLevel;
import com.academictracker.prediction.model.StudentPerformanceHistory;
import com.academictracker.prediction.util.MathUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StrengthProfileEngine {
    private static final double DEFAULT_EXPECTED_SCORE = 70.0;
    private static final double SHRINKAGE_K = 3.0;

    public StrengthProfile profileForCategory(List<StudentPerformanceHistory> histories, String category) {
        List<PerformanceSample> globalSamples = samplesFor(histories, null);
        List<PerformanceSample> categorySamples = samplesFor(histories, category);

        if (globalSamples.isEmpty()) {
            return new StrengthProfile(
                    normalizedLabel(category),
                    StrengthLevel.AVERAGE,
                    DEFAULT_EXPECTED_SCORE,
                    0.0,
                    0,
                    0.0,
                    DEFAULT_EXPECTED_SCORE,
                    DEFAULT_EXPECTED_SCORE,
                    DEFAULT_EXPECTED_SCORE,
                    0,
                    ConfidenceLevel.UNKNOWN,
                    "UNKNOWN",
                    "Prediction confidence is unknown because no completed-course history is available."
            );
        }

        List<PerformanceSample> basis = categorySamples.isEmpty() ? globalSamples : categorySamples;
        double globalAverage = weightedAverage(globalSamples);
        double categoryAverage = weightedAverage(basis);
        int categorySampleSize = categorySamples.size();
        double categoryWeight = categorySampleSize <= 0 ? 0.0 : categorySampleSize / (categorySampleSize + SHRINKAGE_K);
        double globalWeight = 1.0 - categoryWeight;
        double shrunkAverage = categorySamples.isEmpty()
                ? globalAverage
                : (categoryWeight * categoryAverage) + (globalWeight * globalAverage);

        List<PerformanceSample> chronological = basis.stream()
                .sorted(Comparator.comparingInt(PerformanceSample::chronologicalOrder))
                .toList();
        double recentAverage = chronological.size() >= 3
                ? weightedAverage(chronological.subList(Math.max(0, chronological.size() - 3), chronological.size()))
                : shrunkAverage;
        double expected = chronological.size() >= 3
                ? (0.70 * shrunkAverage) + (0.30 * recentAverage)
                : shrunkAverage;
        double volatility = standardDeviation(basis);
        ConfidenceLevel confidence = confidenceFor(categorySamples.isEmpty() ? 0 : categorySampleSize, volatility);
        StrengthLevel level = strengthFor(expected);
        double best = basis.stream().mapToDouble(PerformanceSample::score).max().orElse(expected);
        double worst = basis.stream().mapToDouble(PerformanceSample::score).min().orElse(expected);
        long lowOrFailed = basis.stream().filter(sample -> sample.score() < 50.0 || sample.gpaPoints() < 2.0).count();
        String trend = trendFor(chronological);
        String explanation = explanation(category, categorySamples.isEmpty(), confidence, categorySampleSize, volatility);

        return new StrengthProfile(
                normalizedLabel(category),
                level,
                MathUtils.clamp(expected, 0.0, 100.0),
                volatility,
                categorySamples.isEmpty() ? globalSamples.size() : categorySampleSize,
                weightedGpa(basis),
                MathUtils.clamp(recentAverage, 0.0, 100.0),
                best,
                worst,
                (int) lowOrFailed,
                confidence,
                trend,
                explanation
        );
    }

    public double scoreForScenario(StrengthProfile profile, ScenarioType scenarioType) {
        StrengthProfile safeProfile = profile == null
                ? profileForCategory(List.of(), "General")
                : profile;
        double expected = safeProfile.averageScore();
        double conservativeMargin;
        double ambitiousMargin;
        switch (safeProfile.confidenceLevel()) {
            case HIGH -> {
                conservativeMargin = 5.0;
                ambitiousMargin = 7.0;
            }
            case MEDIUM -> {
                conservativeMargin = 8.0;
                ambitiousMargin = 8.0;
            }
            case LOW -> {
                conservativeMargin = 12.0;
                ambitiousMargin = 10.0;
            }
            case UNKNOWN -> {
                conservativeMargin = 15.0;
                ambitiousMargin = 10.0;
            }
            default -> {
                conservativeMargin = 10.0;
                ambitiousMargin = 8.0;
            }
        }
        if ("IMPROVING".equals(safeProfile.trendDirection())) {
            ambitiousMargin += 2.0;
        } else if ("DECLINING".equals(safeProfile.trendDirection())) {
            conservativeMargin += 2.0;
        }

        double score = switch (scenarioType) {
            case CONSERVATIVE -> expected - conservativeMargin;
            case EXPECTED -> expected;
            case AMBITIOUS -> expected + ambitiousMargin;
        };
        return MathUtils.clamp(score, 0.0, 100.0);
    }

    private List<PerformanceSample> samplesFor(List<StudentPerformanceHistory> histories, String category) {
        if (histories == null || histories.isEmpty()) {
            return List.of();
        }
        String normalizedCategory = normalize(category);
        List<PerformanceSample> samples = new ArrayList<>();
        int fallbackOrder = 0;
        for (StudentPerformanceHistory history : histories) {
            if (history == null) {
                continue;
            }
            boolean categoryMatches = category == null || category.isBlank()
                    || normalize(history.getCategory()).equals(normalizedCategory)
                    || normalize(history.getSubjectGroup()).equals(normalizedCategory);
            if (!categoryMatches) {
                continue;
            }
            if (!history.getCourseRecords().isEmpty()) {
                for (StudentPerformanceHistory.CoursePerformanceRecord record : history.getCourseRecords()) {
                    if (record == null || !Double.isFinite(record.finalPercentageScore())) {
                        continue;
                    }
                    samples.add(new PerformanceSample(
                            record.finalPercentageScore(),
                            Math.max(1, record.credits()),
                            Double.isFinite(record.gpaPoints()) ? record.gpaPoints() : 0.0,
                            record.chronologicalOrder()
                    ));
                }
            } else {
                for (Double score : history.getPastScores()) {
                    if (score != null && Double.isFinite(score)) {
                        samples.add(new PerformanceSample(score, 1, 0.0, fallbackOrder++));
                    }
                }
            }
        }
        return samples;
    }

    private double weightedAverage(List<PerformanceSample> samples) {
        double weighted = 0.0;
        int credits = 0;
        for (PerformanceSample sample : samples) {
            weighted += sample.score() * sample.credits();
            credits += sample.credits();
        }
        return credits == 0 ? DEFAULT_EXPECTED_SCORE : weighted / credits;
    }

    private double weightedGpa(List<PerformanceSample> samples) {
        double weighted = 0.0;
        int credits = 0;
        for (PerformanceSample sample : samples) {
            weighted += sample.gpaPoints() * sample.credits();
            credits += sample.credits();
        }
        return credits == 0 ? 0.0 : weighted / credits;
    }

    private double standardDeviation(List<PerformanceSample> samples) {
        return MathUtils.standardDeviation(samples.stream().map(PerformanceSample::score).toList());
    }

    private ConfidenceLevel confidenceFor(int categorySampleSize, double volatility) {
        if (categorySampleSize >= 5 && volatility <= 10.0) {
            return ConfidenceLevel.HIGH;
        }
        if (categorySampleSize >= 3) {
            return ConfidenceLevel.MEDIUM;
        }
        if (categorySampleSize >= 1) {
            return ConfidenceLevel.LOW;
        }
        return ConfidenceLevel.UNKNOWN;
    }

    private StrengthLevel strengthFor(double average) {
        if (average >= 85.0) {
            return StrengthLevel.STRONG;
        }
        if (average >= 75.0) {
            return StrengthLevel.GOOD;
        }
        if (average >= 65.0) {
            return StrengthLevel.AVERAGE;
        }
        return StrengthLevel.WEAK;
    }

    private String trendFor(List<PerformanceSample> chronological) {
        if (chronological.size() < 3) {
            return "INSUFFICIENT_DATA";
        }
        double first = chronological.get(0).score();
        double last = chronological.get(chronological.size() - 1).score();
        if (last - first >= 5.0) {
            return "IMPROVING";
        }
        if (first - last >= 5.0) {
            return "DECLINING";
        }
        return "STABLE";
    }

    private String explanation(String category, boolean usingGlobalFallback, ConfidenceLevel confidence, int sampleSize, double volatility) {
        if (usingGlobalFallback) {
            return "Prediction uses the global completed-course profile because category history is unavailable.";
        }
        if (confidence == ConfidenceLevel.UNKNOWN) {
            return "Prediction confidence is unknown because there are no completed courses in this category.";
        }
        if (confidence == ConfidenceLevel.LOW) {
            return "Prediction confidence is low because there are fewer than 3 completed courses in "
                    + normalizedLabel(category) + ".";
        }
        if (confidence == ConfidenceLevel.HIGH) {
            return "Prediction confidence is high because this category has enough low-volatility history.";
        }
        return "Prediction confidence is medium based on " + sampleSize
                + " completed courses; score volatility is " + String.format("%.1f", volatility) + ".";
    }

    private String normalizedLabel(String value) {
        return value == null || value.isBlank() ? "General" : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "general" : value.trim().toLowerCase();
    }

    public record StrengthProfile(
            String category,
            StrengthLevel level,
            double averageScore,
            double volatility,
            int sampleSize,
            double creditWeightedGpa,
            double recentAverageScore,
            double bestScore,
            double worstScore,
            int failedOrLowScoreCount,
            ConfidenceLevel confidenceLevel,
            String trendDirection,
            String explanation
    ) {
    }

    private record PerformanceSample(double score, int credits, double gpaPoints, int chronologicalOrder) {
    }
}
