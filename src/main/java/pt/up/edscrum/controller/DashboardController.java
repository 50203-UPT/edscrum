package pt.up.edscrum.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.up.edscrum.dto.dashboard.RankingDTO;
import pt.up.edscrum.dto.dashboard.StudentDashboardDTO;
import pt.up.edscrum.dto.dashboard.TeacherDashboardDTO;
import pt.up.edscrum.service.DashboardService;

@RestController
@RequestMapping("/dashboard")
/**
 * Endpoints para obter dashboards de professor e estudante e rankings.
 */
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Obtém o dashboard do professor para um curso.
     *
     * @param courseId ID do curso
     * @return TeacherDashboardDTO com dados do dashboard
     */
    @GetMapping("/teacher/{courseId}")
    public ResponseEntity<TeacherDashboardDTO> getTeacherDashboard(@PathVariable Long courseId, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        if (!"TEACHER".equals(currentUserRole)) return ResponseEntity.status(403).build();
        TeacherDashboardDTO dto = dashboardService.getTeacherDashboard(courseId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Obtém o dashboard do estudante.
     *
     * @param studentId ID do estudante
     * @return StudentDashboardDTO com dados do estudante
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<StudentDashboardDTO> getStudentDashboard(@PathVariable Long studentId, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        if (!currentUserId.equals(studentId) && !"TEACHER".equals(currentUserRole)) return ResponseEntity.status(403).build();
        StudentDashboardDTO dto = dashboardService.getStudentDashboard(studentId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Obtém o ranking de estudantes de um curso.
     *
     * @param courseId ID do curso
     * @return Lista de RankingDTO para estudantes
     */
    @GetMapping("/ranking/student/{courseId}")
    public List<RankingDTO> getStudentRanking(@PathVariable Long courseId) {
        return dashboardService.getStudentRanking(courseId);
    }

    /**
     * Obtém o ranking de equipas de um curso.
     *
     * @param courseId ID do curso
     * @return Lista de RankingDTO para equipas
     */
    @GetMapping("/ranking/team/{courseId}")
    public List<RankingDTO> getTeamRanking(@PathVariable Long courseId) {
        return dashboardService.getTeamRanking(courseId);
    }
}
