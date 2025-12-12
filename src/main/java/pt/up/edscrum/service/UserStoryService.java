package pt.up.edscrum.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.up.edscrum.enums.UserStoryStatus;
import pt.up.edscrum.model.Sprint;
import pt.up.edscrum.model.User;
import pt.up.edscrum.model.UserStory;
import pt.up.edscrum.repository.SprintRepository;
import pt.up.edscrum.repository.UserRepository;
import pt.up.edscrum.repository.UserStoryRepository;

@Service
@Transactional
public class UserStoryService {

    private final UserStoryRepository userStoryRepository;
    private final SprintRepository sprintRepository;
    private final UserRepository userRepository;

    public UserStoryService(UserStoryRepository userStoryRepository, 
                           SprintRepository sprintRepository,
                           UserRepository userRepository) {
        this.userStoryRepository = userStoryRepository;
        this.sprintRepository = sprintRepository;
        this.userRepository = userRepository;
    }

    public UserStory createUserStory(UserStory userStory) {
        // Garantir que o sprint existe
        if (userStory.getSprint() != null && userStory.getSprint().getId() != null) {
            Sprint sprint = sprintRepository.findById(userStory.getSprint().getId())
                .orElseThrow(() -> new RuntimeException("Sprint não encontrado"));
            userStory.setSprint(sprint);
        }

        // Garantir que o assignee existe, se fornecido
        if (userStory.getAssignee() != null && userStory.getAssignee().getId() != null) {
            User assignee = userRepository.findById(userStory.getAssignee().getId())
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
            userStory.setAssignee(assignee);
        }

        // Status default é TODO
        if (userStory.getStatus() == null) {
            userStory.setStatus(UserStoryStatus.TODO);
        }

        return userStoryRepository.save(userStory);
    }

    public UserStory updateUserStoryStatus(Long storyId, UserStoryStatus newStatus) {
        UserStory story = userStoryRepository.findById(storyId)
            .orElseThrow(() -> new RuntimeException("User Story não encontrada"));
        
        story.setStatus(newStatus);
        return userStoryRepository.save(story);
    }

    public UserStory updateUserStory(Long storyId, UserStory updatedStory) {
        UserStory existing = userStoryRepository.findById(storyId)
            .orElseThrow(() -> new RuntimeException("User Story não encontrada"));
        
        existing.setName(updatedStory.getName());
        existing.setDescription(updatedStory.getDescription());
        existing.setPriority(updatedStory.getPriority());
        existing.setStoryPoints(updatedStory.getStoryPoints());
        
        if (updatedStory.getAssignee() != null && updatedStory.getAssignee().getId() != null) {
            User assignee = userRepository.findById(updatedStory.getAssignee().getId())
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
            existing.setAssignee(assignee);
        }
        
        return userStoryRepository.save(existing);
    }

    public void deleteUserStory(Long storyId) {
        userStoryRepository.deleteById(storyId);
    }
}
