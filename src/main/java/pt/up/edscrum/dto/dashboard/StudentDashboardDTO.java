package pt.up.edscrum.dto.dashboard;

import java.util.List;

import pt.up.edscrum.model.Course;

public class StudentDashboardDTO {

    private Long id;
    private String name;
    private String email;
    private String profileImage;
    private boolean notificationAwards;
    private boolean notificationRankings;
    private String studentTag;

    // Dados do Curso Principal (onde o aluno está inscrito)
    private Long courseId;
    private String courseName;

    // Dados do Projeto e Equipa
    private Long projectId;
    private String teamName;
    private String roleInTeam;

    private int totalPoints;

    // --- NOVOS CAMPOS PARA OS CARTÕES DA VISÃO GERAL ---
    private int currentRank;            // A tua posição
    private int totalClassStudents;     // Total de alunos na turma
    private int totalClassTeams;        // Total de equipas na turma

    private double classAverage;        // Média da turma

    private String topPerformerName;    // Nome do melhor aluno
    private int topPerformerScore;      // Pontuação do melhor

    private int scoreVariation;         // Diferença (Melhor - Pior)

    // Listas de Dados
    private List<AwardDisplayDTO> earnedAwards;
    private List<AwardDisplayDTO> unearnedAwards;
    private List<PointHistoryDTO> pointHistory;
    private List<RankingDTO> topStudents; // Top 5 para a tabela

    private List<ProjectWithProgressDTO> projects;
    private List<Course> enrolledCourses;
    private List<Course> availableCourses;

    // Classe auxiliar para prémios
    public static class AwardDisplayDTO {

        public String name;
        public int points;
        public String description;
        public String type;

        public AwardDisplayDTO(String name, int points, String description, String type) {
            this.name = name;
            this.points = points;
            this.description = description;
            this.type = type;
        }
    }

    public StudentDashboardDTO() {
    }

    // --- GETTERS E SETTERS ---
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
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

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getRoleInTeam() {
        return roleInTeam;
    }

    public void setRoleInTeam(String roleInTeam) {
        this.roleInTeam = roleInTeam;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public boolean isNotificationAwards() { return notificationAwards; }
    public void setNotificationAwards(boolean notificationAwards) { this.notificationAwards = notificationAwards; }

    public boolean isNotificationRankings() {
         return notificationRankings; 
    }

    public void setNotificationRankings(boolean notificationRankings) {
         this.notificationRankings = notificationRankings; 
    }

    public String getStudentTag() {
        return studentTag;
    }

    public void setStudentTag(String studentTag) {
        this.studentTag = studentTag;
    }
    
    // Getters/Setters Estatísticas
    public int getCurrentRank() {
        return currentRank;
    }

    public void setCurrentRank(int currentRank) {
        this.currentRank = currentRank;
    }

    public int getTotalClassStudents() {
        return totalClassStudents;
    }

    public void setTotalClassStudents(int totalClassStudents) {
        this.totalClassStudents = totalClassStudents;
    }

    public int getTotalClassTeams() {
        return totalClassTeams;
    }

    public void setTotalClassTeams(int totalClassTeams) {
        this.totalClassTeams = totalClassTeams;
    }

    public double getClassAverage() {
        return classAverage;
    }

    public void setClassAverage(double classAverage) {
        this.classAverage = classAverage;
    }

    public String getTopPerformerName() {
        return topPerformerName;
    }

    public void setTopPerformerName(String topPerformerName) {
        this.topPerformerName = topPerformerName;
    }

    public int getTopPerformerScore() {
        return topPerformerScore;
    }

    public void setTopPerformerScore(int topPerformerScore) {
        this.topPerformerScore = topPerformerScore;
    }

    public int getScoreVariation() {
        return scoreVariation;
    }

    public void setScoreVariation(int scoreVariation) {
        this.scoreVariation = scoreVariation;
    }

    // Getters/Setters Listas
    public List<AwardDisplayDTO> getEarnedAwards() {
        return earnedAwards;
    }

    public void setEarnedAwards(List<AwardDisplayDTO> earnedAwards) {
        this.earnedAwards = earnedAwards;
    }

    public List<AwardDisplayDTO> getUnearnedAwards() {
        return unearnedAwards;
    }

    public void setUnearnedAwards(List<AwardDisplayDTO> unearnedAwards) {
        this.unearnedAwards = unearnedAwards;
    }

    public List<PointHistoryDTO> getPointHistory() {
        return pointHistory;
    }

    public void setPointHistory(List<PointHistoryDTO> pointHistory) {
        this.pointHistory = pointHistory;
    }

    public List<RankingDTO> getTopStudents() {
        return topStudents;
    }

    public void setTopStudents(List<RankingDTO> topStudents) {
        this.topStudents = topStudents;
    }

    public List<ProjectWithProgressDTO> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectWithProgressDTO> projects) {
        this.projects = projects;
    }

    public List<Course> getEnrolledCourses() {
        return enrolledCourses;
    }

    public void setEnrolledCourses(List<Course> enrolledCourses) {
        this.enrolledCourses = enrolledCourses;
    }

    public List<Course> getAvailableCourses() {
        return availableCourses;
    }

    public void setAvailableCourses(List<Course> availableCourses) {
        this.availableCourses = availableCourses;
    }
}
