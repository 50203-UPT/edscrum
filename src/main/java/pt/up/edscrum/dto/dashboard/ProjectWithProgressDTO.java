package pt.up.edscrum.dto.dashboard;

import java.time.LocalDate;
import java.util.List;

import pt.up.edscrum.enums.ProjectStatus;

/**
 * DTO que representa um projeto com informação de progresso e sprints para
 * exibição no dashboard.
 */
public class ProjectWithProgressDTO {
    private Long id;
    private String name;
    private String sprintGoals;
    private LocalDate startDate;
    private LocalDate endDate;
    private ProjectStatus status;
    private int progress;
    private List<SprintWithProgressDTO> sprints;
    private List<MemberWithRoleDTO> members;

    /** Informações do curso para exibição no dashboard */
    private Long courseId;
    private String courseName;
    /** Getters e setters */
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

    public List<MemberWithRoleDTO> getMembers() {
        return members;
    }

    public void setMembers(List<MemberWithRoleDTO> members) {
        this.members = members;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
}
