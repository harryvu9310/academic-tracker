package com.academictracker.prediction.engine;

import com.academictracker.prediction.advisor.VietnameseAdvisor;
import com.academictracker.prediction.core.GpaConverter;
import com.academictracker.prediction.core.GradeCalculator;
import com.academictracker.prediction.model.Assessment;
import com.academictracker.prediction.model.Course;
import com.academictracker.prediction.model.PredictionMode;
import com.academictracker.prediction.model.ScenarioType;
import com.academictracker.prediction.model.StudentPerformanceHistory;
import com.academictracker.prediction.result.CoursePredictionResult;
import com.academictracker.prediction.result.GpaResult;
import com.academictracker.prediction.result.WhatIfResult;
import com.academictracker.prediction.util.MathUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CoursePredictionEngine {
    private final GpaConverter gpaConverter;
    private final GradeCalculator gradeCalculator;
    private final StrengthProfileEngine strengthProfileEngine;
    private final TargetRecommendationEngine targetRecommendationEngine;
    private final PriorityEngine priorityEngine;
    private final VietnameseAdvisor vietnameseAdvisor;

    public CoursePredictionEngine() {
        this.gpaConverter = new GpaConverter();
        this.gradeCalculator = new GradeCalculator(gpaConverter);
        this.strengthProfileEngine = new StrengthProfileEngine();
        this.targetRecommendationEngine = new TargetRecommendationEngine(gpaConverter);
        this.priorityEngine = new PriorityEngine();
        this.vietnameseAdvisor = new VietnameseAdvisor();
    }

    public CoursePredictionResult predict(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Course is required.");
        }
        double target = course.getTargetScore() == null ? 50.0 : course.getTargetScore();
        return predict(course, target, PredictionMode.DRAFT, List.of(), null);
    }

    public CoursePredictionResult predict(Course course, double targetScore, PredictionMode mode) {
        return predict(course, targetScore, mode, List.of(), null);
    }

    public CoursePredictionResult predict(
            Course course,
            double targetScore,
            PredictionMode mode,
            List<StudentPerformanceHistory> histories,
            Map<String, Double> customAssessmentScores
    ) {
        if (course == null) {
            throw new IllegalArgumentException("Course is required.");
        }
        StrengthProfileEngine.StrengthProfile strength = strengthProfileEngine.profileForCategory(histories, course.getCategory());
        double expectedMissingScore = strengthProfileEngine.scoreForScenario(strength, ScenarioType.EXPECTED);
        GradeCalculator.Calculation calculation = gradeCalculator.calculate(course, targetScore, mode, expectedMissingScore);

        List<String> warnings = new ArrayList<>(calculation.warnings());
        List<String> errors = new ArrayList<>(calculation.errors());
        WhatIfResult conservative = buildScenario(course, mode, "conservative",
                strengthProfileEngine.scoreForScenario(strength, ScenarioType.CONSERVATIVE), Map.of());
        WhatIfResult expected = buildScenario(course, mode, "expected", expectedMissingScore, Map.of());
        WhatIfResult ambitious = buildScenario(course, mode, "ambitious",
                strengthProfileEngine.scoreForScenario(strength, ScenarioType.AMBITIOUS), Map.of());
        WhatIfResult custom = customAssessmentScores == null
                ? null
                : buildScenario(course, mode, "custom", expectedMissingScore, customAssessmentScores);

        double projectedScore = expected.finalCourseScore();
        GpaResult projectedGrade = gpaConverter.fromScore(MathUtils.clamp(projectedScore, 0.0, 100.0));
        if (!projectedGrade.passing()) {
            warnings.add("Expected score is below the passing threshold.");
        }

        double recommendedTarget = targetRecommendationEngine.recommendedTarget(
                course,
                calculation.requiredScoreForTarget(),
                strength.level()
        );
        GpaResult recommendedGrade = gpaConverter.fromScore(recommendedTarget);
        List<CoursePredictionResult.AssessmentPlan> plans = perAssessmentPlan(
                course,
                targetScore,
                recommendedTarget,
                calculation
        );
        PriorityEngine.PriorityRecommendation priority = priorityEngine.evaluate(
                course,
                calculation.passRiskLevel(),
                projectedScore,
                recommendedTarget,
                projectedGrade,
                strength.level()
        );
        String feasibility = feasibilityLabel(calculation.requiredScoreForTarget(), calculation.maxPossibleScore(), targetScore);
        String riskExplanation = riskExplanation(calculation.requiredScoreForTarget(), expectedMissingScore, strength);

        CoursePredictionResult interim = new CoursePredictionResult(
                course.getId(),
                course.getCourseName(),
                course.getCategory(),
                course.getCredits(),
                course.getMajorCourse(),
                mode,
                calculation.valid(),
                warnings,
                errors,
                calculation.totalAssignedWeight(),
                calculation.completedWeight(),
                calculation.missingWeight(),
                calculation.unassignedWeight(),
                calculation.currentWeightedScore(),
                calculation.currentPaceScore(),
                projectedScore,
                projectedGrade,
                calculation.requiredScoreForPass(),
                calculation.requiredScoreForTarget(),
                calculation.requiredScoreForEachGradeBoundary(),
                plans,
                calculation.passRiskLevel(),
                recommendedTarget,
                recommendedGrade,
                conservative,
                expected,
                ambitious,
                custom,
                priority.priorityLevel(),
                priority.reasonVi(),
                feasibility,
                riskExplanation,
                strength.confidenceLevel().name(),
                expectedMissingScore,
                strength.explanation(),
                ""
        );

        return new CoursePredictionResult(
                interim.courseId(),
                interim.courseName(),
                interim.category(),
                interim.credits(),
                interim.majorCourse(),
                interim.mode(),
                interim.valid(),
                interim.warnings(),
                interim.errors(),
                interim.totalAssignedWeight(),
                interim.completedWeight(),
                interim.missingWeight(),
                interim.unassignedWeight(),
                interim.currentWeightedScore(),
                interim.currentPaceScore(),
                interim.projectedFinalScore(),
                interim.projectedGrade(),
                interim.requiredScoreForPass(),
                interim.requiredScoreForTarget(),
                interim.requiredScoreForEachGradeBoundary(),
                interim.perAssessmentPlan(),
                interim.passRiskLevel(),
                interim.recommendedTargetScore(),
                interim.recommendedTargetGrade(),
                interim.conservativeScenario(),
                interim.expectedScenario(),
                interim.ambitiousScenario(),
                interim.customWhatIfScenario(),
                interim.priorityLevel(),
                interim.priorityReason(),
                interim.feasibilityLabel(),
                interim.riskExplanation(),
                interim.profileConfidence(),
                interim.profileExpectedScore(),
                interim.profileExplanation(),
                vietnameseAdvisor.courseExplanation(course, interim)
        );
    }

    public WhatIfResult customWhatIfScenario(Course course, Map<String, Double> assessmentScores, PredictionMode mode) {
        if (course == null) {
            throw new IllegalArgumentException("Course is required.");
        }
        StrengthProfileEngine.StrengthProfile strength = strengthProfileEngine.profileForCategory(List.of(), course.getCategory());
        return buildScenario(course, mode, "custom", strengthProfileEngine.scoreForScenario(strength, ScenarioType.EXPECTED), assessmentScores);
    }

    private WhatIfResult buildScenario(
            Course course,
            PredictionMode mode,
            String scenarioName,
            double defaultMissingScore,
            Map<String, Double> customAssessmentScores
    ) {
        List<String> warnings = new ArrayList<>();
        Map<String, Double> customScores = customAssessmentScores == null ? Map.of() : customAssessmentScores;
        double totalAssignedWeight = 0.0;
        double finalScore = 0.0;
        Map<String, Double> appliedScores = new LinkedHashMap<>();

        for (Assessment assessment : course.getAssessments()) {
            if (assessment == null || !Double.isFinite(assessment.getWeight())) {
                warnings.add("Invalid assessment weight ignored in scenario.");
                continue;
            }
            totalAssignedWeight += assessment.getWeight();
            Double score = assessment.getScore();
            if (score == null) {
                score = customScores.getOrDefault(assessment.getName(), defaultMissingScore);
            }
            if (score == null || !Double.isFinite(score)) {
                warnings.add("Invalid assessment score ignored in scenario: " + assessment.getName());
                score = defaultMissingScore;
            }
            double clampedScore = MathUtils.clamp(score, 0.0, 100.0);
            appliedScores.put(assessment.getName(), clampedScore);
            finalScore += assessment.getWeight() * clampedScore;
        }

        if (totalAssignedWeight > 1.0 + 0.001) {
            warnings.add("Assessment weight exceeds 100%; scenario output should not be used.");
        }
        double unassignedWeight = Math.max(0.0, 1.0 - totalAssignedWeight);
        if (mode == PredictionMode.DRAFT && unassignedWeight > 0.001) {
            finalScore += unassignedWeight * defaultMissingScore;
            warnings.add("Unassigned weight is estimated in draft mode.");
        }
        finalScore = MathUtils.clamp(finalScore, 0.0, 100.0);
        GpaResult grade = gpaConverter.fromScore(finalScore);
        String explanation = "Nếu scenario " + scenarioName + " xảy ra, final course score khoảng "
                + String.format("%.1f", finalScore)
                + ", tương ứng " + grade.letterGrade()
                + " / " + String.format("%.1f", grade.scale4()) + ".";
        return new WhatIfResult(scenarioName, finalScore, grade, appliedScores, warnings, explanation);
    }

    private List<CoursePredictionResult.AssessmentPlan> perAssessmentPlan(
            Course course,
            double targetScore,
            double recommendedTarget,
            GradeCalculator.Calculation calculation
    ) {
        List<CoursePredictionResult.AssessmentPlan> plans = new ArrayList<>();
        double remainingCapacity = calculation.missingWeight() + calculation.unassignedWeight();
        if (remainingCapacity <= 0.001) {
            return plans;
        }
        for (Assessment assessment : course.getAssessments()) {
            if (assessment.isGraded()) {
                continue;
            }
            double otherWeight = Math.max(0.0, remainingCapacity - assessment.getWeight());
            double requiredThis = assessment.getWeight() <= 0.0
                    ? 101.0
                    : (targetScore - calculation.currentWeightedScore() - otherWeight * recommendedTarget) / assessment.getWeight();
            double recommendedThis = MathUtils.clamp(Math.max(requiredThis, recommendedTarget), 0.0, 100.0);
            plans.add(new CoursePredictionResult.AssessmentPlan(
                    assessment.getName(),
                    assessment.getWeight(),
                    requiredThis,
                    recommendedThis,
                    "Nếu các phần còn lại giữ khoảng " + String.format("%.1f", recommendedTarget)
                            + ", " + assessment.getName() + " cần khoảng "
                            + String.format("%.1f", requiredThis) + "."
            ));
        }
        if (calculation.unassignedWeight() > 0.001) {
            plans.add(new CoursePredictionResult.AssessmentPlan(
                    "Unassigned weight",
                    calculation.unassignedWeight(),
                    calculation.requiredScoreForTarget(),
                    MathUtils.clamp(Math.max(calculation.requiredScoreForTarget(), recommendedTarget), 0.0, 100.0),
                    "Phần weight chưa gán cần được cấu hình trước khi dùng strict mode."
            ));
        }
        return plans;
    }

    private String feasibilityLabel(double requiredRemainingAverage, double maxPossibleScore, double targetScore) {
        if (!Double.isFinite(requiredRemainingAverage)) {
            return "IMPOSSIBLE";
        }
        if (maxPossibleScore + 0.0001 < targetScore) {
            return "IMPOSSIBLE_TARGET_ALREADY_LOST";
        }
        if (requiredRemainingAverage <= 0.0) {
            return "ABOVE_TARGET_PACE_NOT_CERTAIN";
        }
        if (requiredRemainingAverage <= 75.0) {
            return "FEASIBLE";
        }
        if (requiredRemainingAverage <= 90.0) {
            return "CHALLENGING_BUT_POSSIBLE";
        }
        if (requiredRemainingAverage <= 100.0) {
            return "HIGH_RISK_POSSIBLE";
        }
        return "IMPOSSIBLE_REQUIRES_OVER_100";
    }

    private String riskExplanation(
            double requiredRemainingAverage,
            double profileExpectedScore,
            StrengthProfileEngine.StrengthProfile strength
    ) {
        if (!Double.isFinite(requiredRemainingAverage)) {
            return "No remaining weighted work can recover this target in the current course structure.";
        }
        double gap = requiredRemainingAverage - profileExpectedScore;
        if (requiredRemainingAverage <= 0.0) {
            return "Current entered assessments are above target pace, but remaining assessments can still change the final result.";
        }
        if (gap > 20.0) {
            return "Required remaining average is far above the student's "
                    + strength.category() + " expected profile (" + String.format("%.1f", profileExpectedScore) + ").";
        }
        if (gap > 10.0) {
            return "Required remaining average is above the student's historical profile, so this is high-risk.";
        }
        return "Required remaining average is aligned with the current historical profile.";
    }
}
