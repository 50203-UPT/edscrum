package pt.up.edscrum.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pt.up.edscrum.model.Project;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
        // Validação das datas: se ambas as datas existirem, validar ordem e data de início > data atual
        if (project.getStartDate() != null && project.getEndDate() != null) {
            if (!project.getStartDate().isBefore(project.getEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de início deve ser anterior à data de fim");
            }
            if (!project.getStartDate().isAfter(java.time.LocalDate.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de início deve ser após a data atual");
            }
        }
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
        // Se ambas as datas são fornecidas, validamos também no update
        if (project.getStartDate() != null && project.getEndDate() != null) {
            if (!project.getStartDate().isBefore(project.getEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de início deve ser anterior à data de fim");
            }
            if (!project.getStartDate().isAfter(java.time.LocalDate.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de início deve ser após a data atual");
            }
        }
        return projectRepository.save(project);
    }

    // MÉTODO APAGAR
    public void deleteProject(Long id) {
        Project project = getProjectById(id);

        // 1. Desassociar equipas antes de apagar
        if (project.getTeams() != null) {
            for (Team team : project.getTeams()) {
                if (team.getProjects() != null) {
                    team.getProjects().remove(project);
                    teamRepository.save(team);
                }
            }
        }

        // 2. Apagar o projeto (agora seguro)
        projectRepository.deleteById(id);
    }
}
