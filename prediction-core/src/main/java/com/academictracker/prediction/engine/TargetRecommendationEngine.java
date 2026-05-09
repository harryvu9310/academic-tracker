package com.academictracker.prediction.engine;

import com.academictracker.prediction.core.GpaConverter;
import com.academictracker.prediction.model.Course;
import com.academictracker.prediction.model.StrengthLevel;
import com.academictracker.prediction.result.GpaResult;
import com.academictracker.prediction.util.MathUtils;

public class TargetRecommendationEngine {
    private final GpaConverter gpaConverter;

    public TargetRecommendationEngine() {
        this(new GpaConverter());
    }

    public TargetRecommendationEngine(GpaConverter gpaConverter) {
        this.gpaConverter = gpaConverter;
    }

    /** Produces a practical score target, not just the mathematical minimum. */
    public double recommendedTarget(Course course, double requiredScore, StrengthLevel strengthLevel) {
        double required = Double.isFinite(requiredScore) ? Math.max(0.0, requiredScore) : 100.0;
        double baseline;
        int credits = course == null ? 0 : course.getCredits();
        switch (strengthLevel) {
            case STRONG -> baseline = credits >= 3 ? 90.0 : 85.0;
            case GOOD -> baseline = credits >= 3 ? 80.0 : 75.0;
            case AVERAGE -> baseline = credits >= 3 ? 70.0 : 60.0;
            case WEAK -> baseline = credits >= 3 ? 60.0 : 50.0;
            default -> baseline = 70.0;
        }
        return MathUtils.clamp(Math.max(required, baseline), 0.0, 100.0);
    }

    public GpaResult recommendedGrade(Course course, double requiredScore, StrengthLevel strengthLevel) {
        return gpaConverter.fromScore(recommendedTarget(course, requiredScore, strengthLevel));
    }
}
