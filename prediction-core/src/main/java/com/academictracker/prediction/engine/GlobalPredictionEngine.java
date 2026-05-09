package com.academictracker.prediction.engine;

import com.academictracker.prediction.advisor.VietnameseAdvisor;
import com.academictracker.prediction.core.GpaConverter;
import com.academictracker.prediction.core.GradeCalculator;
import com.academictracker.prediction.core.SemesterGpaCalculator;
import com.academictracker.prediction.model.Course;
import com.academictracker.prediction.model.CourseStatus;
import com.academictracker.prediction.model.GradeBand;
import com.academictracker.prediction.model.PredictionContext;
import com.academictracker.prediction.model.PredictionMode;
import com.academictracker.prediction.model.PredictionScope;
import com.academictracker.prediction.model.ProgramConfig;
import com.academictracker.prediction.model.ScenarioType;
import com.academictracker.prediction.result.CoursePredictionResult;
import com.academictracker.prediction.result.CombinedPredictionResult;
import com.academictracker.prediction.result.GlobalPredictionResult;
import com.academictracker.prediction.result.TargetValidationResult;
import com.academictracker.prediction.util.MathUtils;
import com.academictracker.prediction.util.ValidationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class GlobalPredictionEngine {
    private static final String UNKNOWN_TOTAL_WARNING =
            "Program total credits is unknown, prediction is based only on known courses.";

    private final GpaConverter gpaConverter = new GpaConverter();
    private final GradeCalculator gradeCalculator = new GradeCalculator(gpaConverter);
    private final SemesterGpaCalculator semesterGpaCalculator = new SemesterGpaCalculator();
    private final CoursePredictionEngine coursePredictionEngine = new CoursePredictionEngine();
    private final VietnameseAdvisor vietnameseAdvisor = new VietnameseAdvisor();

    public GlobalPredictionResult predictCumulativeTarget(PredictionContext context) {
        return predict(context, PredictionScope.CUMULATIVE);
    }

    public GlobalPredictionResult predictMajorTarget(PredictionContext context) {
        return predict(context, PredictionScope.MAJOR);
    }

    public CombinedPredictionResult predictBothTargets(PredictionContext context) {
        GlobalPredictionResult cumulative = predict(context, PredictionScope.CUMULATIVE);
        GlobalPredictionResult major = predict(context, PredictionScope.MAJOR);
        List<String> warnings = new ArrayList<>(cumulative.warnings());
        warnings.addAll(major.warnings());
        return new CombinedPredictionResult(
                cumulative,
                major,
                cumulative.targetProvided(),
                major.targetProvided(),
                cumulative.targetFeasible(),
                major.targetFeasible(),
                cumulative.impossibleReason(),
                major.impossibleReason(),
                cumulative.suggestedRealisticTarget(),
                major.suggestedRealisticTarget(),
                warnings.stream().filter(item -> item != null && !item.isBlank()).distinct().toList(),
                "Cumulative: " + cumulative.primaryAdvice() + "\nMajor: " + major.primaryAdvice()
        );
    }

    public TargetValidationResult validateTargetBeforeSave(double target, PredictionContext context) {
        return validateTargetBeforeSave(target, context, PredictionScope.CUMULATIVE);
    }

    public TargetValidationResult validateTargetBeforeSave(
            double target,
            PredictionContext context,
            PredictionScope scope
    ) {
        ValidationUtils.requireGpaRange(target);
        List<String> warnings = new ArrayList<>();
        Analysis analysis = analyzeOptions(context, scope, warnings);
        double min = analysis.minAchievableGpa();
        double max = analysis.maxAchievableGpa();
        if (target > max + 0.0001) {
            double suggested = Math.max(0.0, Math.floor(max * 10.0) / 10.0);
            return new TargetValidationResult(
                    false,
                    String.join(" ", warnings),
                    "Target " + String.format("%.2f", target)
                            + " is impossible with current remaining credits. Highest achievable GPA is "
                            + String.format("%.2f", max) + ".",
                    max,
                    min,
                    false,
                    suggested
            );
        }
        boolean requiresConfirmation = target > analysis.expectedScenarioGpa() + 0.25;
        String warning = target <= min + 0.0001
                ? "Bạn đang vượt mức target dựa trên điểm đã nhập. Tuy nhiên, kết quả cuối cùng vẫn có thể thay đổi nếu các assessment còn lại thấp."
                : String.join(" ", warnings);
        return new TargetValidationResult(
                true,
                warning == null || warning.isBlank() ? null : warning,
                null,
                max,
                min,
                requiresConfirmation,
                null
        );
    }

    public GlobalPredictionResult.ProgressSnapshot calculateProgress(PredictionContext context) {
        ProgramConfig config = context.getProgramConfig();
        int completed = config.getCompletedCredits() != null
                ? config.getCompletedCredits()
                : sumCredits(context.getCourses(), course -> course.getStatus() == CourseStatus.COMPLETED);
        int inProgress = config.getInProgressCredits() != null
                ? config.getInProgressCredits()
                : sumCredits(context.getCourses(), course -> course.getStatus() == CourseStatus.IN_PROGRESS);
        int planned = config.getPlannedCredits() != null
                ? config.getPlannedCredits()
                : sumCredits(context.getCourses(), course -> course.getStatus() == CourseStatus.PLANNED);
        int known = completed + inProgress + planned;
        Integer degreeTotal = config.getDegreeTotalCredits();
        boolean degreeKnown = degreeTotal != null && degreeTotal > 0;
        int totalForProgress = degreeKnown ? degreeTotal : known;
        String warning = degreeKnown ? null : UNKNOWN_TOTAL_WARNING;
        if (degreeKnown && known > totalForProgress) {
            totalForProgress = known;
            warning = "Known credits exceed configured program total; progress uses known credits.";
        }
        double progressRatio = totalForProgress == 0 ? 0.0 : completed / (double) totalForProgress;
        return new GlobalPredictionResult.ProgressSnapshot(
                completed,
                inProgress,
                planned,
                known,
                degreeTotal,
                totalForProgress,
                MathUtils.clamp(progressRatio, 0.0, 1.0),
                degreeKnown,
                warning
        );
    }

    private GlobalPredictionResult predict(PredictionContext context, PredictionScope scope) {
        List<String> warnings = new ArrayList<>();
        PredictionContext safeContext = context == null ? new PredictionContext() : context;
        Predicate<Course> filter = scopeFilter(scope, warnings, safeContext.getCourses());
        List<Course> relevantCourses = safeContext.getCourses().stream().filter(filter).toList();
        List<CoursePredictionResult> coursePredictions = relevantCourses.stream()
                .filter(course -> course.getStatus() != CourseStatus.COMPLETED)
                .map(course -> coursePredictionEngine.predict(
                        course,
                        course.getTargetScore() == null ? 70.0 : course.getTargetScore(),
                        PredictionMode.DRAFT,
                        safeContext.getStudentPerformanceHistory(),
                        null
                ))
                .toList();

        double official = semesterGpaCalculator.officialGPA(safeContext.getCourses(), filter);
        double currentProjected = semesterGpaCalculator.currentProjectedGPA(
                safeContext.getCourses(),
                safeContext.getStudentPerformanceHistory(),
                filter
        );
        double conservative = semesterGpaCalculator.scenarioGPA(
                safeContext.getCourses(),
                safeContext.getStudentPerformanceHistory(),
                ScenarioType.CONSERVATIVE,
                filter
        );
        double expected = semesterGpaCalculator.scenarioGPA(
                safeContext.getCourses(),
                safeContext.getStudentPerformanceHistory(),
                ScenarioType.EXPECTED,
                filter
        );
        double ambitious = semesterGpaCalculator.scenarioGPA(
                safeContext.getCourses(),
                safeContext.getStudentPerformanceHistory(),
                ScenarioType.AMBITIOUS,
                filter
        );

        Analysis analysis = analyzeOptions(safeContext, scope, warnings);
        Double target = targetForScope(safeContext, scope);
        boolean targetProvided = target != null;
        boolean mathematicallyFeasible = !targetProvided || target <= analysis.maxAchievableGpa() + 0.0001;
        boolean targetFeasible = mathematicallyFeasible;
        boolean realisticBasedOnHistory = !targetProvided || target <= ambitious + 0.0001;
        String riskLevel = riskLevel(targetProvided, mathematicallyFeasible, target, expected, ambitious, analysis.maxAchievableGpa());
        String realisticFeasibility = realisticFeasibility(targetProvided, mathematicallyFeasible, realisticBasedOnHistory, riskLevel);
        double requiredAverageScale4 = targetProvided
                ? requiredAverageScale4(safeContext, scope, filter, official, analysis.denominatorCredits(), target)
                : 0.0;
        String confidenceLevel = confidenceLevel(coursePredictions, safeContext);
        List<GlobalPredictionResult.CourseBandPlan> bandPlan = targetProvided
                ? planForTarget(analysis, target)
                : List.of();
        String impossibleReason = null;
        Double suggestedTarget = null;
        TargetValidationResult validation = null;
        if (targetProvided) {
            validation = validateTargetBeforeSave(target, safeContext, scope);
            impossibleReason = validation.impossibleReason();
            suggestedTarget = validation.suggestedTarget();
            targetFeasible = validation.valid();
        }

        GlobalPredictionResult.ProgressSnapshot progress = calculateProgress(safeContext);
        if (progress.warning() != null) {
            warnings.add(progress.warning());
        }
        List<String> distinctWarnings = warnings.stream()
                .filter(item -> item != null && !item.isBlank())
                .distinct()
                .toList();
        List<String> advisorMessages = new ArrayList<>();
        GlobalPredictionResult interim = new GlobalPredictionResult(
                scope,
                MathUtils.round2(official),
                MathUtils.round2(currentProjected),
                MathUtils.round2(conservative),
                MathUtils.round2(expected),
                MathUtils.round2(ambitious),
                MathUtils.round2(analysis.minAchievableGpa()),
                MathUtils.round2(analysis.maxAchievableGpa()),
                targetProvided,
                target,
                targetFeasible,
                impossibleReason,
                suggestedTarget,
                validation,
                progress,
                bandPlan,
                coursePredictions,
                distinctWarnings,
                advisorMessages,
                mathematicallyFeasible,
                realisticBasedOnHistory,
                realisticFeasibility,
                riskLevel,
                confidenceLevel,
                MathUtils.round2(requiredAverageScale4),
                ""
        );
        String primaryAdvice = vietnameseAdvisor.globalAdvice(interim);
        advisorMessages.add(primaryAdvice);
        advisorMessages.addAll(vietnameseAdvisor.priorityMessages(coursePredictions));
        return new GlobalPredictionResult(
                interim.scope(),
                interim.officialGPA(),
                interim.currentProjectedGPA(),
                interim.conservativeScenarioGPA(),
                interim.expectedScenarioGPA(),
                interim.ambitiousScenarioGPA(),
                interim.minAchievableGPA(),
                interim.maxAchievableGPA(),
                interim.targetProvided(),
                interim.targetGPA(),
                interim.targetFeasible(),
                interim.impossibleReason(),
                interim.suggestedRealisticTarget(),
                interim.targetValidation(),
                interim.progress(),
                interim.feasibleBandPlan(),
                interim.coursePredictions(),
                interim.warnings(),
                advisorMessages,
                interim.mathematicallyFeasible(),
                interim.realisticBasedOnHistory(),
                interim.realisticFeasibility(),
                interim.riskLevel(),
                interim.confidenceLevel(),
                interim.requiredAverageScale4(),
                primaryAdvice
        );
    }

    private PredictionScope effectiveScope(PredictionScope scope) {
        return scope == null ? PredictionScope.CUMULATIVE : scope;
    }

    private Predicate<Course> scopeFilter(PredictionScope scope, List<String> warnings, List<Course> courses) {
        PredictionScope effective = effectiveScope(scope);
        if (effective == PredictionScope.MAJOR) {
            boolean missingMajorInfo = courses.stream().anyMatch(course -> course.getMajorCourse() == null);
            if (missingMajorInfo) {
                warnings.add("Major GPA prediction is limited because some courses are missing major-course classification.");
            }
            return course -> Boolean.TRUE.equals(course.getMajorCourse());
        }
        return course -> true;
    }

    private Double targetForScope(PredictionContext context, PredictionScope scope) {
        return switch (effectiveScope(scope)) {
            case MAJOR -> context.getTargetMajorGPA();
            case BOTH, CUMULATIVE -> context.getTargetCumulativeGPA();
        };
    }

    private Analysis analyzeOptions(PredictionContext context, PredictionScope scope, List<String> warnings) {
        Predicate<Course> filter = scopeFilter(scope, warnings, context.getCourses());
        List<Course> relevantCourses = context.getCourses().stream().filter(filter).toList();
        int knownCredits = sumCredits(relevantCourses, course -> true);
        Integer configuredTotal = configuredTotalCredits(context.getProgramConfig(), effectiveScope(scope));
        int denominatorCredits;
        if (configuredTotal == null || configuredTotal <= 0) {
            denominatorCredits = knownCredits;
            warnings.add(UNKNOWN_TOTAL_WARNING);
        } else {
            denominatorCredits = configuredTotal;
        }
        if (denominatorCredits < knownCredits) {
            warnings.add("Known credits exceed configured total; GPA feasibility uses known credits.");
            denominatorCredits = knownCredits;
        }

        List<List<CourseBandOption>> choices = new ArrayList<>();
        for (Course course : relevantCourses) {
            choices.add(optionsForCourse(course));
        }
        int placeholderCredits = Math.max(0, denominatorCredits - knownCredits);
        if (placeholderCredits > 0) {
            choices.add(placeholderOptions(placeholderCredits));
            warnings.add("Some remaining credits are not mapped to courses; plan includes a placeholder.");
        }

        if (denominatorCredits == 0 || choices.isEmpty()) {
            return new Analysis(0, choices, 0.0, 0.0, 0.0, Map.of());
        }
        int minPoints = 0;
        int maxPoints = 0;
        for (List<CourseBandOption> optionGroup : choices) {
            int groupMin = optionGroup.stream().mapToInt(CourseBandOption::points2).min().orElse(0);
            int groupMax = optionGroup.stream().mapToInt(CourseBandOption::points2).max().orElse(0);
            minPoints += groupMin;
            maxPoints += groupMax;
        }
        Map<Integer, List<CourseBandOption>> reachable = reachablePlans(choices);
        double minGpa = minPoints / (denominatorCredits * 2.0);
        double maxGpa = maxPoints / (denominatorCredits * 2.0);
        double expectedGpa = semesterGpaCalculator.scenarioGPA(
                context.getCourses(),
                context.getStudentPerformanceHistory(),
                ScenarioType.EXPECTED,
                filter
        );
        return new Analysis(denominatorCredits, choices, minGpa, maxGpa, expectedGpa, reachable);
    }

    private List<GlobalPredictionResult.CourseBandPlan> planForTarget(Analysis analysis, double target) {
        if (analysis.denominatorCredits() == 0) {
            return List.of();
        }
        int targetPoints = (int) Math.ceil(target * analysis.denominatorCredits() * 2.0 - 0.000001);
        return analysis.reachablePlans().entrySet().stream()
                .filter(entry -> entry.getKey() >= targetPoints)
                .min(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue().stream().map(this::toBandPlan).toList())
                .orElse(List.of());
    }

    private Map<Integer, List<CourseBandOption>> reachablePlans(List<List<CourseBandOption>> choices) {
        Map<Integer, List<CourseBandOption>> states = new HashMap<>();
        states.put(0, List.of());
        for (List<CourseBandOption> group : choices) {
            Map<Integer, List<CourseBandOption>> next = new LinkedHashMap<>();
            for (Map.Entry<Integer, List<CourseBandOption>> state : states.entrySet()) {
                for (CourseBandOption option : group) {
                    int points = state.getKey() + option.points2();
                    next.computeIfAbsent(points, ignored -> {
                        List<CourseBandOption> plan = new ArrayList<>(state.getValue());
                        plan.add(option);
                        return List.copyOf(plan);
                    });
                }
            }
            states = next;
        }
        return states;
    }

    private List<CourseBandOption> optionsForCourse(Course course) {
        double minScore = semesterGpaCalculator.minAchievableScore(course);
        double maxScore = semesterGpaCalculator.maxAchievableScore(course);
        List<GradeBand> possibleBands = gpaConverter.possibleBandsForScoreRange(minScore, maxScore);
        if (possibleBands.isEmpty()) {
            possibleBands = List.of(gpaConverter.fromScore(MathUtils.clamp(maxScore, 0.0, 100.0)).scale4() >= 0
                    ? gpaConverter.minimumBandForScaleAtLeast(0.0)
                    : gpaConverter.minimumBandForScaleAtLeast(0.0));
        }
        return possibleBands.stream()
                .map(band -> new CourseBandOption(
                        course,
                        band,
                        (int) Math.round(band.scale4() * 2.0 * course.getCredits()),
                        Math.max(band.minInclusive(), minScore)
                ))
                .toList();
    }

    private List<CourseBandOption> placeholderOptions(int credits) {
        Course placeholder = new Course(
                "unmapped-remaining-credits",
                "Unspecified remaining credits",
                "Unmapped",
                credits,
                null,
                CourseStatus.PLANNED
        );
        return gpaConverter.bandsAscending().stream()
                .map(band -> new CourseBandOption(
                        placeholder,
                        band,
                        (int) Math.round(band.scale4() * 2.0 * credits),
                        band.minInclusive()
                ))
                .toList();
    }

    private GlobalPredictionResult.CourseBandPlan toBandPlan(CourseBandOption option) {
        Course course = option.course();
        String reason = course.getCourseName() + " nên nhắm " + option.band().letter()
                + " (tối thiểu " + String.format("%.0f", option.minimumScore100()) + ")"
                + " vì môn này có " + course.getCredits() + " tín chỉ.";
        return new GlobalPredictionResult.CourseBandPlan(
                course.getId(),
                course.getCourseName(),
                course.getCategory(),
                course.getCredits(),
                course.getMajorCourse(),
                option.band().letter(),
                option.band().scale4(),
                option.minimumScore100(),
                reason
        );
    }

    private String riskLevel(
            boolean targetProvided,
            boolean mathematicallyFeasible,
            Double target,
            double expectedScenarioGpa,
            double ambitiousScenarioGpa,
            double maxAchievableGpa
    ) {
        if (!targetProvided) {
            return "INFO";
        }
        if (!mathematicallyFeasible || target == null || target > maxAchievableGpa + 0.0001) {
            return "EXTREME";
        }
        if (target <= expectedScenarioGpa + 0.10) {
            return "LOW";
        }
        if (target <= ambitiousScenarioGpa + 0.10) {
            return "MEDIUM";
        }
        return "HIGH";
    }

    private String realisticFeasibility(
            boolean targetProvided,
            boolean mathematicallyFeasible,
            boolean realisticBasedOnHistory,
            String riskLevel
    ) {
        if (!targetProvided) {
            return "No target provided; GPA values are shown as official/projected/scenario context.";
        }
        if (!mathematicallyFeasible) {
            return "Impossible with the current credits and discrete grade bands.";
        }
        if (realisticBasedOnHistory) {
            return "Realistic under the current history-based scenario model; risk level: " + riskLevel + ".";
        }
        return "Mathematically feasible but unrealistic unless remaining-course performance improves sharply; risk level: "
                + riskLevel + ".";
    }

    private double requiredAverageScale4(
            PredictionContext context,
            PredictionScope scope,
            Predicate<Course> filter,
            double officialGpa,
            int denominatorCredits,
            double target
    ) {
        int officialCredits = sumCredits(context.getCourses(), course -> filter.test(course)
                && course.getStatus() == CourseStatus.COMPLETED
                && gradeCalculator.isOfficiallyComplete(course));
        int remainingCredits = Math.max(0, denominatorCredits - officialCredits);
        if (remainingCredits == 0) {
            return target <= officialGpa + 0.0001 ? 0.0 : 4.1;
        }
        double officialPoints = officialGpa * officialCredits;
        return (target * denominatorCredits - officialPoints) / remainingCredits;
    }

    private String confidenceLevel(List<CoursePredictionResult> coursePredictions, PredictionContext context) {
        if (context.getStudentPerformanceHistory().isEmpty()) {
            return "UNKNOWN";
        }
        if (coursePredictions.isEmpty()) {
            return "HIGH";
        }
        if (coursePredictions.stream().anyMatch(item -> "UNKNOWN".equals(item.profileConfidence()))) {
            return "UNKNOWN";
        }
        if (coursePredictions.stream().anyMatch(item -> "LOW".equals(item.profileConfidence()))) {
            return "LOW";
        }
        if (coursePredictions.stream().anyMatch(item -> "MEDIUM".equals(item.profileConfidence()))) {
            return "MEDIUM";
        }
        return "HIGH";
    }

    private int sumCredits(List<Course> courses, Predicate<Course> include) {
        int total = 0;
        for (Course course : courses) {
            if (course != null && include.test(course)) {
                total += course.getCredits();
            }
        }
        return total;
    }

    private Integer configuredTotalCredits(ProgramConfig config, PredictionScope scope) {
        if (scope == PredictionScope.MAJOR) {
            return config.getMajorRequiredCredits();
        }
        return config.getDegreeTotalCredits();
    }

    private record CourseBandOption(Course course, GradeBand band, int points2, double minimumScore100) {
    }

    private record Analysis(
            int denominatorCredits,
            List<List<CourseBandOption>> choices,
            double minAchievableGpa,
            double maxAchievableGpa,
            double expectedScenarioGpa,
            Map<Integer, List<CourseBandOption>> reachablePlans
    ) {
    }
}
