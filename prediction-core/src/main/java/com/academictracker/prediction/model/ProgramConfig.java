package com.academictracker.prediction.model;

import com.academictracker.prediction.constants.GradingScale;

import java.util.ArrayList;
import java.util.List;

public class ProgramConfig {
    private Integer degreeTotalCredits;
    private Integer majorRequiredCredits;
    private Integer completedCredits;
    private Integer inProgressCredits;
    private Integer plannedCredits;
    private List<GradeBand> gradingScale;

    public ProgramConfig() {
        this.gradingScale = GradingScale.defaultBands();
    }

    public Integer getDegreeTotalCredits() {
        return degreeTotalCredits;
    }

    public void setDegreeTotalCredits(Integer degreeTotalCredits) {
        if (degreeTotalCredits != null && degreeTotalCredits <= 0) {
            throw new IllegalArgumentException("degreeTotalCredits must be greater than 0.");
        }
        this.degreeTotalCredits = degreeTotalCredits;
    }

    public Integer getMajorRequiredCredits() {
        return majorRequiredCredits;
    }

    public void setMajorRequiredCredits(Integer majorRequiredCredits) {
        if (majorRequiredCredits != null && majorRequiredCredits <= 0) {
            throw new IllegalArgumentException("majorRequiredCredits must be greater than 0.");
        }
        this.majorRequiredCredits = majorRequiredCredits;
    }

    public Integer getCompletedCredits() {
        return completedCredits;
    }

    public void setCompletedCredits(Integer completedCredits) {
        if (completedCredits != null && completedCredits < 0) {
            throw new IllegalArgumentException("completedCredits cannot be negative.");
        }
        this.completedCredits = completedCredits;
    }

    public Integer getInProgressCredits() {
        return inProgressCredits;
    }

    public void setInProgressCredits(Integer inProgressCredits) {
        if (inProgressCredits != null && inProgressCredits < 0) {
            throw new IllegalArgumentException("inProgressCredits cannot be negative.");
        }
        this.inProgressCredits = inProgressCredits;
    }

    public Integer getPlannedCredits() {
        return plannedCredits;
    }

    public void setPlannedCredits(Integer plannedCredits) {
        if (plannedCredits != null && plannedCredits < 0) {
            throw new IllegalArgumentException("plannedCredits cannot be negative.");
        }
        this.plannedCredits = plannedCredits;
    }

    public List<GradeBand> getGradingScale() {
        if (gradingScale == null || gradingScale.isEmpty()) {
            gradingScale = GradingScale.defaultBands();
        }
        return gradingScale;
    }

    public void setGradingScale(List<GradeBand> gradingScale) {
        this.gradingScale = gradingScale == null ? GradingScale.defaultBands() : new ArrayList<>(gradingScale);
    }
}
