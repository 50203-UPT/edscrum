package pt.up.edscrum.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.enums.UserStoryStatus;
import pt.up.edscrum.model.Sprint;
import pt.up.edscrum.enums.SprintStatus;
import pt.up.edscrum.enums.ProjectStatus;
import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.User;
import pt.up.edscrum.model.UserStory;
import pt.up.edscrum.repository.SprintRepository;
import pt.up.edscrum.repository.UserRepository;
import pt.up.edscrum.repository.UserStoryRepository;

@Service
@Transactional
/**
 * Serviço para gestão de `UserStory`: criação, atualização de estado, edição e
 * remoção. Garante integridade referencial para sprint e assignee.
 */
public class UserStoryService {

    private final UserStoryRepository userStoryRepository;
    private final SprintRepository sprintRepository;
    private final UserRepository userRepository;

    /**
     * Construtor do serviço de user stories.
     */
    public UserStoryService(UserStoryRepository userStoryRepository,
            SprintRepository sprintRepository,
            UserRepository userRepository) {
        this.userStoryRepository = userStoryRepository;
        this.sprintRepository = sprintRepository;
        this.userRepository = userRepository;
    }

    /**
     * Cria uma nova user story. Valida e associa o sprint e o assignee quando
     * fornecidos. Define o estado por omissão para `TODO`.
     *
     * @param userStory objecto `UserStory` a criar
     * @return user story persistida
     */
    public UserStory createUserStory(UserStory userStory) {
        // Garantir que o sprint existe
        if (userStory.getSprint() != null && userStory.getSprint().getId() != null) {
            Sprint sprint = sprintRepository.findById(userStory.getSprint().getId())
                    .orElseThrow(() -> new RuntimeException("Sprint não encontrado"));

            // Se o sprint estiver em PLANEAMENTO, passar para EM_CURSO
            if (sprint.getStatus() == SprintStatus.PLANEAMENTO) {
                sprint.setStatus(SprintStatus.EM_CURSO);

                // Se o projeto associado estiver em PLANEAMENTO, também iniciar o projeto
                Project project = sprint.getProject();
                if (project != null && project.getStatus() == ProjectStatus.PLANEAMENTO) {
                    project.setStatus(ProjectStatus.EM_CURSO);
                }

                sprintRepository.save(sprint);
            }

            userStory.setSprint(sprint);
        }

        // Garantir que o assignee existe, se fornecido. Suporta "id" ou "email" no payload.
        if (userStory.getAssignee() != null) {
            if (userStory.getAssignee().getId() != null) {
                User assignee = userRepository.findById(userStory.getAssignee().getId())
                        .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
                userStory.setAssignee(assignee);
            } else if (userStory.getAssignee().getEmail() != null) {
                User assignee = userRepository.findByEmail(userStory.getAssignee().getEmail())
                        .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
                userStory.setAssignee(assignee);
            }
        }

        // Status default é TODO
        if (userStory.getStatus() == null) {
            userStory.setStatus(UserStoryStatus.TODO);
        }

        return userStoryRepository.save(userStory);
    }

    /**
     * Atualiza o estado de uma user story.
     *
     * @param storyId id da user story
     * @param newStatus novo estado (`UserStoryStatus`)
     * @return user story atualizada
     */
    public UserStory updateUserStoryStatus(Long storyId, UserStoryStatus newStatus) {
        UserStory story = userStoryRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("User Story não encontrada"));

        story.setStatus(newStatus);
        return userStoryRepository.save(story);
    }

    /**
     * Recupera uma UserStory pelo seu id.
     */
    public UserStory getUserStoryById(Long storyId) {
        return userStoryRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("User Story não encontrada"));
    }

    /**
     * Atualiza os campos editáveis de uma user story (nome, descrição,
     * prioridade, story points e assignee).
     *
     * @param storyId id da user story a atualizar
     * @param updatedStory objecto com os novos valores
     * @return user story atualizada
     */
    public UserStory updateUserStory(Long storyId, UserStory updatedStory) {
        UserStory existing = userStoryRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("User Story não encontrada"));

        existing.setName(updatedStory.getName());
        existing.setDescription(updatedStory.getDescription());
        existing.setPriority(updatedStory.getPriority());
        existing.setStoryPoints(updatedStory.getStoryPoints());

        if (updatedStory.getAssignee() != null) {
            if (updatedStory.getAssignee().getId() != null) {
                User assignee = userRepository.findById(updatedStory.getAssignee().getId())
                        .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
                existing.setAssignee(assignee);
            } else if (updatedStory.getAssignee().getEmail() != null) {
                User assignee = userRepository.findByEmail(updatedStory.getAssignee().getEmail())
                        .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
                existing.setAssignee(assignee);
            }
        }

        return userStoryRepository.save(existing);
    }

    /**
     * Elimina uma user story pelo seu id.
     *
     * @param storyId id da user story a eliminar
     */
    public void deleteUserStory(Long storyId) {
        userStoryRepository.deleteById(storyId);
    }
}
