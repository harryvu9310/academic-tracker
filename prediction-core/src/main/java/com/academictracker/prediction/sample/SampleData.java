package com.academictracker.prediction.sample;

import com.academictracker.prediction.model.Assessment;
import com.academictracker.prediction.model.Course;
import com.academictracker.prediction.model.CourseStatus;
import com.academictracker.prediction.model.PredictionContext;
import com.academictracker.prediction.model.ProgramConfig;
import com.academictracker.prediction.model.StudentPerformanceHistory;

import java.util.List;

public final class SampleData {
    private SampleData() {
    }

    public static PredictionContext demoContext() {
        Course statistics = new Course("stats", "Statistics", "Quantitative", 3, true, CourseStatus.IN_PROGRESS);
        statistics.addAssessment(new Assessment("Midterm", 0.4, 82.0));
        statistics.addAssessment(new Assessment("Project", 0.2, null));
        statistics.addAssessment(new Assessment("Final", 0.4, null));

        Course java = new Course("java", "Java Programming", "Programming", 4, true, CourseStatus.PLANNED);

        Course english = new Course("eng", "Academic English", "Language", 2, false, CourseStatus.COMPLETED);
        english.addAssessment(new Assessment("Final", 1.0, 72.0));

        ProgramConfig config = new ProgramConfig();
        config.setDegreeTotalCredits(126);
        config.setMajorRequiredCredits(72);

        PredictionContext context = new PredictionContext();
        context.setProgramConfig(config);
        context.setCourses(List.of(statistics, java, english));
        context.setStudentPerformanceHistory(List.of(
                new StudentPerformanceHistory("Quantitative", List.of(84.0, 88.0, 86.0), null),
                new StudentPerformanceHistory("Programming", List.of(78.0, 82.0), null),
                new StudentPerformanceHistory("Language", List.of(70.0, 72.0), null)
        ));
        context.setTargetCumulativeGPA(3.2);
        context.setTargetMajorGPA(3.3);
        return context;
    }
}
