package pt.up.edscrum.dto.dashboard;

import java.util.List;
import pt.up.edscrum.model.Sprint;
import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.TeamAward;

public class ProjectDetailsDTO {
    private Long id;
    private String name;
    private String description; // Vem de sprintGoals do projeto
    
    // Dados da Equipa
    private Team team;
    
    // Dados de Progresso
    private int totalSprints;
    private int globalScore; // Média ou soma
    private int totalAwards;
    
    // Progresso de Tarefas (Simulado para o visual)
    private int completedTasks;
    private int totalTasks;
    private int progressPercentage;

    // Listas
    private List<Sprint> sprints;
    private List<TeamAward> awards;

    // Construtor vazio
    public ProjectDetailsDTO() {}

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public int getTotalSprints() { return totalSprints; }
    public void setTotalSprints(int totalSprints) { this.totalSprints = totalSprints; }

    public int getGlobalScore() { return globalScore; }
    public void setGlobalScore(int globalScore) { this.globalScore = globalScore; }

    public int getTotalAwards() { return totalAwards; }
    public void setTotalAwards(int totalAwards) { this.totalAwards = totalAwards; }

    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }

    public int getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }

    public List<Sprint> getSprints() { return sprints; }
    public void setSprints(List<Sprint> sprints) { this.sprints = sprints; }
    
    public List<TeamAward> getAwards() { return awards; }
    public void setAwards(List<TeamAward> awards) { this.awards = awards; }
}