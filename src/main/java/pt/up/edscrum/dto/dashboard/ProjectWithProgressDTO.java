package pt.up.edscrum.dto.dashboard;

import java.time.LocalDate;
import java.util.List;

import pt.up.edscrum.enums.ProjectStatus;
import pt.up.edscrum.model.User;

public class ProjectWithProgressDTO {
    private Long id;
    private String name;
    private String sprintGoals;
    private LocalDate startDate;
    private LocalDate endDate;
    private ProjectStatus status;
    private int progress; // 0-100
    private List<SprintWithProgressDTO> sprints;
    private List<User> members;

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSprintGoals() {
        return sprintGoals;
    }

    public void setSprintGoals(String sprintGoals) {
        this.sprintGoals = sprintGoals;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public List<SprintWithProgressDTO> getSprints() {
        return sprints;
    }

    public void setSprints(List<SprintWithProgressDTO> sprints) {
        this.sprints = sprints;
    }

    public List<User> getMembers() {
        return members;
    }

    public void setMembers(List<User> members) {
        this.members = members;
    }
}
