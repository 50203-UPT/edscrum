package pt.up.edscrum.dto.dashboard;

import java.time.LocalDate;
import java.util.List;

import pt.up.edscrum.model.Sprint;
import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.TeamAward;

public class ProjectDetailsDTO {

    private Long id;
    private String name;
    private String description;
    private String courseName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;

    // Dados da Equipa Principal
    private String teamName;
    private int teamTotalXP;
    private List<TeamMemberDTO> members;
    private List<TeamAward> teamAwards;

    private Long courseId;
    private List<Team> availableTeams;

    private List<Sprint> sprints;

    // Classe interna para detalhes dos membros
    public static class TeamMemberDTO {

        public Long id;
        public String name;
        public String role; // SM, PO, Dev
        public int individualXP;
        public int awardsCount; // Quantidade de prémios individuais

        public TeamMemberDTO(Long id, String name, String role, int individualXP, int awardsCount) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.individualXP = individualXP;
            this.awardsCount = awardsCount;
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

    public List<TeamAward> getTeamAwards() {
        return teamAwards;
    }

    public void setTeamAwards(List<TeamAward> teamAwards) {
        this.teamAwards = teamAwards;
    }

    public List<Sprint> getSprints() {
        return sprints;
    }

    public void setSprints(List<Sprint> sprints) {
        this.sprints = sprints;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public List<Team> getAvailableTeams() {
        return availableTeams;
    }

    public void setAvailableTeams(List<Team> availableTeams) {
        this.availableTeams = availableTeams;
    }
}
