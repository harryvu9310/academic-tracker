package com.academictracker.prediction;

import com.academictracker.prediction.engine.CoursePredictionEngine;
import com.academictracker.prediction.engine.PriorityEngine;
import com.academictracker.prediction.model.Assessment;
import com.academictracker.prediction.model.Course;
import com.academictracker.prediction.model.CourseStatus;
import com.academictracker.prediction.model.PredictionMode;
import com.academictracker.prediction.model.PriorityLevel;
import com.academictracker.prediction.result.CoursePredictionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriorityEngineTest {
    private final CoursePredictionEngine courseEngine = new CoursePredictionEngine();
    private final PriorityEngine priorityEngine = new PriorityEngine();

    @Test
    void ranksFailRiskAsCritical() {
        Course course = course("Calculus", 4);
        course.addAssessment(new Assessment("Midterm", 0.8, 20.0));
        course.addAssessment(new Assessment("Final", 0.2, null));

        CoursePredictionResult result = courseEngine.predict(course, 50.0, PredictionMode.STRICT);

        assertEquals(PriorityLevel.CRITICAL, result.priorityLevel());
    }

    @Test
    void highCreditCourseNearNextBandIsHighPriority() {
        Course course = course("Algorithms", 4);
        course.addAssessment(new Assessment("All Scores", 1.0, 79.0));

        CoursePredictionResult result = courseEngine.predict(course, 80.0, PredictionMode.DRAFT);

        assertEquals(PriorityLevel.HIGH, result.priorityLevel());
    }

    @Test
    void rankCoursesKeepsCriticalBeforeHigh() {
        Course risky = course("Risky", 4);
        risky.addAssessment(new Assessment("Midterm", 0.8, 20.0));
        risky.addAssessment(new Assessment("Final", 0.2, null));
        Course high = course("High", 4);
        high.addAssessment(new Assessment("All Scores", 1.0, 79.0));

        List<CoursePredictionResult> ranked = priorityEngine.rankCourses(List.of(
                courseEngine.predict(high, 80.0, PredictionMode.DRAFT),
                courseEngine.predict(risky, 50.0, PredictionMode.STRICT)
        ));

        assertTrue(ranked.getFirst().priorityLevel() == PriorityLevel.CRITICAL);
    }

    private Course course(String name, int credits) {
        return new Course(name.toLowerCase(), name, "General", credits, true, CourseStatus.IN_PROGRESS);
    }
}
