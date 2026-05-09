package com.academictracker.prediction.util;

import java.util.Collection;

public final class MathUtils {
    private MathUtils() {
    }

    public static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public static double average(Collection<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        int count = 0;
        for (Double value : values) {
            if (value != null && Double.isFinite(value)) {
                total += value;
                count++;
            }
        }
        return count == 0 ? 0.0 : total / count;
    }

    public static double standardDeviation(Collection<Double> values) {
        if (values == null || values.size() < 2) {
            return 0.0;
        }
        double average = average(values);
        double total = 0.0;
        int count = 0;
        for (Double value : values) {
            if (value != null && Double.isFinite(value)) {
                double delta = value - average;
                total += delta * delta;
                count++;
            }
        }
        return count < 2 ? 0.0 : Math.sqrt(total / count);
    }
}
