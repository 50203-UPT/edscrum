package pt.up.edscrum.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pt.up.edscrum.model.Project;
import pt.up.edscrum.repository.ProjectRepository;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    // Injeção via construtor
    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // Listar todos os projetos
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    // Buscar projeto por ID
    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    // Criar novo projeto
    public Project createProject(Project project) {
        return projectRepository.save(project);
    }

    // Atualizar projeto
    public Project updateProject(Long id, Project projectDetails) {
        Project project = getProjectById(id);
        project.setName(projectDetails.getName());
        project.setSprintGoals(projectDetails.getSprintGoals());
        project.setCourse(projectDetails.getCourse());
        return projectRepository.save(project);
    }

    // Apagar projeto
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }
}
