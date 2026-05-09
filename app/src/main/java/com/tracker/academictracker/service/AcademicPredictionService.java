package com.tracker.academictracker.service;

import com.academictracker.prediction.engine.CoursePredictionEngine;
import com.academictracker.prediction.engine.GlobalPredictionEngine;
import com.academictracker.prediction.model.PredictionContext;
import com.academictracker.prediction.model.PredictionMode;
import com.academictracker.prediction.model.PredictionScope;
import com.academictracker.prediction.result.CoursePredictionResult;
import com.academictracker.prediction.result.GlobalPredictionResult;
import com.academictracker.prediction.result.TargetValidationResult;
import com.academictracker.prediction.result.WhatIfResult;
import com.tracker.academictracker.model.Course;
import com.tracker.academictracker.model.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AcademicPredictionService {
    private final CoursePredictionEngine coursePredictionEngine = new CoursePredictionEngine();
    private final GlobalPredictionEngine globalPredictionEngine = new GlobalPredictionEngine();

    public CoursePredictionResult predictCourse(Course course, double targetScore, PredictionMode mode) {
        InputValidator.parseScore100(String.valueOf(targetScore), "Course target score");
        return coursePredictionEngine.predict(
                AcademicPredictionMapper.toPredictionCourse(course),
                targetScore,
                mode
        );
    }

    public CoursePredictionResult predictCourse(Student student, Course course, double targetScore, PredictionMode mode) {
        InputValidator.parseScore100(String.valueOf(targetScore), "Course target score");
        return coursePredictionEngine.predict(
                AcademicPredictionMapper.toPredictionCourse(course),
                targetScore,
                mode,
                AcademicPredictionMapper.toPerformanceHistory(student),
                null
        );
    }

    public WhatIfResult predictCourseWhatIf(Course course, Map<String, Double> assessmentScores) {
        return coursePredictionEngine.customWhatIfScenario(
                AcademicPredictionMapper.toPredictionCourse(course),
                assessmentScores,
                PredictionMode.DRAFT
        );
    }

    public GlobalPredictionResult predictCumulative(Student student, Double targetGpa) {
        if (targetGpa != null) {
            InputValidator.parseGpaTarget(String.valueOf(targetGpa), "Cumulative GPA target");
        }
        PredictionContext context = AcademicPredictionMapper.toContext(student, targetGpa, null);
        return globalPredictionEngine.predictCumulativeTarget(context);
    }

    public GlobalPredictionResult predictMajor(Student student, Double targetGpa) {
        if (targetGpa != null) {
            InputValidator.parseGpaTarget(String.valueOf(targetGpa), "Major GPA target");
        }
        PredictionContext context = AcademicPredictionMapper.toContext(student, null, targetGpa);
        return globalPredictionEngine.predictMajorTarget(context);
    }

    public TargetValidationResult validateCumulativeTarget(Student student, double targetGpa) {
        PredictionContext context = AcademicPredictionMapper.toContext(student, targetGpa, null);
        return globalPredictionEngine.validateTargetBeforeSave(targetGpa, context);
    }

    public TargetValidationResult validateMajorTarget(Student student, double targetGpa) {
        PredictionContext context = AcademicPredictionMapper.toContext(student, null, targetGpa);
        return globalPredictionEngine.validateTargetBeforeSave(targetGpa, context, PredictionScope.MAJOR);
    }

    public String formatCoursePrediction(CoursePredictionResult result) {
        List<String> lines = new ArrayList<>();
        if (!result.valid()) {
            lines.add("Chưa thể dự đoán vì dữ liệu điểm chưa hợp lệ:");
            result.errors().forEach(error -> lines.add("- " + error));
            return String.join("\n", lines);
        }

        lines.add(result.explanationVi());
        lines.add(String.format(
                "Projected final: %.1f => %s / %.1f (%s)",
                result.projectedFinalScore(),
                result.projectedGrade().letterGrade(),
                result.projectedGrade().scale4(),
                result.projectedGrade().classification()
        ));
        lines.add("Passing status: " + (result.projectedGrade().passing() ? "Passing" : "No passing")
                + " | Risk: " + result.passRiskLevel());
        lines.add("Feasibility: " + result.feasibilityLabel());
        lines.add(String.format(
                "Profile expected score: %.1f | Confidence: %s",
                result.profileExpectedScore(),
                result.profileConfidence()
        ));
        lines.add("Profile note: " + result.profileExplanation());
        lines.add("Required score to pass: " + formatRequiredScore(result.requiredScoreForPass()));
        lines.add("Required score for target: " + formatRequiredScore(result.requiredScoreForTarget()));

        if (!result.requiredScoreForEachGradeBoundary().isEmpty()) {
            lines.add("Grade boundaries on remaining work:");
            result.requiredScoreForEachGradeBoundary().entrySet().stream()
                    .filter(entry -> Double.isFinite(entry.getValue()))
                    .limit(8)
                    .forEach(entry -> lines.add(String.format("- %s: %s", entry.getKey(), formatRequiredScore(entry.getValue()))));
        }

        if (!result.perAssessmentPlan().isEmpty()) {
            lines.add("Suggested per-assessment plan:");
            result.perAssessmentPlan().forEach(plan -> lines.add(String.format(
                    "- %s (%.0f%%): target %.1f, required %.1f",
                    plan.assessmentName(),
                    plan.weight() * 100.0,
                    plan.recommendedTarget(),
                    plan.requiredScore()
            )));
        }

        lines.add("Scenarios:");
        appendScenario(lines, result.conservativeScenario());
        appendScenario(lines, result.expectedScenario());
        appendScenario(lines, result.ambitiousScenario());

        if (!result.warnings().isEmpty()) {
            lines.add("Warnings:");
            result.warnings().forEach(warning -> lines.add("- " + warning));
        }
        return String.join("\n", lines);
    }

    public String formatGlobalPrediction(GlobalPredictionResult result) {
        List<String> lines = new ArrayList<>();
        String title = switch (result.scope()) {
            case MAJOR -> "MAJOR GPA TARGET";
            case BOTH -> "COMBINED GPA TARGET";
            case CUMULATIVE -> "CUMULATIVE GPA TARGET";
        };
        lines.add(title);
        lines.add(String.format("Current official GPA: %.2f", result.officialGPA()));
        lines.add(String.format("Current projected GPA: %.2f", result.currentProjectedGPA()));
        lines.add(String.format(
                "Scenario GPA estimate: conservative %.2f | expected %.2f | ambitious %.2f",
                result.conservativeScenarioGPA(),
                result.expectedScenarioGPA(),
                result.ambitiousScenarioGPA()
        ));
        lines.add(String.format(
                "Achievable range: %.2f - %.2f",
                result.minAchievableGPA(),
                result.maxAchievableGPA()
        ));

        if (result.progress() != null) {
            lines.add(String.format(
                    "Progress: %d/%d credits completed (%d in progress, %d planned)",
                    result.progress().completedCredits(),
                    result.progress().totalCreditsForProgress(),
                    result.progress().inProgressCredits(),
                    result.progress().plannedCredits()
            ));
        }

        if (result.targetProvided()) {
            lines.add(String.format("Target GPA: %.2f", result.targetGPA()));
            lines.add("Mathematical feasibility: " + (result.mathematicallyFeasible() ? "mathematically feasible" : "not feasible"));
            lines.add("Realistic feasibility: " + result.realisticFeasibility());
            lines.add("Risk level: " + result.riskLevel());
            lines.add("Prediction confidence: " + result.confidenceLevel());
            if (Double.isFinite(result.requiredAverageScale4())) {
                lines.add(String.format("Required average performance: approximately %.2f GPA points across remaining credits", result.requiredAverageScale4()));
            }
            lines.add(result.primaryAdvice());
            if (result.impossibleReason() != null) {
                lines.add(result.impossibleReason());
            }
        }

        if (!result.feasibleBandPlan().isEmpty()) {
            lines.add("Feasible band plan:");
            result.feasibleBandPlan().stream().limit(6).forEach(plan -> lines.add(String.format(
                    "- %s (%d credits): %s, minimum %.0f/100",
                    plan.courseName(),
                    plan.credits(),
                    plan.recommendedLetter(),
                    plan.minimumScore100()
            )));
        }

        if (!result.advisorMessages().isEmpty()) {
            lines.add("Advisor notes:");
            result.advisorMessages().stream().limit(6).forEach(message -> lines.add("- " + message));
        }

        if (!result.warnings().isEmpty()) {
            lines.add("Warnings:");
            result.warnings().forEach(warning -> lines.add("- " + warning));
        }
        return String.join("\n", lines);
    }

    private void appendScenario(List<String> lines, WhatIfResult scenario) {
        if (scenario == null) {
            return;
        }
        lines.add(String.format(
                "- %s: %.1f => %s / %.1f",
                scenario.scenarioName(),
                scenario.finalCourseScore(),
                scenario.grade().letterGrade(),
                scenario.grade().scale4()
        ));
    }

    private String formatRequiredScore(double score) {
        if (!Double.isFinite(score)) {
            return "not available";
        }
        if (score > 100.0) {
            return String.format("%.1f (not achievable)", score);
        }
        if (score < 0.0) {
            return String.format("%.1f (current entered assessments are above target pace, not certain yet)", score);
        }
        return String.format("%.1f/100", Math.max(0.0, score));
    }
}
