package pt.up.edscrum.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.up.edscrum.enums.UserStoryStatus;
import pt.up.edscrum.model.UserStory;
import pt.up.edscrum.service.UserStoryService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/stories")
@CrossOrigin(origins = "*")
public class UserStoryController {

    private final UserStoryService userStoryService;

    public UserStoryController(UserStoryService userStoryService) {
        this.userStoryService = userStoryService;
    }

    /**
     * Cria uma nova user story.
     *
     * @param userStory Objeto UserStory com os dados
     * @return ResponseEntity com a UserStory criada
     */
    @PostMapping
    public ResponseEntity<UserStory> createUserStory(@RequestBody UserStory userStory, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        if (userStory.getAssignee() != null && userStory.getAssignee().getId() != null) {
            if (!currentUserId.equals(userStory.getAssignee().getId()) && !"TEACHER".equals(currentUserRole)) {
                return ResponseEntity.status(403).build();
            }
        }
        UserStory created = userStoryService.createUserStory(userStory);
        return ResponseEntity.ok(created);
    }

    /**
     * Move uma user story para outro estado (status).
     *
     * @param storyId ID da user story
     * @param status Novo estado (nome do enum)
     * @return ResponseEntity com a UserStory atualizada
     */
    @PostMapping("/{storyId}/move")
    public ResponseEntity<UserStory> moveUserStory(@PathVariable Long storyId, @RequestParam String status, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        // Allow if assignee equals current user or teacher
        UserStory existing = userStoryService.getUserStoryById(storyId);
        if (existing.getAssignee() != null && existing.getAssignee().getId() != null && !currentUserId.equals(existing.getAssignee().getId()) && !"TEACHER".equals(currentUserRole)) {
            return ResponseEntity.status(403).build();
        }
        UserStoryStatus newStatus = UserStoryStatus.valueOf(status);
        UserStory updated = userStoryService.updateUserStoryStatus(storyId, newStatus);
        return ResponseEntity.ok(updated);
    }

    /**
     * Atualiza os dados de uma user story.
     *
     * @param storyId ID da user story
     * @param userStory Dados atualizados
     * @return ResponseEntity com a UserStory atualizada
     */
    @PutMapping("/{storyId}")
    public ResponseEntity<UserStory> updateUserStory(@PathVariable Long storyId, @RequestBody UserStory userStory, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        // If updating assignee, ensure current user is the new assignee or a teacher
        if (userStory.getAssignee() != null && userStory.getAssignee().getId() != null) {
            if (!currentUserId.equals(userStory.getAssignee().getId()) && !"TEACHER".equals(currentUserRole)) {
                return ResponseEntity.status(403).build();
            }
        }
        UserStory updated = userStoryService.updateUserStory(storyId, userStory);
        return ResponseEntity.ok(updated);
    }

    /**
     * Elimina uma user story.
     *
     * @param storyId ID da user story a eliminar
     * @return ResponseEntity vazio (200)
     */
    @DeleteMapping("/{storyId}")
    public ResponseEntity<Void> deleteUserStory(@PathVariable Long storyId, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        // Allow deletion if teacher, the assignee of the story, or the sprint's creator
        UserStory existing = userStoryService.getUserStoryById(storyId);
        Long assigneeId = existing.getAssignee() != null ? existing.getAssignee().getId() : null;
        Long sprintOwnerId = null;
        try { sprintOwnerId = existing.getSprint().getCreatedBy() != null ? existing.getSprint().getCreatedBy().getId() : null; } catch (Exception e) { }
        if (!"TEACHER".equals(currentUserRole)) {
            if ((assigneeId == null || !assigneeId.equals(currentUserId)) && (sprintOwnerId == null || !sprintOwnerId.equals(currentUserId))) {
                return ResponseEntity.status(403).build();
            }
        }
        userStoryService.deleteUserStory(storyId);
        return ResponseEntity.ok().build();
    }
}
