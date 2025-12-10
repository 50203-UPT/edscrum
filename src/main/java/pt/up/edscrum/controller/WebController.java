package pt.up.edscrum.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping; // IMPORTANTE
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pt.up.edscrum.dto.dashboard.RankingDTO; // Importante
import pt.up.edscrum.dto.dashboard.StudentDashboardDTO; // Importante
import pt.up.edscrum.dto.dashboard.TeacherDashboardDTO; // Importante
import pt.up.edscrum.model.Course; // Importante
import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.User;
import pt.up.edscrum.service.AuthService; // IMPORTANTE
import pt.up.edscrum.service.AwardService;
import pt.up.edscrum.service.CourseService;
import pt.up.edscrum.service.DashboardService;
import pt.up.edscrum.service.ProjectService;
import pt.up.edscrum.service.TeamService;
import pt.up.edscrum.service.UserService;
import pt.up.edscrum.utils.FileStorageService;

@Controller
public class WebController {

    private final DashboardService dashboardService;
    private final AuthService authService;
    private final UserService userService;
    private final AwardService awardService;
    private final CourseService courseService;
    private final TeamService teamService;
    private final FileStorageService fileStorageService;
    private final ProjectService projectService;

    public WebController(DashboardService dashboardService,
            AuthService authService,
            UserService userService,
            AwardService awardService,
            CourseService courseService,
            TeamService teamService,
            FileStorageService fileStorageService,
            ProjectService projectService) {
        this.dashboardService = dashboardService;
        this.authService = authService;
        this.userService = userService;
        this.awardService = awardService;
        this.courseService = courseService;
        this.teamService = teamService;
        this.fileStorageService = fileStorageService;
        this.projectService = projectService;
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
            return "redirect:/view/teacher/home/" + user.getId();
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
    // 1. Dashboard Geral (Home)
    @GetMapping("/view/teacher/home/{teacherId}")
    public String teacherHome(@PathVariable Long teacherId, Model model) {
        User teacher = userService.getUserById(teacherId);
        model.addAttribute("teacher", teacher);

        // Dados Base
        List<Course> teacherCourses = courseService.getCoursesByTeacher(teacherId);
        List<Team> allTeams = teamService.getAllTeams();

        model.addAttribute("courses", teacherCourses);
        model.addAttribute("teams", allTeams);
        model.addAttribute("awards", awardService.getAllAwards());

        // --- ESTATÍSTICAS E RANKINGS ---
        List<RankingDTO> rankings = new ArrayList<>();

        // Valores padrão (para não dar erro se estiver vazio)
        model.addAttribute("totalStudents", 0);
        model.addAttribute("activeTeamsCount", 0);
        model.addAttribute("averageScore", 0);
        model.addAttribute("topPerformerName", "-");
        model.addAttribute("topPerformerScore", 0);
        model.addAttribute("scoreVariation", 0);

        if (!teacherCourses.isEmpty()) {
            Long firstCourseId = teacherCourses.get(0).getId();
            rankings = dashboardService.getStudentRanking(firstCourseId);

            // Se houver dados, calcula as estatísticas
            if (!rankings.isEmpty()) {
                // 1. Totais
                model.addAttribute("totalStudents", rankings.size());
                long courseTeams = allTeams.stream()
                        .filter(t -> t.getCourse() != null && t.getCourse().getId().equals(firstCourseId))
                        .count();
                model.addAttribute("activeTeamsCount", courseTeams);

                // 2. Média
                double avg = rankings.stream().mapToLong(RankingDTO::getTotalPoints).average().orElse(0.0);
                model.addAttribute("averageScore", (int) avg);

                // 3. Top Performer
                RankingDTO top = rankings.get(0);
                model.addAttribute("topPerformerName", top.getName());
                model.addAttribute("topPerformerScore", top.getTotalPoints());

                // 4. Variação
                RankingDTO bottom = rankings.get(rankings.size() - 1);
                long variation = top.getTotalPoints() - bottom.getTotalPoints();
                model.addAttribute("scoreVariation", variation);
            }
        }

        model.addAttribute("studentRankings", rankings);
        model.addAttribute("students", userService.getAllStudents()); // Para o modal de equipas

        return "teacherHome";
    }

    // 2. Dashboard de Curso Específico
    @GetMapping("/view/teacher/course/{courseId}")
    public String teacherDashboard(@PathVariable Long courseId, Model model) {
        TeacherDashboardDTO data = dashboardService.getTeacherDashboard(courseId);
        model.addAttribute("dashboard", data);
        model.addAttribute("allAwards", awardService.getAllAwards());
        model.addAttribute("allStudents", userService.getAllUsers());
        return "teacherDashboard";
    }

    // --- LÓGICA DE CRIAÇÃO (FORMULÁRIOS) ---
    // NOVO: Método para processar a criação de curso e voltar à mesma página
    @PostMapping("/courses/create")
    public String createCourseWeb(
            @ModelAttribute Course course,
            @RequestParam Long teacherId,
            RedirectAttributes redirectAttributes) {

        try {
            // NOVO: Buscar o professor e associá-lo ao curso
            User teacher = userService.getUserById(teacherId);
            course.setTeacher(teacher);

            courseService.createCourse(course);

            redirectAttributes.addFlashAttribute("successMessage", "Curso '" + course.getName() + "' criado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao criar curso: " + e.getMessage());
        }

        return "redirect:/view/teacher/home/" + teacherId;
    }

    @PostMapping("/projects/create")
    public String createProjectWeb(
            @ModelAttribute Project project,
            @RequestParam Long courseId,
            @RequestParam Long teacherId,
            @RequestParam(required = false) Long teamId, // NOVO: ID da Equipa opcional ou obrigatório
            RedirectAttributes redirectAttributes) {

        try {
            Course course = courseService.getCourseById(courseId);
            project.setCourse(course);

            // Define estado inicial
            project.setStatus(pt.up.edscrum.enums.ProjectStatus.PLANEAMENTO);

            // Grava o Projeto Primeiro
            Project savedProject = projectService.createProject(project);

            // Se uma equipa foi selecionada, associa-a ao projeto
            if (teamId != null) {
                Team team = teamService.getTeamById(teamId);
                team.setProject(savedProject);
                teamService.updateTeam(team.getId(), team); // Grava a alteração na equipa
            }

            redirectAttributes.addFlashAttribute("successMessage", "Projeto iniciado em Planeamento!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao criar projeto: " + e.getMessage());
        }

        return "redirect:/view/teacher/home/" + teacherId;
    }

    // --- NOVO MÉTODO PARA CRIAR EQUIPA (Para garantir que tem Curso) ---
    @PostMapping("/teams/create")
    public String createTeamWeb(
            @RequestParam String name,
            @RequestParam Long courseId,
            @RequestParam Long teacherId,
            @RequestParam(required = false) Long scrumMasterId,
            @RequestParam(required = false) Long productOwnerId,
            @RequestParam(required = false) List<Long> developerIds, // (Lista)
            RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.getCourseById(courseId);
            Team team = new Team();
            team.setName(name);
            team.setCourse(course);

            // Associar Membros se foram selecionados
            if (scrumMasterId != null) {
                team.setScrumMaster(userService.getUserById(scrumMasterId));
            }
            if (productOwnerId != null) {
                team.setProductOwner(userService.getUserById(productOwnerId));
            }
            if (developerIds != null && !developerIds.isEmpty()) {
                List<User> devs = new ArrayList<>();
                for (Long devId : developerIds) {
                    devs.add(userService.getUserById(devId));
                }
                team.setDevelopers(devs);
            }

            teamService.createTeam(team);
            redirectAttributes.addFlashAttribute("successMessage", "Equipa criada com membros associados!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro: " + e.getMessage());
        }
        return "redirect:/view/teacher/home/" + teacherId;
    }

    // --- Método para criar prémios ---
    @PostMapping("/awards/create")
    public String createAwardWeb(
            @ModelAttribute pt.up.edscrum.model.Award award,
            @RequestParam Long teacherId,
            RedirectAttributes redirectAttributes) {

        try {
            awardService.createAward(award);
            redirectAttributes.addFlashAttribute("successMessage", "Prémio '" + award.getName() + "' criado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao criar prémio: " + e.getMessage());
        }

        return "redirect:/view/teacher/home/" + teacherId;
    }

    // --- CONFIGURAÇÕES E PERFIL ---
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
            return "redirect:/view/teacher/home/" + teacher.getId(); // Redireciona corretamente
        }
        return "redirect:/";
    }

    @PostMapping("/teacher/profile/update")
    public String updateTeacherProfile(
            @RequestParam Long teacherId,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String newPassword,
            @RequestParam String currentPassword,
            @RequestParam(required = false) MultipartFile imageFile, // O ficheiro da imagem
            RedirectAttributes redirectAttributes) {

        try {
            User teacher = userService.getUserById(teacherId);

            // Verifica a password atual por segurança
            if (teacher == null || !teacher.getPassword().equals(currentPassword)) {
                throw new Exception("A password atual está incorreta.");
            }

            teacher.setName(name);
            teacher.setEmail(email);

            // Se houve nova password, atualiza
            if (newPassword != null && !newPassword.isEmpty()) {
                teacher.setPassword(newPassword);
            }

            // LOGICA DE IMAGEM: Se um ficheiro foi enviado, guarda-o
            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = fileStorageService.saveFile(imageFile);
                teacher.setProfileImage(fileName); // Guarda só o nome do ficheiro na BD
            }

            userService.updateUser(teacherId, teacher);
            redirectAttributes.addFlashAttribute("successMessage", "Perfil atualizado com sucesso!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atualizar perfil: " + e.getMessage());
        }

        return "redirect:/view/teacher/home/" + teacherId;
    }

    // --- AÇÕES DIVERSAS ---
    @PostMapping("/action/assign-award")
    public String assignAwardAction(@RequestParam Long courseId,
            @RequestParam Long awardId,
            @RequestParam Long studentId) {
        try {
            awardService.assignAwardToStudent(awardId, studentId);
        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
        }
        return "redirect:/view/teacher/course/" + courseId;
    }

    // --- ÁREA DO ESTUDANTE ---
    @GetMapping("/view/student/home/{studentId}")
    public String studentHome(@PathVariable Long studentId, Model model) {
        StudentDashboardDTO data = dashboardService.getStudentDashboard(studentId);
        model.addAttribute("student", data);
        return "studentHome";
    }

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
            @RequestParam String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) MultipartFile imageFile, // ADICIONAR ISTO
            RedirectAttributes redirectAttributes) {

        try {
            User student = userService.getUserById(id);

            // 1. Verificar Password Atual
            if (student != null && student.getPassword().equals(currentPassword)) {

                // 2. Atualizar Dados Básicos
                student.setName(name);
                student.setEmail(email);

                // 3. Atualizar Nova Password (se fornecida)
                if (newPassword != null && !newPassword.isEmpty()) {
                    student.setPassword(newPassword);
                }

                // 4. Lógica de Upload de Imagem (NOVO)
                if (imageFile != null && !imageFile.isEmpty()) {
                    String fileName = fileStorageService.saveFile(imageFile);
                    student.setProfileImage(fileName);
                }

                userService.updateUser(id, student);
                redirectAttributes.addFlashAttribute("successMessage", "Perfil atualizado com sucesso!");
            } else {
                throw new Exception("Password atual incorreta.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro: " + e.getMessage());
        }

        return "redirect:/view/student/home/" + id;
    }

    // Método para inscrever o aluno
    @PostMapping("/api/student/enroll")
    public String enrollStudent(@RequestParam Long studentId,
            @RequestParam Long courseId,
            RedirectAttributes redirectAttributes) {
        try {
            courseService.enrollStudent(courseId, studentId);
            redirectAttributes.addFlashAttribute("successMessage", "Inscrição realizada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao inscrever: " + e.getMessage());
        }
        return "redirect:/view/student/home/" + studentId;
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
