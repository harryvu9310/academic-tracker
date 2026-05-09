package com.tracker.academictracker;

import com.academictracker.prediction.model.PredictionMode;
import com.academictracker.prediction.result.CoursePredictionResult;
import com.academictracker.prediction.result.GlobalPredictionResult;
import com.academictracker.prediction.result.TargetValidationResult;
import com.tracker.academictracker.model.Assessment;
import com.tracker.academictracker.model.Course;
import com.tracker.academictracker.model.CourseStatus;
import com.tracker.academictracker.model.Semester;
import com.tracker.academictracker.model.Student;
import com.tracker.academictracker.service.AcademicPredictionMapper;
import com.tracker.academictracker.service.AcademicPredictionService;
import com.tracker.academictracker.service.CourseTargetService;
import com.tracker.academictracker.service.DashboardTargetService;
import com.tracker.academictracker.service.DataManager;
import com.tracker.academictracker.service.InputValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcademicTrackerIntegrationTest {
    private final AcademicPredictionService service = new AcademicPredictionService();

    @Test
    void officialGpaExcludesActiveAndIncompleteCompletedCourses() {
        Student student = new Student("Test Student", "ST001");
        Semester semester = new Semester(2026, "Sem 1");

        Course active = new Course("JAVA", "Java", 3);
        active.addAssessment(new Assessment("Midterm", "Exam", 50.0, 100.0));
        active.getAssessments().get(0).setScore(100.0);

        Course incompleteCompleted = new Course("DB", "Database", 3);
        incompleteCompleted.setStatus(CourseStatus.COMPLETED);
        incompleteCompleted.addAssessment(new Assessment("Midterm", "Exam", 50.0, 100.0));
        incompleteCompleted.getAssessments().get(0).setScore(100.0);

        Course complete = new Course("ENG", "English", 2);
        complete.setStatus(CourseStatus.COMPLETED);
        complete.addAssessment(new Assessment("Final", "Exam", 100.0, 100.0));
        complete.getAssessments().get(0).setScore(80.0);

        semester.addCourse(active);
        semester.addCourse(incompleteCompleted);
        semester.addCourse(complete);
        student.addSemester(semester);

        assertEquals(3.5, student.getCumulativeGPA(), 0.001);
        assertEquals(2, student.getTotalCreditsCompleted());
    }

    @Test
    void coursePredictionMapsAppWeightsAndMaxScoresIntoPredictionCore() {
        Course course = new Course("JAVA", "Java", 3);
        course.addAssessment(new Assessment("Midterm", "Exam", 30.0, 10.0));
        course.getAssessments().get(0).setScore(9.0);
        course.addAssessment(new Assessment("Final", "Exam", 70.0, 100.0));

        CoursePredictionResult result = service.predictCourse(course, 80.0, PredictionMode.DRAFT);

        assertTrue(result.valid());
        assertEquals(75.714, result.requiredScoreForTarget(), 0.001);
    }

    @Test
    void globalPredictionUsesDiscreteFeasibilityAndRejectsImpossibleTarget() {
        Student student = new Student("Test Student", "ST002");
        Semester semester = new Semester(2026, "Sem 1");
        Course complete = new Course("MATH", "Math", 3);
        complete.setStatus(CourseStatus.COMPLETED);
        complete.addAssessment(new Assessment("Final", "Exam", 100.0, 100.0));
        complete.getAssessments().get(0).setScore(80.0);
        semester.addCourse(complete);
        student.addSemester(semester);

        TargetValidationResult validation = service.validateCumulativeTarget(student, 4.0);
        GlobalPredictionResult result = service.predictCumulative(student, 4.0);

        assertFalse(validation.valid());
        assertFalse(result.targetFeasible());
        assertEquals(3.5, result.maxAchievableGPA(), 0.001);
    }

    @Test
    void invalidCreditsAreRejectedBeforeGpaMath() {
        assertThrows(IllegalArgumentException.class, () -> new Course("BAD", "Bad Data", 0));
        assertThrows(IllegalArgumentException.class, () -> new Course("BAD", "Bad Data", -3));

        Course course = new Course();
        assertThrows(IllegalArgumentException.class, () -> course.setCredits(0));
        assertThrows(IllegalArgumentException.class, () -> course.setCredits(-1));
        course.setCredits(1);
        assertEquals(1, course.getCredits());
    }

    @Test
    void inputValidatorRejectsNanAndInfinityWithFriendlyErrors() {
        assertThrows(IllegalArgumentException.class, () -> InputValidator.parseScore100("NaN", "Score"));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.parseScore100("Infinity", "Score"));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.parseWeightPercent("NaN"));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.parseWeightPercent("-Infinity"));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.parseGpaTarget("NaN", "GPA target"));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.parseGpaTarget("Infinity", "GPA target"));
    }

    @Test
    void mapperCreatesHistoryOnlyFromOfficiallyCompletedCourses() {
        Student student = new Student("History Student", "ST003");
        Semester semester = new Semester(2026, "Sem 1");
        semester.addCourse(completedCourse("PROG1", "Programming I", "Programming", 3, true, 90.0));
        Course active = new Course("PROG2", "Programming II", 3);
        active.setCategory("Programming");
        active.addAssessment(new Assessment("Midterm", "Exam", 50.0, 100.0));
        active.getAssessments().get(0).setScore(100.0);
        semester.addCourse(active);
        Course planned = new Course("PLAN", "Planned", 3);
        planned.setStatus(CourseStatus.PLANNED);
        semester.addCourse(planned);
        student.addSemester(semester);

        var history = AcademicPredictionMapper.toPerformanceHistory(student);

        assertEquals(1, history.size());
        assertEquals(1, history.getFirst().getCourseRecords().size());
        assertEquals("PROG1", history.getFirst().getCourseRecords().getFirst().courseCode());
    }

    @Test
    void mapperTreatsCompletedButIncompleteCourseAsInProgressScenarioInput() {
        Course incompleteCompleted = new Course("CAP", "Capstone", 4);
        incompleteCompleted.setStatus(CourseStatus.COMPLETED);
        incompleteCompleted.addAssessment(new Assessment("Draft", "Project", 50.0, 100.0));
        incompleteCompleted.getAssessments().getFirst().setScore(90.0);

        var predictionCourse = AcademicPredictionMapper.toPredictionCourse(incompleteCompleted);

        assertTrue(incompleteCompleted.needsFinalization());
        assertEquals(com.academictracker.prediction.model.CourseStatus.IN_PROGRESS, predictionCourse.getStatus());
    }

    @Test
    void categoryHistoryChangesCoursePredictionExpectedScore() {
        Student student = new Student("Prediction Student", "ST004");
        Semester past = new Semester(2025, "Sem 1");
        past.addCourse(completedCourse("PROG1", "Programming I", "Programming", 3, true, 92.0));
        past.addCourse(completedCourse("PROG2", "Programming II", "Programming", 3, true, 88.0));
        past.addCourse(completedCourse("LANG1", "Writing", "Language", 3, false, 55.0));
        past.addCourse(completedCourse("LANG2", "Speaking", "Language", 3, false, 58.0));
        student.addSemester(past);

        Course programming = new Course("PROG3", "Algorithms", 3);
        programming.setCategory("Programming");
        programming.addAssessment(new Assessment("Midterm", "Exam", 50.0, 100.0));
        programming.getAssessments().get(0).setScore(75.0);
        programming.addAssessment(new Assessment("Final", "Exam", 50.0, 100.0));

        Course language = new Course("LANG3", "Academic Writing", 3);
        language.setCategory("Language");
        language.addAssessment(new Assessment("Draft", "Assignment", 50.0, 100.0));
        language.getAssessments().get(0).setScore(75.0);
        language.addAssessment(new Assessment("Final", "Exam", 50.0, 100.0));

        CoursePredictionResult strong = service.predictCourse(student, programming, 80.0, PredictionMode.DRAFT);
        CoursePredictionResult weak = service.predictCourse(student, language, 80.0, PredictionMode.DRAFT);

        assertTrue(strong.profileExpectedScore() > weak.profileExpectedScore());
        assertTrue(weak.riskExplanation().contains("above") || weak.riskExplanation().contains("aligned"));
    }

    @Test
    void majorOnlyTargetWorksWhenCumulativeInputIsEmpty() {
        Student student = new Student("Major Student", "ST005");
        Semester semester = new Semester(2026, "Sem 1");
        semester.addCourse(completedCourse("MAJ", "Major Course", "Major", 3, true, 80.0));
        student.addSemester(semester);
        student.setMajorRequiredCredits(3);

        DashboardTargetService.TargetCalculationResult result = new DashboardTargetService(service)
                .calculate(student, "", "3.5");

        assertTrue(result.hasAnyTarget());
        assertTrue(result.output().contains("MAJOR GPA TARGET"));
        assertFalse(result.output().contains("Please enter"));
    }

    @Test
    void courseTargetUpdatePersistsOnlyAfterValidTarget() throws Exception {
        Course course = new Course("JAVA", "Java", 3);
        AtomicBoolean saved = new AtomicBoolean(false);

        CourseTargetService.updateTargetScore(course, 85.0, () -> saved.set(true));

        assertEquals(85.0, course.getTargetScore(), 0.001);
        assertTrue(saved.get());
        assertThrows(IllegalArgumentException.class,
                () -> CourseTargetService.updateTargetScore(course, Double.NaN, () -> saved.set(false)));
        assertEquals(85.0, course.getTargetScore(), 0.001);
    }

    @Test
    void courseTargetUpdateRevertsWhenSaveFails() throws Exception {
        Course course = new Course("JAVA", "Java", 3);
        course.setTargetScore(75.0);

        assertThrows(java.io.IOException.class,
                () -> CourseTargetService.updateTargetScore(course, 90.0, () -> {
                    throw new java.io.IOException("disk full");
                }));

        assertEquals(75.0, course.getTargetScore(), 0.001);
    }

    @Test
    void invalidImportDoesNotOverwriteExistingSave(@TempDir Path tempDir) throws Exception {
        String oldDataFile = System.getProperty("academic.tracker.data.file");
        Path savePath = tempDir.resolve("student.json");
        System.setProperty("academic.tracker.data.file", savePath.toString());
        try {
            Student current = new Student("Current", "OK");
            Semester semester = new Semester(2026, "Sem 1");
            semester.addCourse(completedCourse("SAFE", "Safe Course", "General", 3, false, 80.0));
            current.addSemester(semester);
            DataManager.saveStudent(current);

            Path invalidImport = tempDir.resolve("invalid.json");
            Files.writeString(invalidImport, """
                    {
                      "fullName": "Bad",
                      "studentId": "BAD",
                      "semesters": [{
                        "year": 2026,
                        "termType": "Sem 1",
                        "courses": [{
                          "courseCode": "BAD",
                          "courseName": "Bad Course",
                          "category": "General",
                          "majorCourse": true,
                          "credits": 3,
                          "status": "COMPLETED",
                          "assessments": [{
                            "assessmentName": "Final",
                            "category": "Exam",
                            "weight": 100.0,
                            "score": NaN,
                            "maxScore": 100.0
                          }]
                        }]
                      }]
                    }
                    """);

            assertThrows(Exception.class, () -> DataManager.importStudent(invalidImport));
            Student loaded = DataManager.loadStudent();

            assertEquals("Current", loaded.getFullName());
        } finally {
            if (oldDataFile == null) {
                System.clearProperty("academic.tracker.data.file");
            } else {
                System.setProperty("academic.tracker.data.file", oldDataFile);
            }
        }
    }

    @Test
    void importRejectsZeroCreditCourses(@TempDir Path tempDir) throws Exception {
        Path invalidImport = tempDir.resolve("zero-credit.json");
        Files.writeString(invalidImport, """
                {
                  "fullName": "Bad",
                  "studentId": "ZERO",
                  "semesters": [{
                    "year": 2026,
                    "termType": "Sem 1",
                    "courses": [{
                      "courseCode": "ZERO",
                      "courseName": "Zero Credit",
                      "category": "General",
                      "majorCourse": false,
                      "credits": 0,
                      "status": "ACTIVE",
                      "assessments": []
                    }]
                  }]
                }
                """);

        assertThrows(Exception.class, () -> DataManager.previewImport(invalidImport));
    }

    private Course completedCourse(String code, String name, String category, int credits, boolean major, double score) {
        Course course = new Course(code, name, credits);
        course.setCategory(category);
        course.setMajorCourse(major);
        course.setStatus(CourseStatus.COMPLETED);
        course.addAssessment(new Assessment("Final", "Exam", 100.0, 100.0));
        course.getAssessments().get(0).setScore(score);
        return course;
    }
}
