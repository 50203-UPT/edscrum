package pt.up.edscrum.dto.dashboard;

public class ProjectProgressDTO {

    private Long projectId;
    private String projectName;
    private double completionPercentage;

    public ProjectProgressDTO(double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public ProjectProgressDTO(long projectId, String projectName, double completionPercentage) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.completionPercentage = completionPercentage;
    }

    public ProjectProgressDTO() {
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public double getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }
}
