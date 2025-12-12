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

    // Calcula percentagem para o Dashboard baseado no status de cada story
    // TODO = 0%, IN_PROGRESS = 25%, TESTING = 75%, DONE = 100%
    public int calculateSprintProgress(Long sprintId) {
        Sprint sprint = getSprintById(sprintId);
        if (sprint.getUserStories() == null || sprint.getUserStories().isEmpty()) {
            return 0;
        }

        // Calcular média dos valores fixos de cada story
        double totalProgress = sprint.getUserStories().stream()
                .mapToDouble(us -> {
                    return switch (us.getStatus()) {
                        case TODO -> 0.0;
                        case IN_PROGRESS -> 25.0;
                        case TESTING -> 75.0;
                        case DONE -> 100.0;
                    };
                })
                .sum();

        int totalStories = sprint.getUserStories().size();
        return (int) (totalProgress / totalStories);
    }

    public Sprint completeSprint(Long sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint não encontrada"));

        // Verificar se todas as user stories estão DONE
        boolean allDone = sprint.getUserStories().stream()
                .allMatch(us -> us.getStatus() == UserStoryStatus.DONE);

        if (!allDone) {
            throw new RuntimeException("Não é possível marcar o sprint como concluído. Todas as User Stories devem estar concluídas (DONE).");
        }

        sprint.setStatus(pt.up.edscrum.enums.SprintStatus.CONCLUIDO);
        return sprintRepository.save(sprint);
    }

    public Sprint reopenSprint(Long sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint não encontrada"));

        sprint.setStatus(pt.up.edscrum.enums.SprintStatus.EM_CURSO);
        
        // Se o projeto estiver concluído, reabri-lo automaticamente
        Project project = sprint.getProject();
        if (project != null && project.getStatus() == pt.up.edscrum.enums.ProjectStatus.CONCLUIDO) {
            project.setStatus(pt.up.edscrum.enums.ProjectStatus.EM_CURSO);
        }
        
        return sprintRepository.save(sprint);
    }

    public void deleteSprint(Long sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint não encontrada"));
        
        // Eliminar todas as user stories associadas
        if (sprint.getUserStories() != null && !sprint.getUserStories().isEmpty()) {
            userStoryRepository.deleteAll(sprint.getUserStories());
        }
        
        // Eliminar o sprint
        sprintRepository.delete(sprint);
    }
}
