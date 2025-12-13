package pt.up.edscrum.dto.dashboard;

import java.time.LocalDate;
import java.util.List;

public class ProjectDetailsDTO {

    private Long id;
    private String name;
    private String description;
    private String courseName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;

    // Dados da Equipa Principal
    private Long teamId;
    private String teamName;
    private int teamTotalXP;
    private List<TeamMemberDTO> members;
    private List<TeamAwardDTO> teamAwards;

    private Long courseId;
    private List<AvailableTeamDTO> availableTeams;

    private List<SprintDTO> sprints;

    // Progresso do projeto
    private int totalStories;
    private int completedStories;
    private int progressPercentage;

    // Classe interna para detalhes dos membros
    public static class TeamMemberDTO {

        public Long id;
        public String name;
        public String role; // SM, PO, Dev
        public String userRole; // TEACHER or STUDENT
        public int individualXP;
        public int awardsCount; // Quantidade de prémios individuais

        public TeamMemberDTO(Long id, String name, String role, String userRole, int individualXP, int awardsCount) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.userRole = userRole;
            this.individualXP = individualXP;
            this.awardsCount = awardsCount;
        }
    }
    
    // Classe interna para sprints simples
    public static class SprintDTO {
        public Long id;
        public String name;
        public String status;
        public LocalDate startDate;
        public LocalDate endDate;
        
        public SprintDTO(Long id, String name, String status, LocalDate startDate, LocalDate endDate) {
            this.id = id;
            this.name = name;
            this.status = status;
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }
    
    // Classe interna para awards da equipa
    public static class TeamAwardDTO {
        public String awardName;
        public int pointsEarned;
        
        public TeamAwardDTO(String awardName, int pointsEarned) {
            this.awardName = awardName;
            this.pointsEarned = pointsEarned;
        }
    }
    
    // Classe interna para equipas disponíveis
    public static class AvailableTeamDTO {
        public Long id;
        public String name;
        
        public AvailableTeamDTO(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public ProjectDetailsDTO() {
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public int getTeamTotalXP() {
        return teamTotalXP;
    }

    public void setTeamTotalXP(int teamTotalXP) {
        this.teamTotalXP = teamTotalXP;
    }

    public List<TeamMemberDTO> getMembers() {
        return members;
    }

    public void setMembers(List<TeamMemberDTO> members) {
        this.members = members;
    }

    public List<TeamAwardDTO> getTeamAwards() {
        return teamAwards;
    }

    public void setTeamAwards(List<TeamAwardDTO> teamAwards) {
        this.teamAwards = teamAwards;
    }

    public List<SprintDTO> getSprints() {
        return sprints;
    }

    public void setSprints(List<SprintDTO> sprints) {
        this.sprints = sprints;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public List<AvailableTeamDTO> getAvailableTeams() {
        return availableTeams;
    }

    public void setAvailableTeams(List<AvailableTeamDTO> availableTeams) {
        this.availableTeams = availableTeams;
    }

    public int getTotalStories() {
        return totalStories;
    }

    public void setTotalStories(int totalStories) {
        this.totalStories = totalStories;
    }

    public int getCompletedStories() {
        return completedStories;
    }

    public void setCompletedStories(int completedStories) {
        this.completedStories = completedStories;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
}
