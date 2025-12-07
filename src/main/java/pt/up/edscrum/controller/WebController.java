package pt.up.edscrum.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pt.up.edscrum.dto.dashboard.StudentDashboardDTO;
import pt.up.edscrum.dto.dashboard.TeacherDashboardDTO;
import pt.up.edscrum.model.User;
import pt.up.edscrum.service.AuthService;
import pt.up.edscrum.service.AwardService;
import pt.up.edscrum.service.CourseService;
import pt.up.edscrum.service.DashboardService;
import pt.up.edscrum.service.UserService;

@Controller
public class WebController {

    private final DashboardService dashboardService;
    private final AuthService authService;
    private final UserService userService;
    private final AwardService awardService;
    private final CourseService courseService;

    public WebController(DashboardService dashboardService,
            AuthService authService,
            UserService userService,
            AwardService awardService,
            CourseService courseService) {
        this.dashboardService = dashboardService;
        this.authService = authService;
        this.userService = userService;
        this.awardService = awardService;
        this.courseService = courseService;
    }

    // --- LOGIN & REGISTO ---
    @GetMapping("/")
    public String loginPage(Model model) {
        return "index";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/";
    }

    @PostMapping("/auth/web/login")
    public String webLogin(@RequestParam String email,
            @RequestParam String password,
            Model model) {

        User user = authService.login(email, password);

        if (user == null) {
            model.addAttribute("error", "Credenciais inválidas! Tenta novamente.");
            return "index";
        }

        if ("TEACHER".equals(user.getRole())) {
            // Agora vai para o menu principal do professor
            return "redirect:/view/teacher/home";
        } else {
            return "redirect:/view/student/home/" + user.getId();
        }
    }

    @PostMapping("/auth/web/register")
    public String webRegister(@ModelAttribute User user) {
        userService.createUser(user);
        return "redirect:/?registered=true";
    }

    // --- ÁREA DO PROFESSOR ---
    // 1. Dashboard Geral (Menu Principal)
    @GetMapping("/view/teacher/home")
    public String teacherHome() {
        return "teacherHome";
    }

    // 2. Lista de Cursos (Clicado a partir do menu)
    @GetMapping("/view/teacher/courses")
    public String teacherCoursesList(Model model) {
        model.addAttribute("courses", courseService.getAllCourses());
        return "teacherCourses";
    }

    // 3. Dashboard Específico de um Curso
    @GetMapping("/view/teacher/{courseId}")
    public String teacherDashboard(@PathVariable Long courseId, Model model) {
        TeacherDashboardDTO data = dashboardService.getTeacherDashboard(courseId);
        model.addAttribute("dashboard", data);
        model.addAttribute("allAwards", awardService.getAllAwards());
        model.addAttribute("allStudents", userService.getAllUsers());
        return "teacherDashboard";
    }

    @PostMapping("/action/assign-award")
    public String assignAwardAction(@RequestParam Long courseId,
            @RequestParam Long awardId,
            @RequestParam Long studentId) {
        try {
            awardService.assignAwardToStudent(awardId, studentId);
        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
        }
        return "redirect:/view/teacher/" + courseId;
    }

    // --- ÁREA DO ESTUDANTE ---
    // 1. Menu Principal do Estudante
    @GetMapping("/view/student/home/{studentId}")
    public String studentHome(@PathVariable Long studentId, Model model) {
        // Precisamos dos dados básicos (nome, courseId) para construir os links
        StudentDashboardDTO data = dashboardService.getStudentDashboard(studentId);
        model.addAttribute("student", data);
        return "studentHome";
    }

    // 2. Dashboard Detalhado (Prémios e Pontos)
    @GetMapping("/view/student/dashboard/{studentId}") // Mudei ligeiramente a URL para organizar
    public String studentDashboard(@PathVariable Long studentId, Model model) {
        StudentDashboardDTO data = dashboardService.getStudentDashboard(studentId);
        model.addAttribute("student", data);
        model.addAttribute("courseId", data.getCourseId());
        return "studentDashboard";
    }

    // --- RANKINGS ---
    @GetMapping("/view/rankings/{courseId}")
    public String viewRankings(@PathVariable Long courseId, Model model) {
        model.addAttribute("studentRanking", dashboardService.getStudentRanking(courseId));
        model.addAttribute("teamRanking", dashboardService.getTeamRanking(courseId));
        model.addAttribute("courseId", courseId);
        return "rankings";
    }
}
