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

    // 4. Página de Configurações do Professor
    @GetMapping("/view/teacher/settings")
    public String teacherSettings(Model model) {
        // NOTA: Numa aplicação real, o utilizador logado seria obtido através do contexto de segurança.
        // Para este exemplo, vamos usar um utilizador estático (ex: o primeiro professor).
        User teacher = userService.getAllUsers().stream()
                                    .filter(u -> "TEACHER".equals(u.getRole()))
                                    .findFirst()
                                    .orElse(null);
        model.addAttribute("teacher", teacher);
        return "teacherSettings";
    }

    @PostMapping("/api/teacher/settings")
    public String updateTeacherSettings(@RequestParam String name,
                                      @RequestParam String email,
                                      @RequestParam(required = false) boolean notificationAwards,
                                      @RequestParam(required = false) boolean notificationRankings) {
        User teacher = userService.getUserByEmail(email);
        if (teacher != null) {
            teacher.setName(name);
            teacher.setNotificationAwards(notificationAwards);
            teacher.setNotificationRankings(notificationRankings);
            userService.updateUser(teacher.getId(), teacher);
        }
        return "redirect:/view/teacher/home";
    }

    // 5. Página de Edição de Perfil
    @GetMapping("/view/teacher/profile")
    public String editProfile(Model model) {
        User teacher = userService.getAllUsers().stream()
                                    .filter(u -> "TEACHER".equals(u.getRole()))
                                    .findFirst()
                                    .orElse(null);
        model.addAttribute("teacher", teacher);
        return "editProfile";
    }

    @PostMapping("/api/teacher/profile/update")
    public String updateProfile(@RequestParam String name,
                                @RequestParam String email,
                                @RequestParam String newPassword,
                                @RequestParam String currentPassword) {
        User teacher = userService.getUserByEmail(email);
        if (teacher != null && teacher.getPassword().equals(currentPassword)) {
            teacher.setName(name);
            teacher.setEmail(email);
            if (newPassword != null && !newPassword.isEmpty()) {
                teacher.setPassword(newPassword);
            }
            userService.updateUser(teacher.getId(), teacher);
        }
        return "redirect:/view/teacher/home";
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
        StudentDashboardDTO data = dashboardService.getStudentDashboard(studentId);
        model.addAttribute("student", data);
        return "studentHome";
    }

    // 2. Dashboard Detalhado (Prémios e Pontos)
    @GetMapping("/view/student/dashboard/{studentId}")
    public String studentDashboard(@PathVariable Long studentId, Model model) {
        StudentDashboardDTO data = dashboardService.getStudentDashboard(studentId);
        model.addAttribute("student", data);
        model.addAttribute("courseId", data.getCourseId());
        return "studentDashboard";
    }

    @GetMapping("/view/student/settings/{studentId}")
    public String studentSettings(@PathVariable Long studentId, Model model) {
        User student = userService.getUserById(studentId);
        model.addAttribute("student", student);
        return "studentSettings";
    }

    @PostMapping("/api/student/settings")
    public String updateStudentSettings(@RequestParam Long id,
                                        @RequestParam String name,
                                        @RequestParam(required = false) boolean notificationAwards,
                                        @RequestParam(required = false) boolean notificationRankings) {
        User student = userService.getUserById(id);
        if (student != null) {
            student.setName(name);
            student.setNotificationAwards(notificationAwards);
            student.setNotificationRankings(notificationRankings);
            userService.updateUser(id, student);
        }
        return "redirect:/view/student/home/" + id;
    }

    @GetMapping("/view/student/profile/{studentId}")
    public String editStudentProfile(@PathVariable Long studentId, Model model) {
        User student = userService.getUserById(studentId);
        model.addAttribute("student", student);
        return "editStudentProfile";
    }

    @PostMapping("/api/student/profile/update")
    public String updateStudentProfile(@RequestParam Long id,
                                     @RequestParam String name,
                                     @RequestParam String email,
                                     @RequestParam String newPassword,
                                     @RequestParam String currentPassword) {
        User student = userService.getUserById(id);
        if (student != null && student.getPassword().equals(currentPassword)) {
            student.setName(name);
            student.setEmail(email);
            if (newPassword != null && !newPassword.isEmpty()) {
                student.setPassword(newPassword);
            }
            userService.updateUser(id, student);
        }
        return "redirect:/view/student/home/" + id;
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