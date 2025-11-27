package pt.up.edscrum.dto.dashboard;

import java.time.LocalDateTime;

public class PointHistoryDTO {

    private Long studentId;
    private LocalDateTime date;
    private int totalPoints;

    public PointHistoryDTO(Long studentId, int totalPoints) {
        this.studentId = studentId;
        this.totalPoints = totalPoints;
    }

    public PointHistoryDTO(Long studentId, LocalDateTime date, int totalPoints) {
        this.studentId = studentId;
        this.date = date;
        this.totalPoints = totalPoints;
    }

    public PointHistoryDTO() {
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }
}
