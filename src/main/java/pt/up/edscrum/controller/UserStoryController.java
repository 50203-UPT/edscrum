package pt.up.edscrum.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.up.edscrum.enums.UserStoryStatus;
import pt.up.edscrum.model.UserStory;
import pt.up.edscrum.service.UserStoryService;

@RestController
@RequestMapping("/api/stories")
@CrossOrigin(origins = "*")
public class UserStoryController {

    private final UserStoryService userStoryService;

    public UserStoryController(UserStoryService userStoryService) {
        this.userStoryService = userStoryService;
    }

    @PostMapping
    public ResponseEntity<UserStory> createUserStory(@RequestBody UserStory userStory) {
        UserStory created = userStoryService.createUserStory(userStory);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/{storyId}/move")
    public ResponseEntity<UserStory> moveUserStory(@PathVariable Long storyId, @RequestParam String status) {
        UserStoryStatus newStatus = UserStoryStatus.valueOf(status);
        UserStory updated = userStoryService.updateUserStoryStatus(storyId, newStatus);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{storyId}")
    public ResponseEntity<UserStory> updateUserStory(@PathVariable Long storyId, @RequestBody UserStory userStory) {
        UserStory updated = userStoryService.updateUserStory(storyId, userStory);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{storyId}")
    public ResponseEntity<Void> deleteUserStory(@PathVariable Long storyId) {
        userStoryService.deleteUserStory(storyId);
        return ResponseEntity.ok().build();
    }
}
