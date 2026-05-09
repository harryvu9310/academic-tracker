package com.academictracker.prediction;

import com.academictracker.prediction.engine.GlobalPredictionEngine;
import com.academictracker.prediction.model.Assessment;
import com.academictracker.prediction.model.Course;
import com.academictracker.prediction.model.CourseStatus;
import com.academictracker.prediction.model.PredictionContext;
import com.academictracker.prediction.model.ProgramConfig;
import com.academictracker.prediction.model.StudentPerformanceHistory;
import com.academictracker.prediction.result.CombinedPredictionResult;
import com.academictracker.prediction.result.GlobalPredictionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalPredictionEngineTest {
    private final GlobalPredictionEngine engine = new GlobalPredictionEngine();

    @Test
    void progressUsesConfiguredDegreeTotalCreditsInsteadOfHardcoded120() {
        PredictionContext context = contextWith(course("Completed", 20, true, CourseStatus.COMPLETED, 90.0));
        context.getProgramConfig().setDegreeTotalCredits(100);

        assertEquals(0.20, engine.calculateProgress(context).progressRatio(), 0.001);

        context.getProgramConfig().setDegreeTotalCredits(150);
        assertEquals(20 / 150.0, engine.calculateProgress(context).progressRatio(), 0.001);
    }

    @Test
    void unknownDegreeTotalFallsBackToKnownCoursesWithWarning() {
        PredictionContext context = contextWith(
                course("Completed", 3, true, CourseStatus.COMPLETED, 90.0),
                course("Planned", 3, true, CourseStatus.PLANNED, null)
        );

        GlobalPredictionResult result = engine.predictCumulativeTarget(context);

        assertEquals(6, result.progress().totalCreditsForProgress());
        assertTrue(result.warnings().stream().anyMatch(item -> item.contains("Program total credits is unknown")));
    }

    @Test
    void officialGpaOnlyUsesCompletedCoursesButCurrentProjectedIncludesInProgress() {
        PredictionContext context = contextWith(
                course("Completed A", 3, true, CourseStatus.COMPLETED, 90.0),
                inProgressCourse("In Progress", 3, 70.0)
        );

        GlobalPredictionResult result = engine.predictCumulativeTarget(context);

        assertEquals(4.0, result.officialGPA(), 0.001);
        assertTrue(result.currentProjectedGPA() < result.officialGPA());
        assertTrue(result.currentProjectedGPA() > 0.0);
    }

    @Test
    void officialGpaExcludesPlannedAndIncompleteCompletedCourses() {
        Course incompleteCompleted = new Course("incomplete", "Incomplete", "General", 3, true, CourseStatus.COMPLETED);
        incompleteCompleted.addAssessment(new Assessment("Midterm", 0.5, 100.0));
        Course planned = new Course("planned", "Planned", "General", 3, true, CourseStatus.PLANNED);
        planned.addAssessment(new Assessment("Placeholder", 1.0, null));
        PredictionContext context = contextWith(
                course("Completed A", 3, true, CourseStatus.COMPLETED, 90.0),
                incompleteCompleted,
                planned
        );

        GlobalPredictionResult result = engine.predictCumulativeTarget(context);

        assertEquals(4.0, result.officialGPA(), 0.001);
    }

    @Test
    void scenarioGpaUsesConservativeExpectedAndAmbitiousBands() {
        PredictionContext context = contextWith(new Course("stats", "Statistics", "Quantitative", 3, true, CourseStatus.PLANNED));
        context.setStudentPerformanceHistory(List.of(new StudentPerformanceHistory("Quantitative", List.of(90.0, 88.0), null)));

        GlobalPredictionResult result = engine.predictCumulativeTarget(context);

        assertTrue(result.conservativeScenarioGPA() < result.expectedScenarioGPA());
        assertTrue(result.expectedScenarioGPA() < result.ambitiousScenarioGPA());
    }

    @Test
    void impossibleTargetReportsMaxAchievableGpa() {
        PredictionContext context = contextWith(course("Completed A", 3, true, CourseStatus.COMPLETED, 80.0));
        context.setTargetCumulativeGPA(4.0);

        GlobalPredictionResult result = engine.predictCumulativeTarget(context);

        assertFalse(result.targetFeasible());
        assertEquals(3.5, result.maxAchievableGPA(), 0.001);
        assertTrue(result.impossibleReason().contains("Highest achievable GPA"));
    }

    @Test
    void targetFeasibilityUsesDiscreteBandsAndCredits() {
        Course course = new Course("almost", "Almost A", "General", 3, true, CourseStatus.IN_PROGRESS);
        course.addAssessment(new Assessment("Current", 1.0, 79.99));
        PredictionContext context = contextWith(course);
        context.setTargetCumulativeGPA(3.2);

        GlobalPredictionResult result = engine.predictCumulativeTarget(context);

        assertFalse(result.targetFeasible());
        assertEquals(3.0, result.maxAchievableGPA(), 0.001);
    }

    @Test
    void majorGpaOnlyUsesMajorCoursesAndWarnsForMissingClassification() {
        PredictionContext context = contextWith(
                course("Major A", 3, true, CourseStatus.COMPLETED, 90.0),
                course("Non Major F", 3, false, CourseStatus.COMPLETED, 20.0),
                course("Unknown", 3, null, CourseStatus.PLANNED, null)
        );
        context.getProgramConfig().setMajorRequiredCredits(3);
        context.setTargetMajorGPA(3.5);

        GlobalPredictionResult result = engine.predictMajorTarget(context);

        assertEquals(4.0, result.officialGPA(), 0.001);
        assertTrue(result.warnings().stream().anyMatch(item -> item.contains("missing major-course classification")));
    }

    @Test
    void primaryAdviceUsesBandsScoresAndCourseActionsInsteadOfNeedGpaWording() {
        PredictionContext context = contextWith(
                course("Completed B", 3, true, CourseStatus.COMPLETED, 70.0),
                new Course("java", "Java", "Programming", 3, true, CourseStatus.PLANNED)
        );
        context.setTargetCumulativeGPA(3.2);

        GlobalPredictionResult result = engine.predictCumulativeTarget(context);

        assertFalse(result.primaryAdvice().contains("need GPA"));
        assertTrue(result.primaryAdvice().contains("Java") || result.primaryAdvice().contains(">="));
    }

    @Test
    void predictBothTargetsPreservesCumulativeFeasibleAndMajorImpossibleSeparately() {
        PredictionContext context = contextWith(
                course("Major A", 3, true, CourseStatus.COMPLETED, 80.0),
                course("General A", 3, false, CourseStatus.COMPLETED, 80.0)
        );
        context.getProgramConfig().setDegreeTotalCredits(6);
        context.getProgramConfig().setMajorRequiredCredits(3);
        context.setTargetCumulativeGPA(3.5);
        context.setTargetMajorGPA(4.0);

        CombinedPredictionResult result = engine.predictBothTargets(context);

        assertTrue(result.cumulativeFeasible());
        assertFalse(result.majorFeasible());
        assertTrue(result.majorImpossibleReason().contains("Highest achievable GPA"));
        assertEquals(3.5, result.majorSuggestedTarget(), 0.001);
    }

    @Test
    void predictBothTargetsPreservesCumulativeImpossibleAndMajorFeasibleSeparately() {
        PredictionContext context = contextWith(
                course("Major A", 3, true, CourseStatus.COMPLETED, 80.0),
                course("General C", 3, false, CourseStatus.COMPLETED, 50.0)
        );
        context.getProgramConfig().setDegreeTotalCredits(6);
        context.getProgramConfig().setMajorRequiredCredits(3);
        context.setTargetCumulativeGPA(4.0);
        context.setTargetMajorGPA(3.5);

        CombinedPredictionResult result = engine.predictBothTargets(context);

        assertFalse(result.cumulativeFeasible());
        assertTrue(result.majorFeasible());
        assertTrue(result.cumulativeImpossibleReason().contains("Highest achievable GPA"));
    }

    @Test
    void predictBothTargetsHandlesBothFeasibleAndBothImpossible() {
        PredictionContext feasible = contextWith(
                course("Major A", 3, true, CourseStatus.COMPLETED, 80.0),
                course("General A", 3, false, CourseStatus.COMPLETED, 80.0)
        );
        feasible.getProgramConfig().setDegreeTotalCredits(6);
        feasible.getProgramConfig().setMajorRequiredCredits(3);
        feasible.setTargetCumulativeGPA(3.5);
        feasible.setTargetMajorGPA(3.5);

        CombinedPredictionResult bothFeasible = engine.predictBothTargets(feasible);
        assertTrue(bothFeasible.cumulativeFeasible());
        assertTrue(bothFeasible.majorFeasible());

        PredictionContext impossible = contextWith(
                course("Major C", 3, true, CourseStatus.COMPLETED, 50.0),
                course("General C", 3, false, CourseStatus.COMPLETED, 50.0)
        );
        impossible.getProgramConfig().setDegreeTotalCredits(6);
        impossible.getProgramConfig().setMajorRequiredCredits(3);
        impossible.setTargetCumulativeGPA(4.0);
        impossible.setTargetMajorGPA(4.0);

        CombinedPredictionResult bothImpossible = engine.predictBothTargets(impossible);
        assertFalse(bothImpossible.cumulativeFeasible());
        assertFalse(bothImpossible.majorFeasible());
        assertTrue(bothImpossible.cumulativeImpossibleReason().contains("Highest achievable GPA"));
        assertTrue(bothImpossible.majorImpossibleReason().contains("Highest achievable GPA"));
    }

    private PredictionContext contextWith(Course... courses) {
        PredictionContext context = new PredictionContext();
        context.setCourses(List.of(courses));
        context.setProgramConfig(new ProgramConfig());
        return context;
    }

    private Course course(String name, int credits, Boolean major, CourseStatus status, Double score) {
        Course course = new Course(name.toLowerCase().replace(" ", "-"), name, "General", credits, major, status);
        if (score != null) {
            course.addAssessment(new Assessment("Final", 1.0, score));
        }
        return course;
    }

    private Course inProgressCourse(String name, int credits, double currentScore) {
        Course course = new Course(name.toLowerCase().replace(" ", "-"), name, "General", credits, true, CourseStatus.IN_PROGRESS);
        course.addAssessment(new Assessment("Midterm", 0.5, currentScore));
        course.addAssessment(new Assessment("Final", 0.5, null));
        return course;
    }
}
