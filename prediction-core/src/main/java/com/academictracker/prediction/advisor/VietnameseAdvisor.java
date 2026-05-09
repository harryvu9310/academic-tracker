package com.academictracker.prediction.advisor;

import com.academictracker.prediction.model.Course;
import com.academictracker.prediction.result.CoursePredictionResult;
import com.academictracker.prediction.result.GlobalPredictionResult;

import java.util.List;
import java.util.stream.Collectors;

public class VietnameseAdvisor {
    public String courseExplanation(Course course, CoursePredictionResult result) {
        if (!result.valid()) {
            return "Chưa thể dự đoán vì cấu trúc điểm của môn đang có lỗi: " + String.join("; ", result.errors());
        }
        if (result.missingWeight() <= 0.001 && result.unassignedWeight() <= 0.001) {
            return "Final course result is complete. Điểm cuối cùng là "
                    + String.format("%.1f", result.projectedFinalScore())
                    + ", tương ứng " + result.projectedGrade().letterGrade()
                    + " / " + String.format("%.1f", result.projectedGrade().scale4()) + ".";
        }
        if (Double.isFinite(result.requiredScoreForTarget()) && result.requiredScoreForTarget() < 0.0) {
            return "Current entered assessments are above target pace. Because remaining assessments still affect the final score, this is not certain yet.";
        }
        if (result.requiredScoreForTarget() > 100.0) {
            return "Target score này không khả thi với phần điểm còn lại. Max possible hiện tại là "
                    + String.format("%.1f", result.currentWeightedScore() + (result.missingWeight() + result.unassignedWeight()) * 100.0)
                    + ".";
        }
        if (Double.isFinite(result.requiredScoreForPass()) && result.requiredScoreForPass() <= 0.0) {
            return "Mốc qua môn đã an toàn về mặt toán học ngay cả nếu phần còn lại bằng 0. Vẫn nên duy trì nhịp học để bảo toàn GPA.";
        }
        return "Bạn nên nhắm khoảng " + String.format("%.1f", result.recommendedTargetScore())
                + " cho môn " + course.getCourseName()
                + ". Mốc này tương ứng " + result.recommendedTargetGrade().letterGrade()
                + " và giúp kiểm soát rủi ro qua môn ở mức " + result.passRiskLevel()
                + ". " + result.riskExplanation()
                + " Độ tin cậy profile: " + result.profileConfidence() + ".";
    }

    public String globalAdvice(GlobalPredictionResult result) {
        if (!result.targetProvided()) {
            return "Chưa có target GPA. Module đã tính official, projected và scenario GPA để app hiển thị bối cảnh hiện tại.";
        }
        if (!result.targetFeasible()) {
            return "Target " + String.format("%.2f", result.targetGPA())
                    + " không khả thi trong mô hình hiện tại. Highest achievable GPA là "
                    + String.format("%.2f", result.maxAchievableGPA())
                    + ". Suggested target: "
                    + String.format("%.2f", result.suggestedRealisticTarget()) + ".";
        }
        String plan = result.feasibleBandPlan().stream()
                .filter(item -> item.minimumScore100() > 0.0)
                .limit(4)
                .map(item -> item.courseName() + " đạt " + item.recommendedLetter()
                        + " (>= " + String.format("%.0f", item.minimumScore100()) + ")")
                .collect(Collectors.joining(", "));
        if (plan.isBlank()) {
            return "Bạn đang vượt mức target dựa trên điểm đã nhập. Tuy nhiên, kết quả cuối cùng vẫn có thể thay đổi nếu các assessment còn lại thấp.";
        }
        return "Target này mathematically feasible trong mô hình hiện tại, với giả định các môn còn lại đạt các band được đề xuất: " + plan + ".";
    }

    public List<String> priorityMessages(List<CoursePredictionResult> predictions) {
        return predictions.stream()
                .map(item -> item.courseName() + ": " + item.priorityReason())
                .toList();
    }
}
