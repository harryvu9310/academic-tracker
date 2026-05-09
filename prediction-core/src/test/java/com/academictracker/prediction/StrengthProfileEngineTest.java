package com.academictracker.prediction;

import com.academictracker.prediction.engine.CoursePredictionEngine;
import com.academictracker.prediction.engine.StrengthProfileEngine;
import com.academictracker.prediction.model.Assessment;
import com.academictracker.prediction.model.ConfidenceLevel;
import com.academictracker.prediction.model.Course;
import com.academictracker.prediction.model.CourseStatus;
import com.academictracker.prediction.model.PredictionMode;
import com.academictracker.prediction.model.StudentPerformanceHistory;
import com.academictracker.prediction.result.CoursePredictionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrengthProfileEngineTest {
    private final StrengthProfileEngine profileEngine = new StrengthProfileEngine();

    @Test
    void strongCategoryProducesHigherExpectedScoreThanWeakCategory() {
        CoursePredictionEngine engine = new CoursePredictionEngine();
        Course strongCourse = course("Algorithms", "Programming");
        Course weakCourse = course("Writing", "Language");
        List<StudentPerformanceHistory> histories = List.of(
                new StudentPerformanceHistory("Programming", List.of(90.0, 92.0, 88.0), null),
                new StudentPerformanceHistory("Language", List.of(52.0, 58.0, 61.0), null)
        );

        CoursePredictionResult strong = engine.predict(strongCourse, 80.0, PredictionMode.DRAFT, histories, null);
        CoursePredictionResult weak = engine.predict(weakCourse, 80.0, PredictionMode.DRAFT, histories, null);

        assertTrue(strong.profileExpectedScore() > weak.profileExpectedScore());
        assertTrue(strong.recommendedTargetScore() >= weak.recommendedTargetScore());
    }

    @Test
    void unknownCategoryFallsBackToGlobalProfileWithUncertainty() {
        StrengthProfileEngine.StrengthProfile profile = profileEngine.profileForCategory(
                List.of(new StudentPerformanceHistory("Programming", List.of(82.0, 84.0, 86.0), null)),
                "Quantitative"
        );

        assertEquals(ConfidenceLevel.UNKNOWN, profile.confidenceLevel());
        assertTrue(profile.averageScore() > 80.0);
        assertTrue(profile.explanation().contains("global"));
    }

    @Test
    void noHistoryUsesCautiousDefault() {
        StrengthProfileEngine.StrengthProfile profile = profileEngine.profileForCategory(List.of(), "General");

        assertEquals(ConfidenceLevel.UNKNOWN, profile.confidenceLevel());
        assertEquals(70.0, profile.averageScore(), 0.001);
    }

    private Course course(String name, String category) {
        Course course = new Course(name.toLowerCase(), name, category, 3, true, CourseStatus.IN_PROGRESS);
        course.addAssessment(new Assessment("Current", 0.5, 75.0));
        course.addAssessment(new Assessment("Final", 0.5, null));
        return course;
    }
}
