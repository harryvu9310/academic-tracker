package com.academictracker.prediction.core;

import com.academictracker.prediction.model.Course;
import com.academictracker.prediction.model.CourseStatus;
import com.academictracker.prediction.model.PredictionMode;
import com.academictracker.prediction.model.ScenarioType;
import com.academictracker.prediction.model.StudentPerformanceHistory;
import com.academictracker.prediction.engine.StrengthProfileEngine;

import java.util.List;
import java.util.function.Predicate;

public class SemesterGpaCalculator {
    private final GpaConverter gpaConverter = new GpaConverter();
    private final GradeCalculator gradeCalculator = new GradeCalculator(gpaConverter);
    private final StrengthProfileEngine strengthProfileEngine = new StrengthProfileEngine();

    public double officialGPA(List<Course> courses) {
        return officialGPA(courses, course -> true);
    }

    public double officialGPA(List<Course> courses, Predicate<Course> filter) {
        return weightedGpa(courses, course -> filter.test(course)
                && course.getStatus() == CourseStatus.COMPLETED
                && gradeCalculator.isOfficiallyComplete(course), course -> finalScore(course, 0.0));
    }

    public double currentProjectedGPA(List<Course> courses, List<StudentPerformanceHistory> histories) {
        return currentProjectedGPA(courses, histories, course -> true);
    }

    public double currentProjectedGPA(
            List<Course> courses,
            List<StudentPerformanceHistory> histories,
            Predicate<Course> filter
    ) {
        return weightedGpa(courses, course -> filter.test(course)
                && (course.getStatus() == CourseStatus.COMPLETED || course.getStatus() == CourseStatus.IN_PROGRESS),
                course -> {
                    if (course.getStatus() == CourseStatus.COMPLETED && gradeCalculator.isOfficiallyComplete(course)) {
                        return finalScore(course, 0.0);
                    }
                    return projectedScore(course, histories, ScenarioType.EXPECTED);
                });
    }

    public double scenarioGPA(List<Course> courses, List<StudentPerformanceHistory> histories, ScenarioType scenarioType) {
        return scenarioGPA(courses, histories, scenarioType, course -> true);
    }

    public double scenarioGPA(
            List<Course> courses,
            List<StudentPerformanceHistory> histories,
            ScenarioType scenarioType,
            Predicate<Course> filter
    ) {
        return weightedGpa(courses, course -> filter.test(course), course -> {
            if (course.getStatus() == CourseStatus.COMPLETED && gradeCalculator.isOfficiallyComplete(course)) {
                return finalScore(course, 0.0);
            }
            return projectedScore(course, histories, scenarioType);
        });
    }

    public double majorGPA(List<Course> courses) {
        return officialGPA(courses, course -> Boolean.TRUE.equals(course.getMajorCourse()));
    }

    public double cumulativeGPA(List<Course> courses) {
        return officialGPA(courses);
    }

    public double minAchievableScore(Course course) {
        if (course.getStatus() == CourseStatus.COMPLETED && gradeCalculator.isOfficiallyComplete(course)) {
            return finalScore(course, 0.0);
        }
        GradeCalculator.Calculation calculation = gradeCalculator.calculate(course, null, PredictionMode.DRAFT, 0.0);
        return Math.max(0.0, calculation.currentWeightedScore());
    }

    public double maxAchievableScore(Course course) {
        if (course.getStatus() == CourseStatus.COMPLETED && gradeCalculator.isOfficiallyComplete(course)) {
            return finalScore(course, 0.0);
        }
        GradeCalculator.Calculation calculation = gradeCalculator.calculate(course, null, PredictionMode.DRAFT, 100.0);
        return Math.min(100.0, calculation.maxPossibleScore());
    }

    private double weightedGpa(List<Course> courses, Predicate<Course> include, ScoreResolver scoreResolver) {
        double totalPoints = 0.0;
        int totalCredits = 0;
        for (Course course : courses) {
            if (course == null || course.getCredits() <= 0 || !include.test(course)) {
                continue;
            }
            double score = scoreResolver.score(course);
            if (!Double.isFinite(score)) {
                continue;
            }
            totalPoints += gpaConverter.fromScore(Math.max(0.0, Math.min(100.0, score))).scale4() * course.getCredits();
            totalCredits += course.getCredits();
        }
        return totalCredits == 0 ? 0.0 : totalPoints / totalCredits;
    }

    private double finalScore(Course course, double defaultMissingScore) {
        GradeCalculator.Calculation calculation = gradeCalculator.calculate(
                course,
                null,
                PredictionMode.DRAFT,
                defaultMissingScore
        );
        return calculation.currentWeightedScore()
                + (calculation.missingWeight() + calculation.unassignedWeight()) * defaultMissingScore;
    }

    private double projectedScore(Course course, List<StudentPerformanceHistory> histories, ScenarioType scenarioType) {
        StrengthProfileEngine.StrengthProfile strength = strengthProfileEngine.profileForCategory(histories, course.getCategory());
        double score = strengthProfileEngine.scoreForScenario(strength, scenarioType);
        return finalScore(course, score);
    }

    private interface ScoreResolver {
        double score(Course course);
    }
}
