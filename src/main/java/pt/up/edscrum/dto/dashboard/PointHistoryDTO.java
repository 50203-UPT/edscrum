package pt.up.edscrum.dto.dashboard;

import java.time.LocalDateTime;

public class PointHistoryDTO {
    private LocalDateTime date;
    private int totalPoints;

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
}