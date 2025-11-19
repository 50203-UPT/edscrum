package pt.up.edscrum.dto.dashboard;

import java.time.LocalDateTime;

public class StudentAwardDTO {
    private String awardName;
    private int points;
    private LocalDateTime dateAssigned;

    public String getAwardName() {
        return awardName;
    }
    public void setAwardName(String awardName) {
        this.awardName = awardName;
    }
    public int getPoints() {
        return points;
    }
    public void setPoints(int points) {
        this.points = points;
    }
    public LocalDateTime getDateAssigned() {
        return dateAssigned;
    }
    public void setDateAssigned(LocalDateTime dateAssigned) {
        this.dateAssigned = dateAssigned;
    }
}