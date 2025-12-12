package pt.up.edscrum.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pt.up.edscrum.dto.dashboard.ProjectDetailsDTO;
import pt.up.edscrum.dto.dashboard.RankingDTO;
import pt.up.edscrum.dto.dashboard.StudentDashboardDTO;
import pt.up.edscrum.dto.dashboard.TeacherDashboardDTO;
import pt.up.edscrum.model.Course;
import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.Sprint;
import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.User;
import pt.up.edscrum.service.AuthService;
import pt.up.edscrum.service.AwardService;
import pt.up.edscrum.service.CourseService;
import pt.up.edscrum.service.DashboardService;
import pt.up.edscrum.service.ProjectService;
import pt.up.edscrum.service.SprintService;
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
    private final SprintService sprintService;

    public WebController(DashboardService dashboardService,
            AuthService authService,
            UserService userService,
            AwardService awardService,
            CourseService courseService,
            TeamService teamService,
            FileStorageService fileStorageService,
            ProjectService projectService,
            SprintService sprintService) {
        this.dashboardService = dashboardService;
        this.authService = authService;
        this.userService = userService;
        this.awardService = awardService;
        this.courseService = courseService;
        this.teamService = teamService;
        this.fileStorageService = fileStorageService;
        this.projectService = projectService;
        this.sprintService = sprintService;
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

    @GetMapping("/forgotPassword")
    public String forgotPasswordPage(@RequestParam(required = false) String success,
            @RequestParam(required = false) String email,
            Model model) {
        if ("true".equals(success) && email != null) {
            model.addAttribute("success", true);
            model.addAttribute("submittedEmail", email);
        }
        return "forgotPassword";
    }

    @PostMapping("/forgotPassword")
    public String handleForgotPassword(@RequestParam String email, Model model) {

        // CORREÇÃO: Adicionar .orElse(null)
        User user = userService.getUserByEmail(email).orElse(null);

        if (user == null) {
            model.addAttribute("error", "Email não existe na base de dados.");
            return "forgotPassword";
        }
        // Email exists, redirect to success
        return "redirect:/forgotPassword?success=true&email=" + email;
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

    // --- FLUXO DE RECUPERAÇÃO DE PALAVRA-PASSE ---
    // 1. PÁGINA "ESQUECEU A PALAVRA-PASSE"
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgotPassword";
    }

    // PROCESSAR ENVIO DE CÓDIGO (E REENVIO)
    @PostMapping("/auth/web/send-code")
    public String processSendCode(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        // Gera o código na BD
        boolean success = authService.generateResetCode(email);

        if (success) {
            // Passa o email para a próxima página (flash attribute esconde-o da URL mas mantém os dados)
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("successMessage", "Código enviado! Verifique a base de dados.");
            return "redirect:/verify-code";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Email não encontrado.");
            return "redirect:/forgot-password";
        }
    }

    // 2. PÁGINA "VERIFICAR CÓDIGO"
    @GetMapping("/verify-code")
    public String showVerifyCodePage(Model model) {
        // Se o email não vier do passo anterior, manda voltar ao início
        if (!model.containsAttribute("email")) {
            return "redirect:/forgot-password";
        }
        return "verifyCode";
    }

    // PROCESSAR VALIDAÇÃO DO CÓDIGO
    @PostMapping("/auth/web/verify-code")
    public String processVerifyCode(@RequestParam("email") String email,
            @RequestParam("code") String code,
            RedirectAttributes redirectAttributes) {

        boolean isValid = authService.validateResetCode(email, code);

        if (isValid) {
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("successMessage", "Código aceite. Defina a nova palavra-passe.");
            return "redirect:/reset-password";
        } else {
            // Se falhar, mantém o email na página para o user não ter de escrever tudo de novo
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("errorMessage", "Código incorreto. Tente novamente.");
            return "redirect:/verify-code";
        }
    }

    // 3. PÁGINA "REDEFINIR PALAVRA-PASSE"
    @GetMapping("/reset-password")
    public String showResetPasswordPage(Model model) {
        if (!model.containsAttribute("email")) {
            return "redirect:/forgot-password";
        }
        return "resetPassword";
    }

    // PROCESSAR MUDANÇA DE PASSWORD
    @PostMapping("/auth/web/change-password")
    public String processChangePassword(@RequestParam("email") String email,
            @RequestParam("newPassword") String newPassword,
            RedirectAttributes redirectAttributes) {

        authService.updatePassword(email, newPassword);
        // Mensagem de sucesso que aparecerá no ecrã de Login
        redirectAttributes.addFlashAttribute("successMessage", "Palavra-passe alterada com sucesso! Faça login.");
        return "redirect:/";
    }

    // --- ÁREA DO PROFESSOR ---
    // 1. Dashboard Geral (Home)
    @GetMapping("/view/teacher/home/{teacherId}")
    public String teacherHome(@PathVariable Long teacherId, Model model) {
        User teacher = userService.getUserById(teacherId);
        model.addAttribute("teacher", teacher);

        // 1. Buscar Cursos e Equipas
        List<Course> teacherCourses = courseService.getCoursesByTeacher(teacherId);
        List<Team> allTeams = teamService.getAllTeams();

        model.addAttribute("courses", teacherCourses);
        model.addAttribute("teams", allTeams);
        model.addAttribute("awards", awardService.getAllAwards());

        // --- Lista de Equipas Disponíveis (Agregada) ---
        List<Team> availableTeams = new ArrayList<>();
        if (teacherCourses != null) {
            for (Course c : teacherCourses) {
                // Para cada curso, busca as equipas livres e adiciona à lista geral
                availableTeams.addAll(teamService.getAvailableTeamsByCourse(c.getId()));
            }
        }
        model.addAttribute("availableTeams", availableTeams); // Envia para o modal

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

        // CORREÇÃO: Usar getAllUsers() em vez de getAllStudents() para garantir que todos aparecem no modal
        model.addAttribute("students", userService.getAllUsers());

        // Necessário para o bloqueio dinâmico no modal de criação
        model.addAttribute("takenMap", teamService.getTakenStudentsMap());

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
    @PostMapping("/courses/create")
    public String createCourseWeb(
            @ModelAttribute Course course,
            @RequestParam Long teacherId,
            RedirectAttributes redirectAttributes) {

        try {
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
            @ModelAttribute Project project, // Recebe name, sprintGoals, startDate, endDate
            @RequestParam Long courseId,
            @RequestParam Long teacherId,
            @RequestParam(required = false) Long teamId,
            RedirectAttributes redirectAttributes) {

        try {
            Course course = courseService.getCourseById(courseId);
            project.setCourse(course);

            // Define estado inicial
            project.setStatus(pt.up.edscrum.enums.ProjectStatus.PLANEAMENTO);

            // Grava o Projeto
            // Nota: As datas (startDate/endDate) vêm automaticamente no objeto 'project'
            Project savedProject = projectService.createProject(project);

            // Associar Equipa (Se selecionada)
            if (teamId != null) {
                Team team = teamService.getTeamById(teamId);
                team.setProject(savedProject);
                teamService.updateTeam(team.getId(), team);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Projeto '" + project.getName() + "' criado com sucesso!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao criar projeto: " + e.getMessage());
        }

        return "redirect:/view/teacher/home/" + teacherId;
    }

    // Apagar Projeto (Web)
    @PostMapping("/projects/delete")
    public String deleteProjectWeb(
            @RequestParam Long projectId,
            @RequestParam Long teacherId,
            RedirectAttributes redirectAttributes) {

        try {
            projectService.deleteProject(projectId);
            redirectAttributes.addFlashAttribute("successMessage", "Projeto eliminado e equipas desassociadas com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao eliminar projeto: " + e.getMessage());
            // Se der erro, volta para a página do projeto
            return "redirect:/view/project/" + projectId + "/user/" + teacherId;
        }

        // Se sucesso, volta para a dashboard do professor
        return "redirect:/view/teacher/home/" + teacherId;
    }

    // CRIAR SPRINT
    @PostMapping("/sprints/create")
    public String createSprintWeb(
            @RequestParam Long projectId,
            @RequestParam Long userId, // Para voltar à página certa
            @ModelAttribute Sprint sprint,
            RedirectAttributes redirectAttributes) {

        try {
            // Define estado inicial
            sprint.setStatus(pt.up.edscrum.enums.SprintStatus.TODO);

            // O SprintService já trata de associar ao projeto
            sprintService.createSprint(projectId, sprint);

            redirectAttributes.addFlashAttribute("successMessage", "Sprint criada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao criar sprint: " + e.getMessage());
        }

        // Redireciona de volta para os detalhes do projeto
        return "redirect:/view/project/" + projectId + "/user/" + userId;
    }

// 2. ATUALIZAR CREATE TEAM
    @PostMapping("/teams/create")
    public String createTeamWeb(
            @RequestParam String name,
            @RequestParam Long courseId,
            @RequestParam Long teacherId, // Vamos usar este ID para o redirecionamento
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long scrumMasterId,
            @RequestParam(required = false) Long productOwnerId,
            @RequestParam(required = false) List<Long> developerIds,
            RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.getCourseById(courseId);
            Team team = new Team();
            team.setName(name);
            team.setCourse(course);

            if (projectId != null) {
                Project project = projectService.getProjectById(projectId);
                team.setProject(project);
            }

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
            redirectAttributes.addFlashAttribute("successMessage", "Equipa criada com sucesso!");

            if (projectId != null) {
                return "redirect:/view/project/" + projectId + "/user/" + teacherId;
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro: " + e.getMessage());
            if (projectId != null) {
                return "redirect:/view/project/" + projectId + "/user/" + teacherId;
            }
        }
        return "redirect:/view/teacher/home/" + teacherId;
    }

    // --- Apagar Equipa ---
    @PostMapping("/teams/delete")
    public String deleteTeamWeb(
            @RequestParam Long teamId,
            @RequestParam Long teacherId,
            RedirectAttributes redirectAttributes) {

        try {
            // O serviço de equipas apaga a equipa pelo ID
            teamService.deleteTeam(teamId);

            redirectAttributes.addFlashAttribute("successMessage", "Equipa eliminada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao eliminar equipa: " + e.getMessage());
        }

        return "redirect:/view/teacher/home/" + teacherId;
    }

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

    @PostMapping("/api/teacher/settings")
    public String updateTeacherSettings(@RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) boolean notificationAwards,
            @RequestParam(required = false) boolean notificationRankings) {

        // CORREÇÃO: Adicionar .orElse(null) para extrair o User do Optional
        User teacher = userService.getUserByEmail(email).orElse(null);

        if (teacher != null) {
            teacher.setName(name);
            teacher.setNotificationAwards(notificationAwards);
            teacher.setNotificationRankings(notificationRankings);
            userService.updateUser(teacher.getId(), teacher);
            return "redirect:/view/teacher/home/" + teacher.getId();
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
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) String removeImage,
            RedirectAttributes redirectAttributes) {

        try {
            User teacher = userService.getUserById(teacherId);

            if (teacher == null || !teacher.getPassword().equals(currentPassword)) {
                throw new Exception("A password atual está incorreta.");
            }

            teacher.setName(name);
            teacher.setEmail(email);

            if (newPassword != null && !newPassword.isEmpty()) {
                teacher.setPassword(newPassword);
            }

            // Se o utilizador marcar para remover a imagem
            if ("true".equals(removeImage)) {
                teacher.setProfileImage(null);
            } else if (imageFile != null && !imageFile.isEmpty()) {
                // Se houver nova imagem, guardar
                String fileName = fileStorageService.saveFile(imageFile);
                teacher.setProfileImage(fileName);
            }

            userService.updateUser(teacherId, teacher);
            redirectAttributes.addFlashAttribute("successMessage", "Perfil atualizado com sucesso!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atualizar perfil: " + e.getMessage());
        }

        return "redirect:/view/teacher/home/" + teacherId;
    }

// 3. ATUALIZAR ASSIGN TEAM
    @PostMapping("/projects/assign-team")
    public String assignTeamToProject(
            @RequestParam Long projectId,
            @RequestParam Long teamId,
            @RequestParam Long userId, // Necessário para voltar à página
            RedirectAttributes redirectAttributes) {
        try {
            Project project = projectService.getProjectById(projectId);
            Team team = teamService.getTeamById(teamId);

            team.setProject(project);
            teamService.updateTeam(team.getId(), team);

            redirectAttributes.addFlashAttribute("successMessage", "Equipa associada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao associar equipa: " + e.getMessage());
        }
        return "redirect:/view/project/" + projectId + "/user/" + userId;
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
        return "redirect:/view/teacher/course/" + courseId;
    }

    // Método Atualizado: Agora recebe userId para carregar a Navbar correta
   @GetMapping("/view/project/{projectId}/user/{userId}")
    public String projectDetails(@PathVariable Long projectId, @PathVariable Long userId, Model model) {
        try {
            User user = userService.getUserById(userId);

            if ("TEACHER".equals(user.getRole())) {
                model.addAttribute("teacher", user);
            } else {
                model.addAttribute("student", user);
            }

            ProjectDetailsDTO project = dashboardService.getProjectDetails(projectId);
            model.addAttribute("project", project);

            // Carregar todos os utilizadores (Professores e Alunos)
            model.addAttribute("students", userService.getAllUsers());

            // --- NOVO: Lista de Alunos já ocupados neste curso ---
            // O project.getCourseId() vem do DTO
            java.util.Set<Long> takenIds = teamService.getTakenStudentIdsByCourse(project.getCourseId());
            model.addAttribute("takenStudentIds", takenIds);

            return "projectDetails";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/?error=project_error";
        }
    }

    @GetMapping("/view/sprint/{sprintId}/user/{userId}")
    public String sprintDashboard(@PathVariable Long sprintId, @PathVariable Long userId, Model model) {
        try {
            User user = userService.getUserById(userId);
            if ("TEACHER".equals(user.getRole())) {
                model.addAttribute("teacher", user);
            } else {
                model.addAttribute("student", user);
            }

            Sprint sprint = sprintService.getSprintById(sprintId);
            model.addAttribute("sprint", sprint);

            // Dados calculados
            int progress = sprintService.calculateSprintProgress(sprintId);
            model.addAttribute("sprintProgress", progress);

            // Contadores para o cabeçalho das colunas
            long todoCount = sprint.getUserStories().stream().filter(s -> s.getStatus().name().equals("TODO")).count();
            long progressCount = sprint.getUserStories().stream().filter(s -> s.getStatus().name().equals("IN_PROGRESS")).count();
            long testingCount = sprint.getUserStories().stream().filter(s -> s.getStatus().name().equals("TESTING")).count();
            long doneCount = sprint.getUserStories().stream().filter(s -> s.getStatus().name().equals("DONE")).count();

            model.addAttribute("todoCount", todoCount);
            model.addAttribute("progressCount", progressCount);
            model.addAttribute("testingCount", testingCount);
            model.addAttribute("doneCount", doneCount);
            model.addAttribute("totalStories", sprint.getUserStories().size());

            return "sprintDashboard";
        } catch (Exception e) {
            return "redirect:/?error=sprint_error";
        }
    }

    // API PARA O DRAG AND DROP (Retorna JSON simples)
    @PostMapping("/api/stories/{storyId}/move")
    @ResponseBody
    public ResponseEntity<?> moveStory(@PathVariable Long storyId, @RequestParam String status) {
        try {
            sprintService.updateUserStoryStatus(storyId, status);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- ÁREA DO ESTUDANTE (CORRIGIDA) ---
    @GetMapping("/view/student/home/{studentId}")
    public String studentHome(@PathVariable Long studentId, Model model) {
        try {
            // 1. Tenta obter os dados do serviço
            StudentDashboardDTO data = dashboardService.getStudentDashboard(studentId);

            // 2. Verifica se o resultado é nulo (segurança extra)
            if (data == null) {
                throw new RuntimeException("Dados do estudante não encontrados ou vazios.");
            }

            // 3. Adiciona ao modelo para o Thymeleaf
            model.addAttribute("student", data);

            // Sucesso: Retorna a view
            return "studentHome";

        } catch (Exception e) {
            // ERRO CRÍTICO ENCONTRADO
            // Imprime o erro na consola para diagnóstico (verifique os logs do seu IDE!)
            System.err.println("==========================================");
            System.err.println("ERRO AO ABRIR DASHBOARD DO ALUNO (ID: " + studentId + ")");
            e.printStackTrace(); // Mostra a linha exata onde falhou
            System.err.println("==========================================");

            // Redireciona para o login com mensagem de erro, em vez de tela branca
            return "redirect:/?error=erro_interno_consulte_logs";
        }
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
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) String removeImage,
            RedirectAttributes redirectAttributes) {

        try {
            User student = userService.getUserById(id);

            if (student != null && student.getPassword().equals(currentPassword)) {
                student.setName(name);
                student.setEmail(email);

                if (newPassword != null && !newPassword.isEmpty()) {
                    student.setPassword(newPassword);
                }

                // Se o utilizador marcar para remover a imagem
                if ("true".equals(removeImage)) {
                    student.setProfileImage(null);
                } else if (imageFile != null && !imageFile.isEmpty()) {
                    // Se houver nova imagem, guardar
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

    @GetMapping("/view/rankings/{courseId}")
    public String viewRankings(@PathVariable Long courseId, Model model) {
        model.addAttribute("studentRanking", dashboardService.getStudentRanking(courseId));
        model.addAttribute("teamRanking", dashboardService.getTeamRanking(courseId));
        model.addAttribute("courseId", courseId);
        return "rankings";
    }
}
