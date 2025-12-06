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
import pt.up.edscrum.service.DashboardService;
import pt.up.edscrum.service.UserService;

@Controller
public class WebController {

    private final DashboardService dashboardService;
    private final AuthService authService;
    private final UserService userService;
    private final AwardService awardService;

    // Injeção de dependências via construtor
    public WebController(DashboardService dashboardService,
            AuthService authService,
            UserService userService,
            AwardService awardService) {
        this.dashboardService = dashboardService;
        this.authService = authService;
        this.userService = userService;
        this.awardService = awardService;
    }

    // ==========================================
    // ÁREA PÚBLICA (LOGIN & REGISTO)
    // ==========================================
    @GetMapping("/")
    public String loginPage(Model model) {
        // Mostra a página de login (index.html)
        return "index";
    }

    @GetMapping("/register")
    public String registerPage() {
        // Mostra a página de registo (register.html)
        return "register";
    }

    @GetMapping("/logout")
    public String logout() {
        // Redireciona para a raiz (login)
        return "redirect:/";
    }

    @PostMapping("/auth/web/login")
    public String webLogin(@RequestParam String email,
            @RequestParam String password,
            Model model) {

        User user = authService.login(email, password);

        if (user == null) {
            model.addAttribute("error", "Credenciais inválidas! Tenta novamente.");
            return "index"; // Volta para o login com erro
        }

        // Lógica de redirecionamento baseada no Role
        if ("TEACHER".equals(user.getRole())) {
            // Nota: Num cenário real, deverias ter uma página para o professor escolher o curso.
            // Aqui assumimos o curso com ID 1 para demonstração.
            return "redirect:/view/teacher/1";
        } else {
            return "redirect:/view/student/" + user.getId();
        }
    }

    @PostMapping("/auth/web/register")
    public String webRegister(@ModelAttribute User user) {
        // Cria o utilizador e redireciona para o login com sucesso
        userService.createUser(user);
        return "redirect:/?registered=true";
    }

    // ==========================================
    // ÁREA DO PROFESSOR
    // ==========================================
    @GetMapping("/view/teacher/{courseId}")
    public String teacherDashboard(@PathVariable Long courseId, Model model) {
        // Busca os dados do dashboard
        TeacherDashboardDTO data = dashboardService.getTeacherDashboard(courseId);
        model.addAttribute("dashboard", data);

        // Dados para o formulário de atribuição de prémios
        model.addAttribute("allAwards", awardService.getAllAwards());
        model.addAttribute("allStudents", userService.getAllUsers()); // Idealmente, filtrar apenas alunos deste curso

        return "teacher-dashboard";
    }

    // Ação: Processar a atribuição de um prémio manual
    @PostMapping("/action/assign-award")
    public String assignAwardAction(@RequestParam Long courseId,
            @RequestParam Long awardId,
            @RequestParam Long studentId) {
        try {
            awardService.assignAwardToStudent(awardId, studentId);
        } catch (Exception e) {
            // Em caso de erro, podes adicionar um parâmetro na URL (ex: ?error=true)
            System.err.println("Erro ao atribuir prémio: " + e.getMessage());
        }
        // Recarrega a página do dashboard para mostrar os dados atualizados
        return "redirect:/view/teacher/" + courseId;
    }

    // ==========================================
    // ÁREA DO ESTUDANTE
    // ==========================================
    @GetMapping("/view/student/{studentId}")
    public String studentDashboard(@PathVariable Long studentId, Model model) {
        StudentDashboardDTO data = dashboardService.getStudentDashboard(studentId);
        model.addAttribute("student", data);

        // Se quiseres mostrar o ID do curso para criar links de navegação:
        model.addAttribute("courseId", data.getCourseId());

        return "student-dashboard";
    }

    // ==========================================
    // RANKINGS (Requisito do Projeto)
    // ==========================================
    @GetMapping("/view/rankings/{courseId}")
    public String viewRankings(@PathVariable Long courseId, Model model) {
        // Rankings de Estudantes
        model.addAttribute("studentRanking", dashboardService.getStudentRanking(courseId));

        // Rankings de Equipas
        model.addAttribute("teamRanking", dashboardService.getTeamRanking(courseId));

        model.addAttribute("courseId", courseId);

        return "rankings"; // Precisarás de criar rankings.html se quiseres ver esta página
    }
}
