package pt.up.edscrum.service;

import java.util.List;
import org.springframework.stereotype.Service;

import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.Sprint;
import pt.up.edscrum.repository.ProjectRepository;
import pt.up.edscrum.repository.SprintRepository;

@Service
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;

    public SprintService(SprintRepository sprintRepository, ProjectRepository projectRepository) {
        this.sprintRepository = sprintRepository;
        this.projectRepository = projectRepository;
    }

    public List<Sprint> getSprintsByProject(Long projectId) {
        return sprintRepository.findByProjectId(projectId);
    }

    public Sprint createSprint(Long projectId, Sprint sprint) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        sprint.setProject(project);
        return sprintRepository.save(sprint);
    }

    public Sprint updateSprint(Long sprintId, Sprint sprintData) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found"));

        sprint.setName(sprintData.getName());
        sprint.setStartDate(sprintData.getStartDate());
        sprint.setEndDate(sprintData.getEndDate());
        sprint.setStatus(sprintData.getStatus());

        return sprintRepository.save(sprint);
    }
}
