package pt.up.edscrum.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pt.up.edscrum.enums.ProjectStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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

        // Valida datas do sprint: start < end
        if (sprint.getStartDate() == null || sprint.getEndDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datas do sprint não podem ser nulas");
        }
        if (!sprint.getStartDate().isBefore(sprint.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de início do sprint deve ser anterior à data de fim");
        }

        // Valida que o sprint está dentro do intervalo do projeto (se o projeto tem datas)
        if (project.getStartDate() != null && project.getEndDate() != null) {
            if (sprint.getStartDate().isBefore(project.getStartDate()) || sprint.getEndDate().isAfter(project.getEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sprint deve estar dentro do intervalo do projeto");
            }
        }

        // Verifica sobreposição com outros sprints do mesmo projeto
        List<Sprint> existing = sprintRepository.findByProjectId(projectId);
        for (Sprint s : existing) {
            // Se houver qualquer interseção (inclui igualdade de datas) => inválido
            if (!(sprint.getEndDate().isBefore(s.getStartDate()) || sprint.getStartDate().isAfter(s.getEndDate()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sprint overlap com sprint existente");
            }
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

        // Valida datas semelhantes às do create
        if (sprint.getStartDate() == null || sprint.getEndDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datas do sprint não podem ser nulas");
        }
        if (!sprint.getStartDate().isBefore(sprint.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de início do sprint deve ser anterior à data de fim");
        }

        Project project = sprint.getProject();
        if (project != null && project.getStartDate() != null && project.getEndDate() != null) {
            if (sprint.getStartDate().isBefore(project.getStartDate()) || sprint.getEndDate().isAfter(project.getEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sprint deve estar dentro do intervalo do projeto");
            }
        }

        // Verifica sobreposição com outros sprints, excluindo o próprio
        List<Sprint> existing = sprintRepository.findByProjectId(sprint.getProject().getId());
        for (Sprint s : existing) {
            if (s.getId().equals(sprint.getId())) {
                continue;
            }
            if (!(sprint.getEndDate().isBefore(s.getStartDate()) || sprint.getStartDate().isAfter(s.getEndDate()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sprint overlap com sprint existente");
            }
        }

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
