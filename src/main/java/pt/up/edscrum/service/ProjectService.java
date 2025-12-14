package pt.up.edscrum.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.ProjectRepository;
import pt.up.edscrum.repository.TeamRepository;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final pt.up.edscrum.service.AwardService awardService;

    public ProjectService(ProjectRepository projectRepository, TeamRepository teamRepository, pt.up.edscrum.service.AwardService awardService) {
        this.projectRepository = projectRepository;
        this.teamRepository = teamRepository;
        this.awardService = awardService;
    }

    /**
     * Obtém todos os projetos existentes.
     *
     * @return Lista de Project
     */
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    /**
     * Obtém um projeto pelo seu ID.
     *
     * @param id ID do projeto
     * @return Project encontrado
     */
    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    /**
     * Cria e persiste um novo projeto.
     *
     * @param project Dados do projeto a criar
     * @return Project criado
     */
    public Project createProject(Project project) {
        return projectRepository.save(project);
    }

    /**
     * Atualiza os dados de um projeto existente.
     *
     * @param id ID do projeto a atualizar
     * @param projectDetails Dados atualizados
     * @return Project atualizado
     */
    public Project updateProject(Long id, Project projectDetails) {
        Project project = getProjectById(id);
        project.setName(projectDetails.getName());
        project.setSprintGoals(projectDetails.getSprintGoals());
        project.setCourse(projectDetails.getCourse());
        project.setStartDate(projectDetails.getStartDate());
        project.setEndDate(projectDetails.getEndDate());
        return projectRepository.save(project);
    }

    /**
     * Elimina um projeto, desassociando equipas antes de apagar.
     *
     * @param id ID do projeto a eliminar
     */
    public void deleteProject(Long id) {
        Project project = getProjectById(id);

        if (project.getTeams() != null) {
            for (Team team : project.getTeams()) {
                team.setProject(null);
                teamRepository.save(team);
            }
        }

        projectRepository.deleteById(id);
    }

    /**
     * Marca um projeto como concluído, validando que todos os sprints
     * associados estão concluídos, e atribui prémios automáticos.
     *
     * @param projectId ID do projeto a concluir
     */
    public void completeProject(Long projectId) {
        Project project = getProjectById(projectId);

        if (project.getSprints() != null && !project.getSprints().isEmpty()) {
            boolean allSprintsDone = project.getSprints().stream()
                    .allMatch(sprint -> sprint.getStatus() == pt.up.edscrum.enums.SprintStatus.CONCLUIDO);

            if (!allSprintsDone) {
                throw new IllegalStateException("Todos os sprints devem estar concluídos antes de marcar o projeto como concluído.");
            }
        }

        project.setStatus(pt.up.edscrum.enums.ProjectStatus.CONCLUIDO);
        projectRepository.save(project);

        if (project.getTeams() != null) {
            for (Team team : project.getTeams()) {
                if (team.getScrumMaster() != null) {
                    awardService.assignAutomaticAwardToStudentByName("Conquistador de Projetos", "Concluíste o teu primeiro projeto.", 100, team.getScrumMaster().getId(), projectId);
                }
                if (team.getProductOwner() != null) {
                    awardService.assignAutomaticAwardToStudentByName("Conquistador de Projetos", "Concluíste o teu primeiro projeto.", 100, team.getProductOwner().getId(), projectId);
                }
                if (team.getDevelopers() != null) {
                    for (pt.up.edscrum.model.User dev : team.getDevelopers()) {
                        awardService.assignAutomaticAwardToStudentByName("Conquistador de Projetos", "Concluíste o teu primeiro projeto.", 100, dev.getId(), projectId);
                    }
                }

                try {
                    awardService.assignAutomaticAwardToTeamByName("Conquistadores de Projeto", "A equipa concluiu este projeto com sucesso.", 150, team.getId(), projectId);
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Reabre um projeto, definindo o estado para EM_CURSO.
     *
     * @param projectId ID do projeto a reabrir
     */
    public void reopenProject(Long projectId) {
        Project project = getProjectById(projectId);
        project.setStatus(pt.up.edscrum.enums.ProjectStatus.EM_CURSO);
        projectRepository.save(project);
    }

    public boolean isUserProductOwner(Long userId, Long projectId) {
        Project project = getProjectById(projectId);
        if (project.getTeams() == null) {
            return false;
        }

        User user = new User();
        user.setId(userId);

        for (Team team : project.getTeams()) {
            if (team.getProductOwner() != null && team.getProductOwner().equals(user)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Remove a associação de uma equipa a um projeto.
     *
     * @param projectId ID do projeto
     * @param teamId ID da equipa a remover
     */
    public void removeTeamFromProject(Long projectId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        if (team.getProject() != null && team.getProject().getId().equals(projectId)) {
            team.setProject(null);
            teamRepository.save(team);
        } else {
            throw new RuntimeException("Esta equipa não está associada a este projeto.");
        }
    }
}
