package pt.up.edscrum.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    /**
     * Página de login.
     *
     * @param model Modelo Thymeleaf para a vista
     * @return Nome da view de login
     */
    @GetMapping("/")
    public String loginPage(Model model) {
        return "index";
    }

    /**
     * Página de registo de utilizador.
     *
     * @return Nome da view de registo
     */
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    /**
     * Efetua logout redireccionando para a página principal.
     *
     * @return Redirecionamento para a página inicial
     */
    @GetMapping("/logout")
    public String logout() {
        return "redirect:/";
    }

    /**
     * Página para recuperação de password. Se o parâmetro 'success' for true e
     * o email for fornecido, mostra mensagem de sucesso.
     *
     * @param success Flag opcional indicando sucesso do envio do código
     * @param email Email submetido (opcional)
     * @param model Modelo para passar atributos à view
     * @return Nome da view de forgotPassword
     */
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

    /**
     * Processa o pedido de recuperação de password (envio de email/código).
     *
     * @param email Email do utilizador que solicitou recuperação
     * @param model Modelo para adicionar mensagens de erro/sucesso
     * @return View a apresentar (formulário ou redirecionamento)
     */
    @PostMapping("/forgotPassword")
    public String handleForgotPassword(@RequestParam String email, Model model) {

        User user = userService.getUserByEmail(email).orElse(null);

        if (user == null) {
            model.addAttribute("error", "Email não existe na base de dados.");
            return "forgotPassword";
        }
        return "redirect:/forgotPassword?success=true&email=" + email;
    }

    /**
     * Processa o login via web.
     *
     * @param email Email do utilizador
     * @param password Password do utilizador
     * @param model Modelo para mensagens de erro
     * @return Redirecionamento para a área do professor ou estudante conforme o
     * papel
     */
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

    /**
     * Processa o registo de novo utilizador.
     *
     * @param user Objeto User preenchido a partir do formulário
     * @return Redirecionamento para a página inicial com flag de registo
     */
    @PostMapping("/auth/web/register")
    public String webRegister(@ModelAttribute User user) {
        userService.createUser(user);
        return "redirect:/?registered=true";
    }

    /**
     * Mostra a página de esquecimento de password (rota alternativa).
     *
     * @return Nome da view forgotPassword
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgotPassword";
    }

    /**
     * Gera e envia (ou marca) um código de reset para o email indicado.
     *
     * @param email Email do utilizador
     * @param redirectAttributes Atributos flash para mensagens
     * @return Redirecionamento para a página apropriada conforme sucesso/falha
     */
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

    /**
     * Página para validar o código de recuperação.
     *
     * @param model Modelo usado para verificar presença do email
     * @return Nome da view verifyCode ou redirecionamento para forgot-password
     */
    @GetMapping("/verify-code")
    public String showVerifyCodePage(Model model) {
        // Se o email não vier do passo anterior, manda voltar ao início
        if (!model.containsAttribute("email")) {
            return "redirect:/forgot-password";
        }
        return "verifyCode";
    }

    /**
     * Processa a validação do código de recuperação.
     *
     * @param email Email do utilizador
     * @param code Código submetido
     * @param redirectAttributes Flash attributes para mensagens
     * @return Redirecionamento para a próxima etapa conforme resultado
     */
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

    /**
     * Página para redefinir a password após validação do código.
     *
     * @param model Modelo para verificar presença do email
     * @return Nome da view resetPassword ou redirecionamento caso falte email
     */
    @GetMapping("/reset-password")
    public String showResetPasswordPage(Model model) {
        if (!model.containsAttribute("email")) {
            return "redirect:/forgot-password";
        }
        return "resetPassword";
    }

    /**
     * Processa a alteração de password após reset.
     *
     * @param email Email do utilizador
     * @param newPassword Nova password a definir
     * @param redirectAttributes Atributos flash para mensagens
     * @return Redirecionamento para a página de login
     */
    @PostMapping("/auth/web/change-password")
    public String processChangePassword(@RequestParam("email") String email,
            @RequestParam("newPassword") String newPassword,
            RedirectAttributes redirectAttributes) {

        authService.updatePassword(email, newPassword);
        // Mensagem de sucesso que aparecerá no ecrã de Login
        redirectAttributes.addFlashAttribute("successMessage", "Palavra-passe alterada com sucesso! Faça login.");
        return "redirect:/";
    }

    // 1. Dashboard Geral (Home)
    /**
     * Exibe a dashboard principal do professor com cursos, equipas, prémios e
     * rankings.
     *
     * @param teacherId ID do professor
     * @param model Modelo para preencher a view teacherHome
     * @return Nome da view teacherHome
     */
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

        // Agrupar equipas por curso (para a vista de equipas organizadas por curso)
        Map<Long, List<Team>> teamsByCourse = allTeams.stream()
                .filter(t -> t.getCourse() != null)
                .collect(Collectors.groupingBy(t -> t.getCourse().getId()));
        model.addAttribute("teamsByCourse", teamsByCourse);

        List<Team> availableTeams = new ArrayList<>();
        if (teacherCourses != null) {
            for (Course c : teacherCourses) {
                // Para cada curso, busca as equipas livres e adiciona à lista geral
                availableTeams.addAll(teamService.getAvailableTeamsByCourse(c.getId()));
            }
        }
        model.addAttribute("availableTeams", availableTeams); // Envia para o modal

        List<RankingDTO> rankings = new ArrayList<>();

        // Valores padrão (para não dar erro se estiver vazio)
        model.addAttribute("totalStudents", 0);
        model.addAttribute("activeTeamsCount", 0);
        model.addAttribute("averageScore", 0);
        model.addAttribute("topPerformerName", "-");
        model.addAttribute("topPerformerScore", 0);
        model.addAttribute("scoreVariation", 0);

        if (!teacherCourses.isEmpty()) {
            // Agregar rankings de todos os cursos do professor, evitando duplicados
            java.util.Map<Long, RankingDTO> byStudent = new java.util.HashMap<>();
            for (Course c : teacherCourses) {
                List<RankingDTO> rlist = dashboardService.getStudentRanking(c.getId());
                for (RankingDTO r : rlist) {
                    RankingDTO existing = byStudent.get(r.getId());
                    if (existing == null) {
                        byStudent.put(r.getId(), new RankingDTO(r.getId(), r.getName(), r.getTotalPoints()));
                    } else {
                        // Mantém a maior pontuação encontrada (evita duplicados entre cursos)
                        if (r.getTotalPoints() > existing.getTotalPoints()) {
                            byStudent.put(r.getId(), new RankingDTO(r.getId(), r.getName(), r.getTotalPoints()));
                        }
                    }
                }
            }

            rankings = new ArrayList<>(byStudent.values());
            rankings.sort((r1, r2) -> Long.compare(r2.getTotalPoints(), r1.getTotalPoints()));

            // Se houver dados, calcula as estatísticas agregadas
            if (!rankings.isEmpty()) {
                model.addAttribute("totalStudents", rankings.size());
                List<Long> courseIds = teacherCourses.stream().map(Course::getId).toList();
                long courseTeams = allTeams.stream()
                        .filter(t -> t.getCourse() != null && courseIds.contains(t.getCourse().getId()))
                        .count();
                model.addAttribute("activeTeamsCount", courseTeams);

                double avg = rankings.stream().mapToLong(RankingDTO::getTotalPoints).average().orElse(0.0);
                model.addAttribute("averageScore", (int) avg);

                RankingDTO top = rankings.get(0);
                model.addAttribute("topPerformerName", top.getName());
                model.addAttribute("topPerformerScore", top.getTotalPoints());

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

        java.util.Map<Long, List<User>> enrolledStudentsMap = new java.util.HashMap<>();
        for (Course course : teacherCourses) {
            List<User> enrolledStudents = courseService.getEnrolledStudentsByCourse(course.getId());
            enrolledStudentsMap.put(course.getId(), enrolledStudents);
        }
        model.addAttribute("enrolledStudentsMap", enrolledStudentsMap);

        java.util.Map<Long, String> studentCoursesMap = new java.util.HashMap<>();
        for (java.util.Map.Entry<Long, List<User>> entry : enrolledStudentsMap.entrySet()) {
            Long courseId = entry.getKey();
            List<User> students = entry.getValue();
            for (User student : students) {
                String existingCourses = studentCoursesMap.getOrDefault(student.getId(), "");
                if (existingCourses.isEmpty()) {
                    studentCoursesMap.put(student.getId(), courseId.toString());
                } else {
                    studentCoursesMap.put(student.getId(), existingCourses + "," + courseId);
                }
            }
        }
        model.addAttribute("studentCoursesMap", studentCoursesMap);

        return "teacherHome";
    }

    // 2. Dashboard de Curso Específico
    /**
     * Mostra dashboard específico de um curso para o professor.
     *
     * @param courseId ID do curso
     * @param model Modelo para a view
     * @return Nome da view teacherDashboard
     */
    @GetMapping("/view/teacher/course/{courseId}")
    public String teacherDashboard(@PathVariable Long courseId, Model model) {
        TeacherDashboardDTO data = dashboardService.getTeacherDashboard(courseId);
        model.addAttribute("dashboard", data);
        model.addAttribute("allAwards", awardService.getAllAwards());
        model.addAttribute("allStudents", userService.getAllUsers());
        return "teacherDashboard";
    }

    /**
     * Cria um novo curso a partir do formulário web.
     *
     * @param course Objeto Course com dados do formulário
     * @param teacherId ID do professor criador
     * @param redirectAttributes Atributos flash para mensagens
     * @return Redirecionamento para a home do professor
     */
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

    /**
     * Cria um novo projeto associado a um curso (e opcionalmente a uma equipa).
     *
     * @param project Objeto Project preenchido pelo formulário
     * @param courseId ID do curso onde o projeto pertence
     * @param teacherId ID do professor que cria o projeto
     * @param teamId ID da equipa a associar (opcional)
     * @param redirectAttributes Atributos flash para mensagens
     * @return Redirecionamento para a home do professor
     */
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

    @PostMapping("/projects/delete")
    /**
     * Elimina um projeto e desassocia equipas.
     *
     * @param projectId ID do projeto a eliminar
     * @param teacherId ID do professor (para redirecionamento)
     * @param redirectAttributes Atributos flash para mensagens
     * @return Redirecionamento para a home do professor
     */
    public String deleteProjectWeb(
            @RequestParam Long projectId,
            @RequestParam Long teacherId,
            RedirectAttributes redirectAttributes) {

        try {
            projectService.deleteProject(projectId);
            redirectAttributes.addFlashAttribute("successMessage", "Projeto eliminado e equipas desassociadas com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao eliminar projeto: " + e.getMessage());
        }

        // Volta sempre para a dashboard do professor
        return "redirect:/view/teacher/home/" + teacherId;
    }

    /**
     * Cria um sprint para um projeto; se criado por um estudante, pode disparar
     * prémios.
     *
     * @param projectId ID do projeto
     * @param studentId ID do estudante que cria o sprint (opcional)
     * @param sprint Objeto Sprint com dados do formulário
     * @param redirectAttributes Atributos flash para mensagens
     * @return Redirecionamento para a home do estudante
     */
    @PostMapping("/sprints/create")
    public String createSprintWeb(
            @RequestParam Long projectId,
            @RequestParam(required = false) Long studentId, // Alunos criam sprints
            @ModelAttribute Sprint sprint,
            RedirectAttributes redirectAttributes) {

        try {
            // Define estado inicial
            sprint.setStatus(pt.up.edscrum.enums.SprintStatus.PLANEAMENTO);

            // Associa quem criou (se fornecido) e chama o serviço
            if (studentId != null) {
                sprint.setCreatedBy(userService.getUserById(studentId));
            }
            Sprint saved = sprintService.createSprint(projectId, sprint);

            // Trigger prémios automáticos relacionados com sprints
            if (studentId != null) {
                awardService.handleSprintCreated(studentId, projectId);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Sprint criada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao criar sprint: " + e.getMessage());
        }

        // Redireciona de volta para a página do aluno
        return "redirect:/view/student/home/" + studentId;
    }

    /**
     * Cria uma nova equipa, atribui papéis e pode atribuir prémios automáticos.
     *
     * @param name Nome da equipa
     * @param courseId ID do curso
     * @param teacherId ID do professor (para redirecionamento)
     * @param projectId ID do projeto associado (opcional)
     * @param scrumMasterId ID do Scrum Master (opcional)
     * @param productOwnerId ID do Product Owner (opcional)
     * @param developerIds Lista de IDs de developers (opcional)
     * @param redirectAttributes Atributos flash para mensagens
     * @return Redirecionamento para a aba de equipas na home do professor
     */
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

            // Prémios automáticos: Formaste a tua primeira equipa?
            try {
                if (scrumMasterId != null) {
                    List<Team> teamsOfSM = teamService.findTeamsByUserId(scrumMasterId);
                    if (teamsOfSM != null && teamsOfSM.size() == 1) {
                        awardService.assignAutomaticAwardToStudentByName("Arquiteto de Equipas", "Formaste a tua primeira equipa.", 30, scrumMasterId, null);
                    }
                    // Se o team está associado a um projeto, e é a primeira vez como líder num projeto
                    if (projectId != null) {
                        awardService.assignAutomaticAwardToStudentByName("Líder de Projeto (SM)", "Assumiste o papel de Scrum Master num projeto.", 80, scrumMasterId, projectId);
                    }
                }
                if (productOwnerId != null) {
                    List<Team> teamsOfPO = teamService.findTeamsByUserId(productOwnerId);
                    if (teamsOfPO != null && teamsOfPO.size() == 1) {
                        awardService.assignAutomaticAwardToStudentByName("Arquiteto de Equipas", "Formaste a tua primeira equipa.", 30, productOwnerId, null);
                    }
                    if (projectId != null) {
                        awardService.assignAutomaticAwardToStudentByName("Líder de Projeto (PO)", "Assumiste o papel de Product Owner num projeto.", 80, productOwnerId, projectId);
                    }
                }
            } catch (Exception e) {
                // Non-fatal
            }
            redirectAttributes.addFlashAttribute("successMessage", "Equipa criada com sucesso!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro: " + e.getMessage());
        }
        // Redireciona para teacherHome na aba de equipas
        return "redirect:/view/teacher/home/" + teacherId + "?tab=teams";
    }

    /**
     * Elimina uma equipa por ID.
     *
     * @param teamId ID da equipa a eliminar
     * @param teacherId ID do professor (para redirecionamento)
     * @param redirectAttributes Atributos flash para mensagens
     * @return Redirecionamento para a aba de equipas na home do professor
     */
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

        return "redirect:/view/teacher/home/" + teacherId + "?tab=teams";
    }

    /**
     * Cria um novo prémio manual definido pelo professor.
     *
     * @param award Objeto Award preenchido pelo formulário
     * @param teacherId ID do professor criador
     * @param redirectAttributes Atributos flash para mensagens
     * @return Redirecionamento para a aba de prémios na home do professor
     */
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

        return "redirect:/view/teacher/home/" + teacherId + "?tab=awards";
    }

    /**
     * Atualiza definições do professor (nome, notificações).
     *
     * @param name Nome a definir
     * @param email Email do professor (usado para identificar o registo)
     * @param notificationAwards Flag para notificações de prémios
     * @param notificationRankings Flag para notificações de rankings
     * @return Redirecionamento para a home do professor ou inicio se não
     * encontrado
     */
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

    /**
     * Atualiza o perfil do professor incluindo nome, email, password e imagem.
     *
     * @param teacherId ID do professor
     * @param name Nome a definir
     * @param email Email a definir
     * @param currentPassword Password atual (necessária apenas para mudar
     * password)
     * @param newPassword Nova password (opcional)
     * @param confirmNewPassword Confirmação da nova password
     * @param imageFile Ficheiro de imagem para atualizar o avatar (opcional)
     * @param removeImage Flag para remover imagem existente ('true' para
     * remover)
     * @param redirectAttributes Atributos flash para mensagens
     * @return Redirecionamento para a home do professor
     */
    @PostMapping("/teacher/profile/update")
    public String updateTeacherProfile(
            @RequestParam Long teacherId,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmNewPassword,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) String removeImage,
            RedirectAttributes redirectAttributes) {

        try {
            User teacher = userService.getUserById(teacherId);

            if (teacher != null) {
                // Only check current password if changing password
                if (newPassword != null && !newPassword.isEmpty()) {
                    if (currentPassword == null || !teacher.getPassword().equals(currentPassword)) {
                        throw new Exception("A password atual está incorreta.");
                    }
                    if (confirmNewPassword == null || !newPassword.equals(confirmNewPassword)) {
                        throw new Exception("Nova password e confirmação não coincidem.");
                    }
                    teacher.setPassword(newPassword);
                }

                teacher.setName(name);
                teacher.setEmail(email);

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
            } else {
                throw new Exception("Professor não encontrado.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atualizar perfil: " + e.getMessage());
        }

        return "redirect:/view/teacher/home/" + teacherId;
    }

// 3. ATUALIZAR ASSIGN TEAM
    /**
     * Associa uma equipa a um projeto.
     *
     * @param projectId ID do projeto
     * @param teamId ID da equipa
     * @param userId ID do utilizador para redirecionamento (normalmente
     * professor)
     * @param redirectAttributes Atributos flash para mensagens
     * @return Redirecionamento para a home do professor
     */
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
        // Redirecionar para a página do professor
        return "redirect:/view/teacher/home/" + userId;
    }

    /**
     * Ação para atribuir um prémio a um estudante.
     *
     * @param courseId ID do curso (para redirecionamento)
     * @param awardId ID do prémio
     * @param studentId ID do estudante
     * @return Redirecionamento para a página do curso
     */
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

    /**
     * Atribui um prémio a uma equipa num projeto.
     *
     * @param teamId ID da equipa
     * @param awardId ID do prémio
     * @param projectId ID do projeto
     * @param teacherId ID do professor (para redirecionamento)
     * @param redirectAttributes Atributos flash para mensagens
     * @return Redirecionamento para a home do professor
     */
    @PostMapping("/action/assign-award-to-team")
    public String assignAwardToTeamAction(@RequestParam Long teamId,
            @RequestParam Long awardId,
            @RequestParam Long projectId,
            @RequestParam Long teacherId, // Para redirecionamento
            RedirectAttributes redirectAttributes) {
        try {
            awardService.assignAwardToTeam(awardId, teamId, projectId);
            redirectAttributes.addFlashAttribute("successMessage", "Prémio atribuído à equipa com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atribuir prémio à equipa: " + e.getMessage());
        }
        return "redirect:/view/teacher/home/" + teacherId;
    }

    /**
     * Atribui um prémio a um estudante específico dentro de uma equipa.
     *
     * @param studentId ID do estudante
     * @param awardId ID do prémio
     * @param projectId ID do projeto
     * @param teacherId ID do professor (para redirecionamento)
     * @param redirectAttributes Atributos flash para mensagens
     * @return Redirecionamento para a home do professor
     */
    @PostMapping("/action/assign-award-to-student-in-team")
    public String assignAwardToStudentInTeamAction(@RequestParam Long studentId,
            @RequestParam Long awardId,
            @RequestParam Long projectId,
            @RequestParam Long teacherId, // Para redirecionamento
            RedirectAttributes redirectAttributes) {
        try {
            awardService.assignAwardToStudent(awardId, studentId, projectId);
            redirectAttributes.addFlashAttribute("successMessage", "Prémio atribuído ao aluno com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atribuir prémio ao aluno: " + e.getMessage());
        }
        return "redirect:/view/teacher/home/" + teacherId;
    }

    // Rota legacy - redireciona para a home correta (projectDetails foi removido)
    /**
     * Rota legacy que redireciona para a home apropriada conforme o papel do
     * utilizador.
     *
     * @param projectId ID do projeto (não usado para exibição direta)
     * @param userId ID do utilizador
     * @return Redirecionamento para a home do professor ou estudante
     */
    @GetMapping("/view/project/{projectId}/user/{userId}")
    public String projectDetailsRedirect(@PathVariable Long projectId, @PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId);

            if ("TEACHER".equals(user.getRole())) {
                return "redirect:/view/teacher/home/" + userId;
            } else {
                return "redirect:/view/student/home/" + userId;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/?error=project_error";
        }
    }

    /**
     * Exibe o dashboard de um sprint para um utilizador (professor ou
     * estudante).
     *
     * @param sprintId ID do sprint
     * @param userId ID do utilizador que visualiza
     * @param model Modelo para a view
     * @return Nome da view sprintDashboard ou redirecionamento em caso de erro
     */
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

            // Buscar membros da equipa do projeto
            Project project = sprint.getProject();
            if (project != null) {
                model.addAttribute("projectId", project.getId());
            }
            if (project != null && project.getTeams() != null && !project.getTeams().isEmpty()) {
                Team team = project.getTeams().get(0); // Pega a primeira equipa associada ao projeto
                List<User> teamMembers = teamService.getTeamMembers(team.getId());
                model.addAttribute("teamMembers", teamMembers);
            }

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

            // Verificar se todas as stories estão DONE
            boolean allDone = sprint.getUserStories().isEmpty()
                    || sprint.getUserStories().stream().allMatch(s -> s.getStatus().name().equals("DONE"));
            model.addAttribute("canComplete", allDone && !sprint.getUserStories().isEmpty());

            return "sprintDashboard";
        } catch (Exception e) {
            return "redirect:/?error=sprint_error";
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

            // 4. Adiciona contagem total de estudantes para exibição no dashboard
            // Usa UserService.getAllStudents() (retorna lista) e pega o tamanho.
            try {
                int totalStudents = userService.getAllStudents() != null ? userService.getAllStudents().size() : 0;
                model.addAttribute("studentCount", totalStudents);
            } catch (Exception ex) {
                model.addAttribute("studentCount", 0);
            }

            // Sucesso: Retorna a view
            return "studentHome";

        } catch (Exception e) {
            System.err.println("==========================================");
            System.err.println("ERRO AO ABRIR DASHBOARD DO ALUNO (ID: " + studentId + ")");
            e.printStackTrace();
            System.err.println("==========================================");

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
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmNewPassword,
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) String removeImage,
            RedirectAttributes redirectAttributes) {

        try {
            User student = userService.getUserById(id);

            if (student != null) {
                // Only check current password if changing password
                if (newPassword != null && !newPassword.isEmpty()) {
                    if (currentPassword == null || !student.getPassword().equals(currentPassword)) {
                        throw new Exception("Password atual incorreta.");
                    }
                    if (confirmNewPassword == null || !newPassword.equals(confirmNewPassword)) {
                        throw new Exception("Nova password e confirmação não coincidem.");
                    }
                    student.setPassword(newPassword);
                }

                student.setName(name);
                student.setEmail(email);

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
                throw new Exception("Estudante não encontrado.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro: " + e.getMessage());
        }

        return "redirect:/view/student/home/" + id;
    }

    @PostMapping("/api/student/enroll")
    public Object enrollStudent(
            @RequestParam Long studentId,
            @RequestParam Long courseId,
            @RequestParam(required = false) String accessCode,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            RedirectAttributes redirectAttributes) {

        boolean isAjax = "XMLHttpRequest".equals(requestedWith);

        try {
            // Obter nome do curso antes de inscrever
            Course course = courseService.getCourseById(courseId);
            String courseName = course != null ? course.getName() : "";

            // Chama o serviço do Dashboard (onde pusemos a lógica)
            dashboardService.enrollStudentInCourse(studentId, courseId, accessCode);

            // Se é um pedido AJAX, retornar sucesso com o nome do curso
            if (isAjax) {
                return ResponseEntity.ok().body(courseName);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Inscrição realizada com sucesso! Bem-vindo ao curso " + courseName + ".");
        } catch (IllegalArgumentException e) {
            // Se é um pedido AJAX, retornar erro HTTP para mostrar no modal
            if (isAjax) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }

            // Erros de código errado ou curso não encontrado
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            // Se é um pedido AJAX, retornar erro HTTP
            if (isAjax) {
                return ResponseEntity.status(500).body("Erro inesperado ao inscrever: " + e.getMessage());
            }

            // Outros erros genéricos
            redirectAttributes.addFlashAttribute("errorMessage", "Erro inesperado ao inscrever: " + e.getMessage());
        }

        return "redirect:/view/student/home/" + studentId + "?tab=all-courses";
    }

    @GetMapping("/view/rankings/{courseId}")
    public String viewRankings(@PathVariable Long courseId, Model model) {
        model.addAttribute("studentRanking", dashboardService.getStudentRanking(courseId));
        model.addAttribute("teamRanking", dashboardService.getTeamRanking(courseId));
        model.addAttribute("courseId", courseId);
        return "rankings";
    }
}
