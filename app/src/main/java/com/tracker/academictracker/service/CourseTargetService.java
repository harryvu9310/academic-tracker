package com.tracker.academictracker.service;

import com.tracker.academictracker.model.Course;

import java.io.IOException;

public final class CourseTargetService {
    private CourseTargetService() {
    }

    public static void updateTargetScore(Course course, double targetScore, SaveAction saveAction) throws IOException {
        if (course == null) {
            throw new IllegalArgumentException("Course is required.");
        }
        if (!Double.isFinite(targetScore) || targetScore < 0.0 || targetScore > 100.0) {
            throw new IllegalArgumentException("Target score must be a finite number between 0 and 100.");
        }
        Double previousTarget = course.getTargetScore();
        course.setTargetScore(targetScore);
        if (saveAction != null) {
            try {
                saveAction.save();
            } catch (IOException e) {
                course.setTargetScore(previousTarget);
                throw e;
            }
        }
    }

    @FunctionalInterface
    public interface SaveAction {
        void save() throws IOException;
    }
}
