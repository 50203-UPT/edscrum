package pt.up.edscrum.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.up.edscrum.model.Sprint;
import pt.up.edscrum.service.SprintService;

@RestController
@RequestMapping("/sprints")
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

    @PostMapping("/{sprintId}/complete")
    public Sprint completeSprint(@PathVariable Long sprintId) {
        return sprintService.completeSprint(sprintId);
    }

    @PostMapping("/{sprintId}/reopen")
    public Sprint reopenSprint(@PathVariable Long sprintId) {
        return sprintService.reopenSprint(sprintId);
    }

    @DeleteMapping("/{sprintId}")
    public void deleteSprint(@PathVariable Long sprintId) {
        sprintService.deleteSprint(sprintId);
    }
}
