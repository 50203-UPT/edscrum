package pt.up.edscrum.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;

import pt.up.edscrum.model.Sprint;
import pt.up.edscrum.service.SprintService;

@RestController
@RequestMapping("/api/sprints")
@CrossOrigin(origins = "*")
public class SprintController {

    private final SprintService sprintService;

    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    @GetMapping("/project/{projectId}")
    public List<Sprint> getSprints(@PathVariable Long projectId) {
        return sprintService.getSprintsByProject(projectId);
    }

    @PostMapping("/project/{projectId}")
    public Sprint createSprint(@PathVariable Long projectId, @RequestBody Sprint sprint) {
        return sprintService.createSprint(projectId, sprint);
    }

    @PutMapping("/{sprintId}")
    public Sprint updateSprint(@PathVariable Long sprintId, @RequestBody Sprint sprint) {
        return sprintService.updateSprint(sprintId, sprint);
    }
}
