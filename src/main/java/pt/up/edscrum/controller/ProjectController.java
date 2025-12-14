package pt.up.edscrum.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.up.edscrum.dto.dashboard.ProjectDetailsDTO;
import pt.up.edscrum.model.Project;
import pt.up.edscrum.service.DashboardService;
import pt.up.edscrum.service.ProjectService;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final DashboardService dashboardService;

    public ProjectController(ProjectService projectService, DashboardService dashboardService) {
        this.projectService = projectService;
        this.dashboardService = dashboardService;
    }

    /**
     * Obtém todos os projetos.
     *
     * @return Lista de Project
     */
    @GetMapping
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    /**
     * Obtém um projeto por ID.
     *
     * @param id ID do projeto
     * @return ResponseEntity com o Project
     */
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable Long id) {
        Project project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    /**
     * Obtém detalhes avançados do projeto (inclui sprints, equipa e métricas).
     *
     * @param id ID do projeto
     * @return ResponseEntity com ProjectDetailsDTO
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<ProjectDetailsDTO> getProjectDetails(@PathVariable Long id) {
        ProjectDetailsDTO details = dashboardService.getProjectDetails(id);
        return ResponseEntity.ok(details);
    }

    /**
     * Cria um novo projeto.
     *
     * @param project Dados do projeto no corpo da requisição
     * @return ResponseEntity com o Project criado (201)
     */
    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        Project created = projectService.createProject(project);
        return ResponseEntity.status(201).body(created);
    }

    /**
     * Atualiza um projeto existente.
     *
     * @param id ID do projeto
     * @param projectDetails Dados atualizados do projeto
     * @return ResponseEntity com o Project atualizado
     */
    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable Long id, @RequestBody Project projectDetails) {
        Project updated = projectService.updateProject(id, projectDetails);
        return ResponseEntity.ok(updated);
    }

    /**
     * Elimina um projeto por ID.
     *
     * @param id ID do projeto a eliminar
     * @return ResponseEntity sem conteúdo (204)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Marca um projeto como concluído.
     *
     * @param id ID do projeto
     * @return ResponseEntity 200 OK se concluído ou erro apropriado
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeProject(@PathVariable Long id, @RequestParam(required = false) Long studentId, jakarta.servlet.http.HttpSession session) {
        try {
            Long currentUserId = (Long) session.getAttribute("currentUserId");
            String currentUserRole = (String) session.getAttribute("currentUserRole");
            if (currentUserId == null) return ResponseEntity.status(401).body("Unauthorized");

            // Ensure the acting user is the one in session (or teacher)
            if (studentId != null && !currentUserId.equals(studentId) && !"TEACHER".equals(currentUserRole)) {
                return ResponseEntity.status(403).body("Forbidden");
            }

            // Determine the effective acting user: either the studentId (when teacher overrides)
            // or the current session user. Use that to verify Product Owner role.
            Long actingUserId = (studentId != null) ? studentId : currentUserId;
            if (!projectService.isUserProductOwner(actingUserId, id)) {
                return ResponseEntity.status(403).body("Apenas o Product Owner pode concluir o projeto.");
            }
            projectService.completeProject(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao concluir projeto: " + e.getMessage());
        }
    }

    /**
     * Reabre um projeto que estava concluído.
     *
     * @param id ID do projeto
     * @return ResponseEntity 200 OK se reaberto ou erro 500 em falha
     */
    @PutMapping("/{id}/reopen")
    public ResponseEntity<?> reopenProject(@PathVariable Long id, @RequestParam(required = false) Long studentId, jakarta.servlet.http.HttpSession session) {
        try {
            Long currentUserId = (Long) session.getAttribute("currentUserId");
            String currentUserRole = (String) session.getAttribute("currentUserRole");
            if (currentUserId == null) return ResponseEntity.status(401).body("Unauthorized");

            if (studentId != null && !currentUserId.equals(studentId) && !"TEACHER".equals(currentUserRole)) {
                return ResponseEntity.status(403).body("Forbidden");
            }

            Long actingUserIdReopen = (studentId != null) ? studentId : currentUserId;

            if (!projectService.isUserProductOwner(actingUserIdReopen, id)) {
                return ResponseEntity.status(403).body("Apenas o Product Owner pode reabrir o projeto.");
            }
            projectService.reopenProject(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao reabrir projeto: " + e.getMessage());
        }
    }

    /**
     * Remove a associação de uma equipa a um projeto.
     *
     * @param projectId ID do projeto
     * @param teamId ID da equipa
     * @return ResponseEntity 200 OK se removido ou 500 em caso de erro
     */
    @PostMapping("/{projectId}/remove-team/{teamId}")
    public ResponseEntity<?> removeTeamFromProject(@PathVariable Long projectId, @PathVariable Long teamId) {
        try {
            projectService.removeTeamFromProject(projectId, teamId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao remover equipa: " + e.getMessage());
        }
    }
}
