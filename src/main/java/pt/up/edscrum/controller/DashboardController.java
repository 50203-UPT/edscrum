package pt.up.edscrum.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.up.edscrum.dto.dashboard.RankingDTO;
import pt.up.edscrum.dto.dashboard.StudentDashboardDTO;
import pt.up.edscrum.dto.dashboard.TeacherDashboardDTO;
import pt.up.edscrum.service.DashboardService;

@RestController
@RequestMapping("/dashboard")
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
    public TeacherDashboardDTO getTeacherDashboard(@PathVariable Long courseId) {
        return dashboardService.getTeacherDashboard(courseId);
    }

    /**
     * Obtém o dashboard do estudante.
     *
     * @param studentId ID do estudante
     * @return StudentDashboardDTO com dados do estudante
     */
    @GetMapping("/student/{studentId}")
    public StudentDashboardDTO getStudentDashboard(@PathVariable Long studentId) {
        return dashboardService.getStudentDashboard(studentId);
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
