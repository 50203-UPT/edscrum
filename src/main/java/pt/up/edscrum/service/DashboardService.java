package pt.up.edscrum.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // IMPORTANTE

import pt.up.edscrum.dto.dashboard.ProjectDetailsDTO;
import pt.up.edscrum.dto.dashboard.ProjectProgressDTO;
import pt.up.edscrum.dto.dashboard.RankingDTO;
import pt.up.edscrum.dto.dashboard.StudentDashboardDTO;
import pt.up.edscrum.dto.dashboard.TeacherDashboardDTO; // Importar Enum
import pt.up.edscrum.enums.SprintStatus;
import pt.up.edscrum.model.Award;
import pt.up.edscrum.model.Course;
import pt.up.edscrum.model.Enrollment;
import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.Sprint;
import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.TeamAward;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.CourseRepository;
import pt.up.edscrum.repository.EnrollmentRepository;
import pt.up.edscrum.repository.ProjectRepository;
import pt.up.edscrum.repository.ScoreRepository;
import pt.up.edscrum.repository.StudentAwardRepository;
import pt.up.edscrum.repository.TeamAwardRepository;
import pt.up.edscrum.repository.TeamRepository;
import pt.up.edscrum.repository.UserRepository;
import pt.up.edscrum.repository.UserStoryRepository;

@Service
public class DashboardService {

    private final CourseRepository courseRepo;
    private final ProjectRepository projectRepo;
    private final TeamRepository teamRepo;
    private final TeamAwardRepository teamAwardRepo;
    private final StudentAwardRepository studentAwardRepo;
    private final UserRepository userRepo;
    private final ScoreRepository scoreRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final AwardService awardService;
    private final SprintService sprintService;
    private final UserStoryRepository userStoryRepo;

    public DashboardService(CourseRepository courseRepo, ProjectRepository projectRepo, TeamRepository teamRepo, TeamAwardRepository teamAwardRepo, StudentAwardRepository studentAwardRepo, UserRepository userRepo, ScoreRepository scoreRepo, EnrollmentRepository enrollmentRepo, AwardService awardService, SprintService sprintService, UserStoryRepository userStoryRepo) {
        this.courseRepo = courseRepo;
        this.projectRepo = projectRepo;
        this.teamRepo = teamRepo;
        this.teamAwardRepo = teamAwardRepo;
        this.studentAwardRepo = studentAwardRepo;
        this.userRepo = userRepo;
        this.scoreRepo = scoreRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.awardService = awardService;
        this.sprintService = sprintService;
        this.userStoryRepo = userStoryRepo;
    }

    // ===================== DASHBOARD PROFESSOR =====================
    @Transactional(readOnly = true)
    public TeacherDashboardDTO getTeacherDashboard(Long courseId) {
        Course course = courseRepo.findById(courseId).orElseThrow(() -> new RuntimeException("Curso não encontrado"));
        TeacherDashboardDTO dto = new TeacherDashboardDTO();
        dto.setCourseId(course.getId());
        dto.setCourseName(course.getName());

        // Contagens
        dto.setTotalStudents(course.getEnrollments() != null ? course.getEnrollments().size() : 0);
        dto.setTotalTeams((int) teamRepo.countByCourseId(courseId));
        dto.setTotalProjects((int) projectRepo.countByCourseId(courseId));

        // Projetos e Progresso
        dto.setProjects(projectRepo.findByCourseId(courseId).stream().map(p -> {
            ProjectProgressDTO pp = new ProjectProgressDTO();
            pp.setProjectId(p.getId());
            pp.setProjectName(p.getName());

            // Lógica de progresso corrigida e segura contra Nulos
            if (p.getSprints() == null || p.getSprints().isEmpty()) {
                pp.setCompletionPercentage(0);
            } else {
                long total = p.getSprints().size();
                long done = p.getSprints().stream()
                        .filter(s -> s.getStatus() != null && s.getStatus() == SprintStatus.CONCLUIDO) // Comparação segura
                        .count();
                pp.setCompletionPercentage(total == 0 ? 0 : (done * 100.0 / total));
            }
            return pp;
        }).collect(Collectors.toList()));

        // Stats de prémios
        try {
            dto.setAwardStats(studentAwardRepo.countAwardsByCourse(courseId));
        } catch (Exception e) {
            dto.setAwardStats(new ArrayList<>()); // Fallback seguro
        }

        return dto;
    }

    // ===================== DASHBOARD ESTUDANTE (CORRIGIDO) =====================
    @Transactional(readOnly = true)
    public StudentDashboardDTO getStudentDashboard(Long studentId) {

        User user = userRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("User não encontrado"));

        StudentDashboardDTO dto = new StudentDashboardDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setProfileImage(user.getProfileImage());
        dto.setStudentTag(user.getStudentTag());

        dto.setNotificationAwards(user.isNotificationAwards());
        dto.setNotificationRankings(user.isNotificationRankings());

        // 1. Calcular Pontos Totais
        int myScore = calculateTotalPointsForStudent(studentId);
        dto.setTotalPoints(myScore);

        dto.setEarnedAwards(studentAwardRepo.findFullAwardsForStudent(studentId));

        // Calculate unearned awards
        List<StudentDashboardDTO.AwardDisplayDTO> earnedAwards = dto.getEarnedAwards();
        Set<String> earnedAwardNames = earnedAwards.stream().map(a -> a.name).collect(Collectors.toSet());
        List<Award> allAwards = awardService.getAllAwards();
        List<StudentDashboardDTO.AwardDisplayDTO> unearnedAwards = allAwards.stream()
                .filter(award -> !earnedAwardNames.contains(award.getName()))
                .map(award -> new StudentDashboardDTO.AwardDisplayDTO(award.getName(), award.getPoints(), award.getDescription(), award.getType()))
                .collect(Collectors.toList());
        dto.setUnearnedAwards(unearnedAwards);

        dto.setPointHistory(scoreRepo.getPointHistory(studentId));

        // 2. Separar Cursos
        List<Course> allCourses = courseRepo.findAll();
        List<Enrollment> enrollments = enrollmentRepo.findAllByStudent(user);
        Set<Long> enrolledIds = enrollments.stream().map(e -> e.getCourse().getId()).collect(Collectors.toSet());

        List<Course> enrolledList = new ArrayList<>();
        List<Course> availableList = new ArrayList<>();
        for (Course c : allCourses) {
            if (enrolledIds.contains(c.getId())) {
                enrolledList.add(c);
            } else {
                availableList.add(c);
            }
        }
        dto.setEnrolledCourses(enrolledList);
        dto.setAvailableCourses(availableList);

        // 3. Lógica Multi-Curso para Projetos e Equipas
        List<Project> allStudentProjects = new ArrayList<>();

        if (!enrollments.isEmpty()) {
            // Assume o primeiro curso como o "Principal" para mostrar no Dashboard (Rankings, etc)
            Course mainCourse = enrollments.get(0).getCourse();
            dto.setCourseId(mainCourse.getId());
            dto.setCourseName(mainCourse.getName());

            // Preenche estatísticas apenas para o curso principal
            fillCourseStatistics(dto, mainCourse.getId(), studentId);
        } else {
            clearCourseStatistics(dto);
        }

        // 4. Procurar Equipas e Projetos em TODAS as inscrições
        for (Enrollment enrollment : enrollments) {
            Long currentCourseId = enrollment.getCourse().getId();

            // Usa o novo método do repositório para buscar a equipa específica DESTE curso
            teamRepo.findTeamByCourseAndUser(currentCourseId, studentId).ifPresent(team -> {
                
                // Se a equipa for do curso que estamos a mostrar como "Principal", preenche o cabeçalho
                if (dto.getCourseId() != null && dto.getCourseId().equals(currentCourseId)) {
                    dto.setTeamName(team.getName());
                    dto.setRoleInTeam(getRoleInTeam(team, studentId));
                }

                // Se a equipa tiver projeto, adiciona à lista
                if (team.getProject() != null) {
                    allStudentProjects.add(team.getProject());
                }
            });
        }
        
        // 5. Converter Projects para ProjectWithProgressDTO com cálculo de progresso
        List<pt.up.edscrum.dto.dashboard.ProjectWithProgressDTO> projectsWithProgress = new ArrayList<>();
        for (Project project : allStudentProjects) {
            projectsWithProgress.add(convertProjectToDTO(project));
        }
        dto.setProjects(projectsWithProgress);

        return dto;
    }

    // ===================== NOVO MÉTODO: INSCRIÇÃO COM CÓDIGO =====================
    @Transactional
    public void enrollStudentInCourse(Long studentId, Long courseId, String accessCode) {
        // 1. Buscar o Curso
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado."));

        // 2. VERIFICAÇÃO DO CÓDIGO (SEGURANÇA)
        // Se o curso tiver código definido, o input tem de ser igual.
        if (course.getCode() != null && !course.getCode().trim().isEmpty()) {
            if (accessCode == null || !course.getCode().equals(accessCode.trim())) {
                throw new IllegalArgumentException("O código de acesso está incorreto.");
            }
        }

        // 3. Buscar o Aluno
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado."));

        // 4. Verificar se já está inscrito (Defesa extra)
        // Nota: Certifica-te que atualizaste o EnrollmentRepository como pedido no Passo 1
        if (enrollmentRepo.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new IllegalArgumentException("Já estás inscrito neste curso.");
        }

        // 5. Criar e Salvar Inscrição
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setStudent(student);
        enrollmentRepo.save(enrollment);
    }

    @Transactional(readOnly = true)
    public ProjectDetailsDTO getProjectDetails(Long projectId) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        ProjectDetailsDTO dto = new ProjectDetailsDTO();

        // Cabeçalho
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getSprintGoals());
        dto.setCourseName(project.getCourse().getName());
        dto.setStatus(project.getStatus().name());
        dto.setStartDate(project.getStartDate());
        dto.setEndDate(project.getEndDate());
        dto.setCourseId(project.getCourse().getId());
        
        // Converter sprints para DTO simples
        List<ProjectDetailsDTO.SprintDTO> sprintDTOs = new ArrayList<>();
        if (project.getSprints() != null) {
            for (Sprint s : project.getSprints()) {
                sprintDTOs.add(new ProjectDetailsDTO.SprintDTO(
                    s.getId(), s.getName(), s.getStatus().name(), s.getStartDate(), s.getEndDate()
                ));
            }
        }
        dto.setSprints(sprintDTOs);
        
        // Converter equipas disponíveis para DTO simples
        List<Team> availableTeams = teamRepo.findAvailableTeamsByCourse(project.getCourse().getId());
        List<ProjectDetailsDTO.AvailableTeamDTO> availableTeamDTOs = new ArrayList<>();
        for (Team t : availableTeams) {
            availableTeamDTOs.add(new ProjectDetailsDTO.AvailableTeamDTO(t.getId(), t.getName()));
        }
        dto.setAvailableTeams(availableTeamDTOs);

        // Dados da Equipa (Assumindo 1 equipa por projeto para simplificar visualização)
        if (project.getTeams() != null && !project.getTeams().isEmpty()) {
            Team team = project.getTeams().get(0);
            dto.setTeamId(team.getId());
            dto.setTeamName(team.getName());

            // 1. XP e Prémios da Equipa
            List<TeamAward> tAwards = teamAwardRepo.findByTeamId(team.getId());
            List<ProjectDetailsDTO.TeamAwardDTO> awardDTOs = new ArrayList<>();
            for (TeamAward ta : tAwards) {
                awardDTOs.add(new ProjectDetailsDTO.TeamAwardDTO(
                    ta.getAward() != null ? ta.getAward().getName() : "Prémio",
                    ta.getPointsEarned()
                ));
            }
            dto.setTeamAwards(awardDTOs);
            int teamScore = tAwards.stream().mapToInt(TeamAward::getPointsEarned).sum();
            dto.setTeamTotalXP(teamScore);

            // 2. Membros e seus XPs individuais
            List<ProjectDetailsDTO.TeamMemberDTO> members = new ArrayList<>();

            // Helper para criar DTO de membro
            if (team.getScrumMaster() != null) {
                User u = team.getScrumMaster();
                members.add(createMemberDTO(u, "Scrum Master"));
            }
            if (team.getProductOwner() != null) {
                User u = team.getProductOwner();
                members.add(createMemberDTO(u, "Product Owner"));
            }
            for (User dev : team.getDevelopers()) {
                members.add(createMemberDTO(dev, "Developer"));
            }
            dto.setMembers(members);
        }

        // Calcular progresso do projeto (baseado em User Stories) - usando repositório para evitar LazyInit
        int totalStories = 0;
        int completedStories = 0;
        if (project.getSprints() != null) {
            for (Sprint sprint : project.getSprints()) {
                List<pt.up.edscrum.model.UserStory> stories = userStoryRepo.findBySprintId(sprint.getId());
                totalStories += stories.size();
                completedStories += (int) stories.stream()
                        .filter(us -> "DONE".equals(us.getStatus().name()))
                        .count();
            }
        }
        dto.setTotalStories(totalStories);
        dto.setCompletedStories(completedStories);
        dto.setProgressPercentage(totalStories > 0 ? (completedStories * 100) / totalStories : 0);

        return dto;
    }

    private ProjectDetailsDTO.TeamMemberDTO createMemberDTO(User u, String role) {
        int xp = calculateTotalPointsForStudent(u.getId()); // Reutiliza método existente
        int awardsCount = studentAwardRepo.findAllByStudentId(u.getId()).size();
        return new ProjectDetailsDTO.TeamMemberDTO(u.getId(), u.getName(), role, xp, awardsCount);
    }

    public List<RankingDTO> getStudentRanking(Long courseId) {
        List<RankingDTO> classRanking = new ArrayList<>();
        List<Enrollment> courseEnrollments = enrollmentRepo.findByCourseId(courseId);

        for (Enrollment e : courseEnrollments) {
            User u = e.getStudent();
            pt.up.edscrum.model.Score s = scoreRepo.findByUser(u);
            int totalP = (s != null) ? s.getTotalPoints() : 0;
            classRanking.add(new RankingDTO(u.getId(), u.getName(), (long) totalP));
        }
        classRanking.sort((r1, r2) -> Long.compare(r2.getTotalPoints(), r1.getTotalPoints()));
        return classRanking;
    }

    private int calculateTotalPointsForStudent(Long studentId) {
        User u = userRepo.findById(studentId).orElse(null);
        if (u == null) {
            return 0;
        }
        pt.up.edscrum.model.Score s = scoreRepo.findByUser(u);
        return (s != null) ? s.getTotalPoints() : 0;
    }

    public List<RankingDTO> getTeamRanking(Long courseId) {
        return scoreRepo.getTeamRanking(courseId);
    }

    // ===================== HELPER METHODS =====================
    
    private String getRoleInTeam(Team team, Long studentId) {
        if (team.getScrumMaster() != null && team.getScrumMaster().getId().equals(studentId)) return "Scrum Master";
        if (team.getProductOwner() != null && team.getProductOwner().getId().equals(studentId)) return "Product Owner";
        return "Developer";
    }

    private void fillCourseStatistics(StudentDashboardDTO dto, Long courseId, Long studentId) {
        List<RankingDTO> allRankings = getStudentRanking(courseId);
        dto.setTotalClassStudents(allRankings.size());
        dto.setTotalClassTeams((int) teamRepo.countByCourseId(courseId));

        if (!allRankings.isEmpty()) {
            RankingDTO top = allRankings.get(0);
            dto.setTopPerformerName(top.getName());
            dto.setTopPerformerScore(top.getTotalPoints().intValue());

            RankingDTO bottom = allRankings.get(allRankings.size() - 1);
            dto.setScoreVariation((int) (top.getTotalPoints() - bottom.getTotalPoints()));

            double avg = allRankings.stream().mapToLong(RankingDTO::getTotalPoints).average().orElse(0.0);
            dto.setClassAverage(avg);

            int myRank = 0;
            for (int i = 0; i < allRankings.size(); i++) {
                if (allRankings.get(i).getId().equals(studentId)) {
                    myRank = i + 1;
                    break;
                }
            }
            dto.setCurrentRank(myRank > 0 ? myRank : allRankings.size());
            dto.setTopStudents(allRankings.stream().limit(5).collect(Collectors.toList()));
        }
    }

    private void clearCourseStatistics(StudentDashboardDTO dto) {
        dto.setTotalClassStudents(0);
        dto.setTopPerformerName("-");
        dto.setScoreVariation(0);
        dto.setClassAverage(0);
        dto.setCurrentRank(0);
        dto.setTopStudents(new ArrayList<>());
    }

    // Método para converter Project em ProjectWithProgressDTO
    private pt.up.edscrum.dto.dashboard.ProjectWithProgressDTO convertProjectToDTO(Project project) {
        pt.up.edscrum.dto.dashboard.ProjectWithProgressDTO dto = new pt.up.edscrum.dto.dashboard.ProjectWithProgressDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setSprintGoals(project.getSprintGoals());
        dto.setStartDate(project.getStartDate());
        dto.setEndDate(project.getEndDate());
        dto.setStatus(project.getStatus());

        // Converter sprints e calcular progresso do projeto
        List<pt.up.edscrum.dto.dashboard.SprintWithProgressDTO> sprintsWithProgress = new ArrayList<>();
        int totalSprintProgress = 0;
        int sprintCount = 0;

        if (project.getSprints() != null && !project.getSprints().isEmpty()) {
            for (Sprint sprint : project.getSprints()) {
                pt.up.edscrum.dto.dashboard.SprintWithProgressDTO sprintDTO = convertSprintToDTO(sprint);
                sprintsWithProgress.add(sprintDTO);
                totalSprintProgress += sprintDTO.getProgress();
                sprintCount++;
            }
            // Progresso do projeto é a média dos progressos dos sprints
            dto.setProgress(sprintCount > 0 ? totalSprintProgress / sprintCount : 0);
        } else {
            dto.setProgress(0);
        }

        dto.setSprints(sprintsWithProgress);

        // Adicionar membros da equipa com roles (ordenados: PO, SM, Devs)
        List<pt.up.edscrum.dto.dashboard.MemberWithRoleDTO> projectMembers = new ArrayList<>();
        if (project.getTeams() != null) {
            for (Team team : project.getTeams()) {
                // Adicionar Product Owner primeiro (Azul)
                if (team.getProductOwner() != null) {
                    User po = team.getProductOwner();
                    int awardsCount = studentAwardRepo.findAllByStudentId(po.getId()).size();
                    int xp = studentAwardRepo.findAllByStudentId(po.getId()).stream()
                            .mapToInt(sa -> sa.getPointsEarned()).sum();
                    pt.up.edscrum.dto.dashboard.MemberWithRoleDTO poMember = new pt.up.edscrum.dto.dashboard.MemberWithRoleDTO(
                        po.getId(), 
                        po.getName(), 
                        "Product Owner",
                        awardsCount,
                        xp
                    );
                    if (projectMembers.stream().noneMatch(m -> m.getId().equals(poMember.getId()))) {
                        projectMembers.add(poMember);
                    }
                }
                // Adicionar Scrum Master segundo (Roxo)
                if (team.getScrumMaster() != null) {
                    User sm = team.getScrumMaster();
                    int awardsCount = studentAwardRepo.findAllByStudentId(sm.getId()).size();
                    int xp = studentAwardRepo.findAllByStudentId(sm.getId()).stream()
                            .mapToInt(sa -> sa.getPointsEarned()).sum();
                    pt.up.edscrum.dto.dashboard.MemberWithRoleDTO smMember = new pt.up.edscrum.dto.dashboard.MemberWithRoleDTO(
                        sm.getId(), 
                        sm.getName(), 
                        "Scrum Master",
                        awardsCount,
                        xp
                    );
                    if (projectMembers.stream().noneMatch(m -> m.getId().equals(smMember.getId()))) {
                        projectMembers.add(smMember);
                    }
                }
                // Adicionar Developers por último (Verde)
                if (team.getDevelopers() != null) {
                    for (User developer : team.getDevelopers()) {
                        int awardsCount = studentAwardRepo.findAllByStudentId(developer.getId()).size();
                        int xp = studentAwardRepo.findAllByStudentId(developer.getId()).stream()
                                .mapToInt(sa -> sa.getPointsEarned()).sum();
                        pt.up.edscrum.dto.dashboard.MemberWithRoleDTO devMember = new pt.up.edscrum.dto.dashboard.MemberWithRoleDTO(
                            developer.getId(), 
                            developer.getName(), 
                            "Developer",
                            awardsCount,
                            xp
                        );
                        if (projectMembers.stream().noneMatch(m -> m.getId().equals(devMember.getId()))) {
                            projectMembers.add(devMember);
                        }
                    }
                }
            }
        }
        dto.setMembers(projectMembers);

        return dto;
    }

    // Método para converter Sprint em SprintWithProgressDTO
    private pt.up.edscrum.dto.dashboard.SprintWithProgressDTO convertSprintToDTO(Sprint sprint) {
        pt.up.edscrum.dto.dashboard.SprintWithProgressDTO dto = new pt.up.edscrum.dto.dashboard.SprintWithProgressDTO();
        dto.setId(sprint.getId());
        dto.setName(sprint.getName());
        dto.setDescription(sprint.getDescription());
        dto.setStartDate(sprint.getStartDate());
        dto.setEndDate(sprint.getEndDate());
        dto.setStatus(sprint.getStatus());

        // Calcular progresso com base nas user stories
        dto.setProgress(sprintService.calculateSprintProgress(sprint.getId()));

        return dto;
    }
}