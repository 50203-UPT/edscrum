package pt.up.edscrum.dto.dashboard;

import java.time.LocalDateTime;

public class StudentAwardDTO {

    private String awardName;
    private int points;
    private LocalDateTime dateAssigned;

    public StudentAwardDTO(String awardName, int points, LocalDateTime dateAssigned) {
        this.awardName = awardName;
        this.points = points;
        this.dateAssigned = dateAssigned;
    }

    public StudentAwardDTO() {
    }

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
