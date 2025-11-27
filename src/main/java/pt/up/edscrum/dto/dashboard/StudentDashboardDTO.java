package pt.up.edscrum.dto.dashboard;

import java.util.List;

public class StudentDashboardDTO {

    private Long userId;
    private String name;
    private Long courseId;
    private int totalPoints;

    private List<AwardStatsDTO> awards;
    private List<PointHistoryDTO> pointHistory;

    public StudentDashboardDTO() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public List<AwardStatsDTO> getAwards() {
        return awards;
    }

    public void setAwards(List<AwardStatsDTO> awards) {
        this.awards = awards;
    }

    public List<PointHistoryDTO> getPointHistory() {
        return pointHistory;
    }

    public void setPointHistory(List<PointHistoryDTO> pointHistory) {
        this.pointHistory = pointHistory;
    }
}
