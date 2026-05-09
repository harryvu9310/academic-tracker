package com.tracker.academictracker.service;

import com.tracker.academictracker.model.Assessment;
import com.tracker.academictracker.model.Course;
import com.tracker.academictracker.model.Semester;
import com.tracker.academictracker.model.Student;

import java.util.HashSet;
import java.util.Set;

public final class StudentDataValidator {
    private StudentDataValidator() {
    }

    public static void validateForPersistence(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("Student cannot be null.");
        }
        if (student.getDegreeTotalCredits() != null && student.getDegreeTotalCredits() <= 0) {
            throw new IllegalArgumentException("Degree total credits must be greater than 0.");
        }
        if (student.getMajorRequiredCredits() != null && student.getMajorRequiredCredits() <= 0) {
            throw new IllegalArgumentException("Major required credits must be greater than 0.");
        }

        Set<String> semesterKeys = new HashSet<>();
        for (Semester semester : student.getSemesters()) {
            if (semester == null) {
                throw new IllegalArgumentException("Semester cannot be null.");
            }
            String semesterKey = semester.getYear() + "::" + safeLower(semester.getTermType());
            if (!semesterKeys.add(semesterKey)) {
                throw new IllegalArgumentException("Duplicate semester detected: " + semester.getDisplayName());
            }
            validateSemester(semester);
        }
    }

    private static void validateSemester(Semester semester) {
        Set<String> courseCodes = new HashSet<>();
        for (Course course : semester.getCourses()) {
            if (course == null) {
                throw new IllegalArgumentException("Course cannot be null.");
            }
            if (course.getCredits() <= 0) {
                throw new IllegalArgumentException("Course credits must be greater than 0: " + course.getCourseCode());
            }
            if (course.getTargetScore() != null && (!Double.isFinite(course.getTargetScore())
                    || course.getTargetScore() < 0.0 || course.getTargetScore() > 100.0)) {
                throw new IllegalArgumentException("Course target score must be a finite number between 0 and 100: "
                        + course.getCourseCode());
            }
            String code = course.getCourseCode() == null ? "" : course.getCourseCode().trim();
            if (!code.isEmpty() && !courseCodes.add(code.toLowerCase())) {
                throw new IllegalArgumentException("Duplicate course code in semester " + semester.getDisplayName()
                        + ": " + code);
            }
            for (Assessment assessment : course.getAssessments()) {
                validateAssessment(course, assessment);
            }
        }
    }

    private static void validateAssessment(Course course, Assessment assessment) {
        if (assessment == null) {
            throw new IllegalArgumentException("Assessment cannot be null in course " + course.getCourseCode() + ".");
        }
        if (!Double.isFinite(assessment.getWeight()) || assessment.getWeight() < 0.0 || assessment.getWeight() > 100.0) {
            throw new IllegalArgumentException("Assessment weight must be finite and between 0 and 100 in course "
                    + course.getCourseCode() + ".");
        }
        if (!Double.isFinite(assessment.getMaxScore()) || assessment.getMaxScore() <= 0.0) {
            throw new IllegalArgumentException("Assessment max score must be finite and greater than 0 in course "
                    + course.getCourseCode() + ".");
        }
        if (assessment.getScore() != null && (!Double.isFinite(assessment.getScore())
                || assessment.getScore() < 0.0 || assessment.getScore() > assessment.getMaxScore())) {
            throw new IllegalArgumentException("Assessment score must be finite and between 0 and max score in course "
                    + course.getCourseCode() + ".");
        }
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
