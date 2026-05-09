package com.tracker.academictracker.service;

public final class InputValidator {
    private InputValidator() {
    }

    public static double parseFiniteDouble(String rawValue, String label) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        double parsed;
        try {
            parsed = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a valid number.", e);
        }
        if (!Double.isFinite(parsed)) {
            throw new IllegalArgumentException(label + " must be a finite number.");
        }
        return parsed;
    }

    public static double parseScore100(String rawValue, String label) {
        double parsed = parseFiniteDouble(rawValue, label);
        if (parsed < 0.0 || parsed > 100.0) {
            throw new IllegalArgumentException(label + " must be between 0 and 100.");
        }
        return parsed;
    }

    public static double parseScoreAgainstMax(String rawValue, String label, double maxScore) {
        if (!Double.isFinite(maxScore) || maxScore <= 0.0) {
            throw new IllegalArgumentException("Max score must be a finite number greater than 0.");
        }
        double parsed = parseFiniteDouble(rawValue, label);
        if (parsed < 0.0 || parsed > maxScore) {
            throw new IllegalArgumentException(label + " must be between 0 and " + maxScore + ".");
        }
        return parsed;
    }

    public static double parseWeightPercent(String rawValue) {
        double parsed = parseFiniteDouble(rawValue, "Weight");
        if (parsed <= 0.0 || parsed > 100.0) {
            throw new IllegalArgumentException("Weight must be greater than 0 and at most 100%.");
        }
        return parsed;
    }

    public static double parseGpaTarget(String rawValue, String label) {
        double parsed = parseFiniteDouble(rawValue, label);
        if (parsed < 0.0 || parsed > 4.0) {
            throw new IllegalArgumentException(label + " must be between 0.0 and 4.0.");
        }
        return parsed;
    }

    public static Integer parseOptionalPositiveInt(String rawValue, String label) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) {
            return null;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a whole number.", e);
        }
        if (parsed <= 0) {
            throw new IllegalArgumentException(label + " must be greater than 0.");
        }
        return parsed;
    }

    public static int parsePositiveInt(String rawValue, String label) {
        Integer parsed = parseOptionalPositiveInt(rawValue, label);
        if (parsed == null) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return parsed;
    }
}
