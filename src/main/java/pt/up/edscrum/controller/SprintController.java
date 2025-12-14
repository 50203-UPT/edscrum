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
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpSession;

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

    /**
     * Obtém todos os sprints de um projeto.
     *
     * @param projectId ID do projeto
     * @return Lista de Sprint
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Sprint>> getSprints(@PathVariable Long projectId, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(sprintService.getSprintsByProject(projectId));
    }

    /**
     * Cria um sprint para um projeto.
     *
     * @param projectId ID do projeto
     * @param sprint Dados do sprint
     * @return Sprint criado
     */
    @PostMapping("/project/{projectId}")
    public ResponseEntity<Sprint> createSprint(@PathVariable Long projectId, @RequestBody Sprint sprint, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        if (sprint.getCreatedBy() != null && sprint.getCreatedBy().getId() != null && !currentUserId.equals(sprint.getCreatedBy().getId()) && !"TEACHER".equals(currentUserRole)) {
            return ResponseEntity.status(403).build();
        }
        Sprint created = sprintService.createSprint(projectId, sprint);
        return ResponseEntity.status(201).body(created);
    }

    /**
     * Atualiza um sprint existente.
     *
     * @param sprintId ID do sprint
     * @param sprint Dados atualizados
     * @return Sprint atualizado
     */
    @PutMapping("/{sprintId}")
    public ResponseEntity<Sprint> updateSprint(@PathVariable Long sprintId, @RequestBody Sprint sprint, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        Sprint existing = sprintService.getSprintById(sprintId);
        Long ownerId = existing.getCreatedBy() != null ? existing.getCreatedBy().getId() : null;
        Long courseTeacherId = null;
        try { courseTeacherId = existing.getProject().getCourse().getTeacher().getId(); } catch (Exception e) { }
        if (!"TEACHER".equals(currentUserRole) || (courseTeacherId != null && !courseTeacherId.equals(currentUserId))) {
            if (ownerId == null || !ownerId.equals(currentUserId)) return ResponseEntity.status(403).build();
        }
        Sprint updated = sprintService.updateSprint(sprintId, sprint);
        return ResponseEntity.ok(updated);
    }

    /**
     * Marca um sprint como concluído.
     *
     * @param sprintId ID do sprint
     * @return Sprint após conclusão
     */
    @PostMapping("/{sprintId}/complete")
    public ResponseEntity<Sprint> completeSprint(@PathVariable Long sprintId, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        Sprint existing = sprintService.getSprintById(sprintId);
        Long ownerId = existing.getCreatedBy() != null ? existing.getCreatedBy().getId() : null;
        Long courseTeacherId = null;
        try { courseTeacherId = existing.getProject().getCourse().getTeacher().getId(); } catch (Exception e) { }
        if (!"TEACHER".equals(currentUserRole) || (courseTeacherId != null && !courseTeacherId.equals(currentUserId))) {
            if (ownerId == null || !ownerId.equals(currentUserId)) return ResponseEntity.status(403).build();
        }
        Sprint s = sprintService.completeSprint(sprintId);
        return ResponseEntity.ok(s);
    }

    /**
     * Reabre um sprint previamente concluído.
     *
     * @param sprintId ID do sprint
     * @return Sprint reaberto
     */
    @PostMapping("/{sprintId}/reopen")
    public ResponseEntity<Sprint> reopenSprint(@PathVariable Long sprintId, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        Sprint existing = sprintService.getSprintById(sprintId);
        Long ownerId = existing.getCreatedBy() != null ? existing.getCreatedBy().getId() : null;
        Long courseTeacherId = null;
        try { courseTeacherId = existing.getProject().getCourse().getTeacher().getId(); } catch (Exception e) { }
        if (!"TEACHER".equals(currentUserRole) || (courseTeacherId != null && !courseTeacherId.equals(currentUserId))) {
            if (ownerId == null || !ownerId.equals(currentUserId)) return ResponseEntity.status(403).build();
        }
        Sprint s = sprintService.reopenSprint(sprintId);
        return ResponseEntity.ok(s);
    }

    /**
     * Elimina um sprint pelo ID.
     *
     * @param sprintId ID do sprint a eliminar
     */
    @DeleteMapping("/{sprintId}")
    public ResponseEntity<Void> deleteSprint(@PathVariable Long sprintId, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        Sprint existing = sprintService.getSprintById(sprintId);
        Long ownerId = existing.getCreatedBy() != null ? existing.getCreatedBy().getId() : null;
        Long courseTeacherId = null;
        try { courseTeacherId = existing.getProject().getCourse().getTeacher().getId(); } catch (Exception e) { }
        if (!"TEACHER".equals(currentUserRole) || (courseTeacherId != null && !courseTeacherId.equals(currentUserId))) {
            if (ownerId == null || !ownerId.equals(currentUserId)) return ResponseEntity.status(403).build();
        }
        sprintService.deleteSprint(sprintId);
        return ResponseEntity.noContent().build();
    }
}
