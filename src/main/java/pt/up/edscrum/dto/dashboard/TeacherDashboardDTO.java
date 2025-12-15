package pt.up.edscrum.dto.dashboard;

import java.util.List;

/**
 * DTO usado para compor o dashboard do professor com contagens, projetos e
 * estatísticas.
 */
public class TeacherDashboardDTO {

    private Long courseId;
    private String courseName;

    private int totalStudents;
    private int totalTeams;
    private int totalProjects;

    private List<ProjectProgressDTO> projects;
    private List<AwardStatsDTO> awardStats;

    public TeacherDashboardDTO() {
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

    public int getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(int totalStudents) {
        this.totalStudents = totalStudents;
    }

    public int getTotalTeams() {
        return totalTeams;
    }

    public void setTotalTeams(int totalTeams) {
        this.totalTeams = totalTeams;
    }

    public int getTotalProjects() {
        return totalProjects;
    }

    public void setTotalProjects(int totalProjects) {
        this.totalProjects = totalProjects;
    }

    public List<ProjectProgressDTO> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectProgressDTO> projects) {
        this.projects = projects;
    }

    public List<AwardStatsDTO> getAwardStats() {
        return awardStats;
    }

    public void setAwardStats(List<AwardStatsDTO> awardStats) {
        this.awardStats = awardStats;
    }
}
