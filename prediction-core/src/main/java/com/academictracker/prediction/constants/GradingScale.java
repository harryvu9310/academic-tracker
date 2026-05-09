package com.academictracker.prediction.constants;

import com.academictracker.prediction.model.GradeBand;

import java.util.List;

public final class GradingScale {
    private GradingScale() {
    }

    public static final double PASSING_SCORE = 50.0;
    public static final double MAX_SCORE = 100.0;
    public static final double MIN_SCORE = 0.0;
    public static final double WEIGHT_TOLERANCE = 0.001;

    public static List<GradeBand> defaultBands() {
        return List.of(
                new GradeBand(90.0, 100.0000001, "A+", 4.0, "Excellent", true),
                new GradeBand(80.0, 90.0, "A", 3.5, "Very good", true),
                new GradeBand(70.0, 80.0, "B+", 3.0, "Good", true),
                new GradeBand(60.0, 70.0, "B", 2.5, "Fair", true),
                new GradeBand(50.0, 60.0, "C", 2.0, "Average", true),
                new GradeBand(40.0, 50.0, "D+", 1.5, "Weak", false),
                new GradeBand(30.0, 40.0, "D", 1.0, "Very weak", false),
                new GradeBand(0.0, 30.0, "F", 0.0, "No passing", false)
        );
    }
}
