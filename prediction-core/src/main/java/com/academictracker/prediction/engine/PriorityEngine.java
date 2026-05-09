package com.academictracker.prediction.engine;

import com.academictracker.prediction.core.GpaConverter;
import com.academictracker.prediction.model.Course;
import com.academictracker.prediction.model.PassRiskLevel;
import com.academictracker.prediction.model.PriorityLevel;
import com.academictracker.prediction.model.StrengthLevel;
import com.academictracker.prediction.result.CoursePredictionResult;
import com.academictracker.prediction.result.GpaResult;

import java.util.Comparator;
import java.util.List;

public class PriorityEngine {
    private final GpaConverter gpaConverter = new GpaConverter();

    public PriorityRecommendation evaluate(
            Course course,
            PassRiskLevel passRiskLevel,
            double projectedScore,
            double recommendedTarget,
            GpaResult projectedGrade,
            StrengthLevel strengthLevel
    ) {
        if (passRiskLevel == PassRiskLevel.CRITICAL || passRiskLevel == PassRiskLevel.RISKY) {
            return new PriorityRecommendation(PriorityLevel.CRITICAL, "Môn có rủi ro không qua, cần ưu tiên trước.");
        }
        if (Double.isFinite(recommendedTarget) && projectedScore >= recommendedTarget + 0.0001) {
            return new PriorityRecommendation(PriorityLevel.LOW, "Môn đang vượt target hiện tại, chỉ cần duy trì nhịp học.");
        }
        boolean highCredit = course != null && course.getCredits() >= 3;
        boolean nearNextBand = gpaConverter.nextHigherBand(projectedScore)
                .map(next -> next.minInclusive() - projectedScore <= 5.0)
                .orElse(false);
        if (highCredit && nearNextBand) {
            return new PriorityRecommendation(PriorityLevel.HIGH, "Môn nhiều tín chỉ và đang gần mốc điểm cao hơn.");
        }
        if (highCredit && strengthLevel == StrengthLevel.STRONG) {
            return new PriorityRecommendation(PriorityLevel.HIGH, "Môn nhiều tín chỉ và thuộc nhóm thế mạnh, có thể kéo GPA tốt.");
        }
        if (projectedGrade != null && !projectedGrade.passing()) {
            return new PriorityRecommendation(PriorityLevel.CRITICAL, "Projected score đang dưới ngưỡng qua môn.");
        }
        if (course != null && course.getCredits() <= 2 && strengthLevel == StrengthLevel.WEAK) {
            return new PriorityRecommendation(PriorityLevel.MEDIUM, "Môn yếu nhưng ít tín chỉ, ưu tiên target an toàn trước.");
        }
        return new PriorityRecommendation(PriorityLevel.MEDIUM, "Môn còn ảnh hưởng đến GPA và nên được theo dõi.");
    }

    public List<CoursePredictionResult> rankCourses(List<CoursePredictionResult> results) {
        return results.stream()
                .sorted(Comparator.comparingInt(item -> priorityRank(item.priorityLevel())))
                .toList();
    }

    private int priorityRank(PriorityLevel level) {
        return switch (level) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    public record PriorityRecommendation(PriorityLevel priorityLevel, String reasonVi) {
    }
}
