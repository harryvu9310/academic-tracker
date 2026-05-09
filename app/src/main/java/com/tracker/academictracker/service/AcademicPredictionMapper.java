package com.tracker.academictracker.service;

import com.academictracker.prediction.model.PredictionContext;
import com.academictracker.prediction.model.ProgramConfig;
import com.academictracker.prediction.model.StudentPerformanceHistory;
import com.academictracker.prediction.core.GpaConverter;
import com.academictracker.prediction.result.GpaResult;
import com.academictracker.prediction.util.ValidationUtils;
import com.tracker.academictracker.model.Assessment;
import com.tracker.academictracker.model.Course;
import com.tracker.academictracker.model.CourseStatus;
import com.tracker.academictracker.model.Semester;
import com.tracker.academictracker.model.Student;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps JavaFX app models separated from the reusable prediction-core models.
 */
public final class AcademicPredictionMapper {
    private static final GpaConverter GPA_CONVERTER = new GpaConverter();

    private AcademicPredictionMapper() {
    }

    public static PredictionContext toContext(Student student, Double targetCumulativeGpa, Double targetMajorGpa) {
        PredictionContext context = new PredictionContext();
        if (student == null) {
            return context;
        }
        if (targetCumulativeGpa != null) {
            ValidationUtils.requireGpaRange(targetCumulativeGpa);
        }
        if (targetMajorGpa != null) {
            ValidationUtils.requireGpaRange(targetMajorGpa);
        }

        List<com.academictracker.prediction.model.Course> predictionCourses = new ArrayList<>();
        for (Semester semester : student.getSemesters()) {
            for (Course course : semester.getCourses()) {
                if (course.getStatus() == CourseStatus.DROPPED) {
                    continue;
                }
                predictionCourses.add(toPredictionCourse(course));
            }
        }

        ProgramConfig config = new ProgramConfig();
        config.setDegreeTotalCredits(student.getDegreeTotalCredits());
        config.setMajorRequiredCredits(student.getMajorRequiredCredits());
        config.setCompletedCredits(sumCredits(student, CourseStatus.COMPLETED, true));
        config.setInProgressCredits(sumCredits(student, CourseStatus.ACTIVE, false));
        config.setPlannedCredits(sumCredits(student, CourseStatus.PLANNED, false));

        context.setCourses(predictionCourses);
        context.setProgramConfig(config);
        context.setStudentPerformanceHistory(toPerformanceHistory(student));
        context.setTargetCumulativeGPA(targetCumulativeGpa);
        context.setTargetMajorGPA(targetMajorGpa);
        return context;
    }

    public static com.academictracker.prediction.model.Course toPredictionCourse(Course source) {
        com.academictracker.prediction.model.Course target = new com.academictracker.prediction.model.Course(
                safeId(source),
                source.getCourseName(),
                source.getCategory(),
                source.getCredits(),
                source.getMajorCourse(),
                toPredictionStatus(source)
        );
        if (source.getTargetScore() != null && Double.isFinite(source.getTargetScore())) {
            target.setTargetScore(source.getTargetScore());
        }
        for (Assessment assessment : source.getAssessments()) {
            target.addAssessment(new com.academictracker.prediction.model.Assessment(
                    assessment.getAssessmentName(),
                    assessment.getWeight() / 100.0,
                    assessment.isGraded() ? assessment.getScorePercent() : null,
                    assessment.getCategory()
            ));
        }
        return target;
    }

    public static List<StudentPerformanceHistory> toPerformanceHistory(Student student) {
        if (student == null) {
            return List.of();
        }
        Map<String, StudentPerformanceHistory> grouped = new LinkedHashMap<>();
        int order = 0;
        for (Semester semester : student.getSemesters()) {
            for (Course course : semester.getCourses()) {
                if (!course.isOfficiallyComplete()) {
                    order++;
                    continue;
                }
                double finalPercent = course.getFinalScorePercent();
                if (!Double.isFinite(finalPercent)) {
                    order++;
                    continue;
                }
                GpaResult grade = GPA_CONVERTER.fromScore(Math.max(0.0, Math.min(100.0, finalPercent)));
                String category = course.getCategory();
                StudentPerformanceHistory history = grouped.computeIfAbsent(
                        normalize(category),
                        ignored -> new StudentPerformanceHistory(category, category, new ArrayList<>(), null, new ArrayList<>())
                );
                history.getPastScores().add(finalPercent);
                history.addCourseRecord(new StudentPerformanceHistory.CoursePerformanceRecord(
                        safeId(course),
                        course.getCourseName(),
                        course.getCredits(),
                        finalPercent,
                        grade.scale4(),
                        grade.letterGrade(),
                        category,
                        category,
                        course.getMajorCourse(),
                        semester.getYear(),
                        semester.getTermType(),
                        order
                ));
                order++;
            }
        }
        return new ArrayList<>(grouped.values());
    }

    private static com.academictracker.prediction.model.CourseStatus toPredictionStatus(Course source) {
        CourseStatus status = source.getStatus();
        if (status == CourseStatus.COMPLETED) {
            if (!source.isOfficiallyComplete()) {
                return com.academictracker.prediction.model.CourseStatus.IN_PROGRESS;
            }
            return com.academictracker.prediction.model.CourseStatus.COMPLETED;
        }
        if (status == CourseStatus.PLANNED) {
            return com.academictracker.prediction.model.CourseStatus.PLANNED;
        }
        return com.academictracker.prediction.model.CourseStatus.IN_PROGRESS;
    }

    private static int sumCredits(Student student, CourseStatus status, boolean officialOnly) {
        int total = 0;
        for (Semester semester : student.getSemesters()) {
            for (Course course : semester.getCourses()) {
                if (course.getStatus() != status) {
                    continue;
                }
                if (officialOnly && !course.isOfficiallyComplete()) {
                    continue;
                }
                total += course.getCredits();
            }
        }
        return total;
    }

    private static String normalize(String value) {
        return value == null ? "general" : value.trim().toLowerCase();
    }

    private static String safeId(Course course) {
        if (course.getCourseCode() != null && !course.getCourseCode().isBlank()) {
            return course.getCourseCode();
        }
        return course.getCourseName() == null ? "" : course.getCourseName();
    }
}
