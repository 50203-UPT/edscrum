package pt.up.edscrum.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.Team; // Importar
import pt.up.edscrum.repository.ProjectRepository;
import pt.up.edscrum.repository.TeamRepository; // Importar

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository; // NOVO

    // Injeção via construtor (Adicionar TeamRepository)
    public ProjectService(ProjectRepository projectRepository, TeamRepository teamRepository) {
        this.projectRepository = projectRepository;
        this.teamRepository = teamRepository;
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    public Project createProject(Project project) {
        return projectRepository.save(project);
    }

    public Project updateProject(Long id, Project projectDetails) {
        Project project = getProjectById(id);
        project.setName(projectDetails.getName());
        project.setSprintGoals(projectDetails.getSprintGoals());
        project.setCourse(projectDetails.getCourse());
        // Se quiseres atualizar datas aqui também, podes adicionar
        project.setStartDate(projectDetails.getStartDate());
        project.setEndDate(projectDetails.getEndDate());
        return projectRepository.save(project);
    }

    // MÉTODO APAGAR
    public void deleteProject(Long id) {
        Project project = getProjectById(id);

        // 1. Desassociar equipas antes de apagar
        if (project.getTeams() != null) {
            for (Team team : project.getTeams()) {
                team.setProject(null);
                teamRepository.save(team);
            }
        }

        // 2. Apagar o projeto (agora seguro)
        projectRepository.deleteById(id);
    }

    // Completar Projeto (apenas se todos os sprints estiverem concluídos)
    public void completeProject(Long projectId) {
        Project project = getProjectById(projectId);
        
        // Verificar se todos os sprints estão concluídos
        if (project.getSprints() != null && !project.getSprints().isEmpty()) {
            boolean allSprintsDone = project.getSprints().stream()
                .allMatch(sprint -> sprint.getStatus() == pt.up.edscrum.enums.SprintStatus.CONCLUIDO);
            
            if (!allSprintsDone) {
                throw new IllegalStateException("Todos os sprints devem estar concluídos antes de marcar o projeto como concluído.");
            }
        }
        
        project.setStatus(pt.up.edscrum.enums.ProjectStatus.CONCLUIDO);
        projectRepository.save(project);
    }

    // Reabrir Projeto
    public void reopenProject(Long projectId) {
        Project project = getProjectById(projectId);
        project.setStatus(pt.up.edscrum.enums.ProjectStatus.EM_CURSO);
        projectRepository.save(project);
    }

    // Remover equipa do projeto
    public void removeTeamFromProject(Long projectId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        
        // Verificar se a equipa está associada a este projeto
        if (team.getProject() != null && team.getProject().getId().equals(projectId)) {
            team.setProject(null);
            teamRepository.save(team);
        } else {
            throw new RuntimeException("Esta equipa não está associada a este projeto.");
        }
    }
}
