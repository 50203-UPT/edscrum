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
import pt.up.edscrum.service.TeamService;
import pt.up.edscrum.service.SprintService;
import pt.up.edscrum.model.Team;
import pt.up.edscrum.service.UserService;
import pt.up.edscrum.model.User;
import java.util.Optional;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/stories")
@CrossOrigin(origins = "*")
/**
 * API para criação, atualização e movimentação de user stories dentro de
 * sprints e projetos.
 */
public class UserStoryController {

    private final UserStoryService userStoryService;
    private final UserService userService;
    private final TeamService teamService;
    private final SprintService sprintService;

    public UserStoryController(UserStoryService userStoryService, UserService userService, TeamService teamService, SprintService sprintService) {
        this.userStoryService = userStoryService;
        this.userService = userService;
        this.teamService = teamService;
        this.sprintService = sprintService;
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
        if (userStory.getAssignee() != null && userStory.getAssignee().getId() == null && userStory.getAssignee().getEmail() != null) {
            Optional<User> opt = userService.getUserByEmail(userStory.getAssignee().getEmail());
            if (opt.isPresent()) {
                userStory.setAssignee(opt.get());
            } else {
                return ResponseEntity.badRequest().build();
            }
        }
        if (userStory.getAssignee() != null && userStory.getAssignee().getId() != null) {
            boolean isAllowed = false;
            if (currentUserId.equals(userStory.getAssignee().getId())) isAllowed = true;
            if ("TEACHER".equals(currentUserRole)) isAllowed = true;

                    
            try {
                if (!isAllowed && userStory.getSprint() != null && userStory.getSprint().getId() != null) {
                    var sprint = sprintService.getSprintById(userStory.getSprint().getId());
                    if (sprint != null && sprint.getProject() != null && sprint.getProject().getTeams() != null) {
                        for (Team t : sprint.getProject().getTeams()) {
                            if (t.getScrumMaster() != null && t.getScrumMaster().getId().equals(currentUserId)) { isAllowed = true; break; }
                            if (t.getProductOwner() != null && t.getProductOwner().getId().equals(currentUserId)) { isAllowed = true; break; }
                            if (t.getDevelopers() != null) {
                                for (pt.up.edscrum.model.User d : t.getDevelopers()) {
                                    if (d != null && d.getId() != null && d.getId().equals(currentUserId)) { isAllowed = true; break; }
                                }
                                if (isAllowed) break;
                            }
                        }
                    }
                }
            } catch (Exception e) { }

            if (!isAllowed) return ResponseEntity.status(403).build();
        }
        
        if (userStory.getCreatedBy() == null && currentUserId != null) {
            User creator = userService.getUserById(currentUserId);
            userStory.setCreatedBy(creator);
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
        
        UserStory existing = userStoryService.getUserStoryById(storyId);
        boolean isProjectTeamMember = false;
        try {
            if (existing.getSprint() != null && existing.getSprint().getProject() != null && existing.getSprint().getProject().getTeams() != null) {
                for (Team t : existing.getSprint().getProject().getTeams()) {
                    if (t.getScrumMaster() != null && t.getScrumMaster().getId().equals(currentUserId)) { isProjectTeamMember = true; break; }
                    if (t.getProductOwner() != null && t.getProductOwner().getId().equals(currentUserId)) { isProjectTeamMember = true; break; }
                    if (t.getDevelopers() != null) {
                        for (pt.up.edscrum.model.User d : t.getDevelopers()) {
                            if (d != null && d.getId() != null && d.getId().equals(currentUserId)) { isProjectTeamMember = true; break; }
                        }
                        if (isProjectTeamMember) break;
                    }
                }
            }
        } catch (Exception e) { }

        if (existing.getAssignee() != null && existing.getAssignee().getId() != null
                && !currentUserId.equals(existing.getAssignee().getId())
                && !"TEACHER".equals(currentUserRole)
                && !isProjectTeamMember) {
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
        
        if (userStory.getAssignee() != null && userStory.getAssignee().getId() == null && userStory.getAssignee().getEmail() != null) {
            Optional<User> opt = userService.getUserByEmail(userStory.getAssignee().getEmail());
            if (opt.isPresent()) {
                userStory.setAssignee(opt.get());
            } else {
                return ResponseEntity.badRequest().build();
            }
        }

        if (userStory.getAssignee() != null && userStory.getAssignee().getId() != null) {
            boolean isAllowed = false;
            if (currentUserId.equals(userStory.getAssignee().getId())) isAllowed = true;
            if ("TEACHER".equals(currentUserRole)) isAllowed = true;

            
            try {
                if (!isAllowed) {
                    UserStory existing = userStoryService.getUserStoryById(storyId);
                    if (existing != null && existing.getSprint() != null && existing.getSprint().getProject() != null) {
                        for (Team t : existing.getSprint().getProject().getTeams()) {
                            if (t.getScrumMaster() != null && t.getScrumMaster().getId().equals(currentUserId)) { isAllowed = true; break; }
                            if (t.getProductOwner() != null && t.getProductOwner().getId().equals(currentUserId)) { isAllowed = true; break; }
                            if (t.getDevelopers() != null) {
                                for (pt.up.edscrum.model.User d : t.getDevelopers()) {
                                    if (d != null && d.getId() != null && d.getId().equals(currentUserId)) { isAllowed = true; break; }
                                }
                                if (isAllowed) break;
                            }
                        }
                    }
                }
            } catch (Exception e) { }

            if (!isAllowed) return ResponseEntity.status(403).build();
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
        
        UserStory existing = userStoryService.getUserStoryById(storyId);
        Long assigneeId = existing.getAssignee() != null ? existing.getAssignee().getId() : null;
        Long sprintOwnerId = null;
        try { sprintOwnerId = existing.getSprint().getCreatedBy() != null ? existing.getSprint().getCreatedBy().getId() : null; } catch (Exception e) { }
        Long creatorId = existing.getCreatedBy() != null ? existing.getCreatedBy().getId() : null;

        
        boolean isProjectTeamMember = false;
        try {
            if (existing.getSprint() != null && existing.getSprint().getProject() != null) {
                for (Team t : existing.getSprint().getProject().getTeams()) {
                    if (t.getScrumMaster() != null && t.getScrumMaster().getId().equals(currentUserId)) { isProjectTeamMember = true; break; }
                    if (t.getProductOwner() != null && t.getProductOwner().getId().equals(currentUserId)) { isProjectTeamMember = true; break; }
                    if (t.getDevelopers() != null) {
                        for (pt.up.edscrum.model.User d : t.getDevelopers()) {
                            if (d != null && d.getId() != null && d.getId().equals(currentUserId)) { isProjectTeamMember = true; break; }
                        }
                        if (isProjectTeamMember) break;
                    }
                }
            }
        } catch (Exception e) { }

        if (!"TEACHER".equals(currentUserRole)) {
            if ((assigneeId == null || !assigneeId.equals(currentUserId))
                    && (sprintOwnerId == null || !sprintOwnerId.equals(currentUserId))
                    && (creatorId == null || !creatorId.equals(currentUserId))
                    && !isProjectTeamMember) {
                return ResponseEntity.status(403).build();
            }
        }
        userStoryService.deleteUserStory(storyId);
        return ResponseEntity.ok().build();
    }
}
