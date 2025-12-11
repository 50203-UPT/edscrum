package pt.up.edscrum.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pt.up.edscrum.enums.ProjectStatus;
import pt.up.edscrum.enums.UserStoryStatus;
import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.Sprint;
import pt.up.edscrum.model.UserStory;
import pt.up.edscrum.repository.ProjectRepository;
import pt.up.edscrum.repository.SprintRepository;
import pt.up.edscrum.repository.UserStoryRepository;

@Service
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final UserStoryRepository userStoryRepository;

    public SprintService(SprintRepository sprintRepository, ProjectRepository projectRepository, UserStoryRepository userStoryRepository) {
        this.sprintRepository = sprintRepository;
        this.projectRepository = projectRepository;
        this.userStoryRepository = userStoryRepository;
    }

    public List<Sprint> getSprintsByProject(Long projectId) {
        return sprintRepository.findByProjectId(projectId);
    }

    public Sprint createSprint(Long projectId, Sprint sprint) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Se o projeto estiver em PLANEAMENTO, passa para EM_CURSO
        if (project.getStatus() == ProjectStatus.PLANEAMENTO) {
            project.setStatus(ProjectStatus.EM_CURSO);
            projectRepository.save(project);
        }

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

    public Sprint getSprintById(Long id) {
        return sprintRepository.findById(id).orElseThrow(() -> new RuntimeException("Sprint não encontrada"));
    }

    public void updateUserStoryStatus(Long storyId, String newStatus) {
        UserStory story = userStoryRepository.findById(storyId).orElseThrow();
        story.setStatus(UserStoryStatus.valueOf(newStatus));
        userStoryRepository.save(story);
    }

    // Calcula percentagem para o Dashboard
    public int calculateSprintProgress(Long sprintId) {
        Sprint sprint = getSprintById(sprintId);
        if (sprint.getUserStories() == null || sprint.getUserStories().isEmpty()) {
            return 0;
        }

        long doneCount = sprint.getUserStories().stream()
                .filter(us -> us.getStatus() == UserStoryStatus.DONE)
                .count();

        return (int) ((doneCount * 100.0) / sprint.getUserStories().size());
    }
}
