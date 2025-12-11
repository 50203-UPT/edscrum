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
import pt.up.edscrum.model.Course;
import pt.up.edscrum.model.Enrollment;
import pt.up.edscrum.model.Project;
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

    public DashboardService(CourseRepository courseRepo, ProjectRepository projectRepo, TeamRepository teamRepo, TeamAwardRepository teamAwardRepo, StudentAwardRepository studentAwardRepo, UserRepository userRepo, ScoreRepository scoreRepo, EnrollmentRepository enrollmentRepo) {
        this.courseRepo = courseRepo;
        this.projectRepo = projectRepo;
        this.teamRepo = teamRepo;
        this.teamAwardRepo = teamAwardRepo;
        this.studentAwardRepo = studentAwardRepo;
        this.userRepo = userRepo;
        this.scoreRepo = scoreRepo;
        this.enrollmentRepo = enrollmentRepo;
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
                        .filter(s -> s.getStatus() != null && s.getStatus() == SprintStatus.DONE) // Comparação segura
                        .count();
                pp.setCompletionPercentage(total == 0 ? 0 : (done * 100.0 / total));
            }
            return pp;
        }).collect(Collectors.toList()));

        // Stats de prémios (Pode retornar lista vazia se a query no repo estiver incorreta, mas não crasha)
        try {
            dto.setAwardStats(studentAwardRepo.countAwardsByCourse(courseId));
        } catch (Exception e) {
            dto.setAwardStats(new ArrayList<>()); // Fallback seguro
        }

        return dto;
    }

    // ===================== DASHBOARD ESTUDANTE =====================
    @Transactional(readOnly = true)
    public StudentDashboardDTO getStudentDashboard(Long studentId) {

        User user = userRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("User não encontrado"));

        StudentDashboardDTO dto = new StudentDashboardDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setProfileImage(user.getProfileImage());

        // CORREÇÃO: Preencher campos do modal para evitar erro no Thymeleaf
        dto.setNotificationAwards(user.isNotificationAwards());
        dto.setNotificationRankings(user.isNotificationRankings());

        // 1. Calcular Pontos Totais
        int myScore = calculateTotalPointsForStudent(studentId);
        dto.setTotalPoints(myScore);

        dto.setEarnedAwards(studentAwardRepo.findFullAwardsForStudent(studentId));
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

        // 3. Estatísticas e Ranking
        if (!enrollments.isEmpty()) {
            Course mainCourse = enrollments.get(0).getCourse();
            dto.setCourseId(mainCourse.getId());
            dto.setCourseName(mainCourse.getName());

            List<RankingDTO> allRankings = getStudentRanking(mainCourse.getId());

            dto.setTotalClassStudents(allRankings.size());
            dto.setTotalClassTeams((int) teamRepo.countByCourseId(mainCourse.getId()));

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
        } else {
            dto.setTotalClassStudents(0);
            dto.setTopPerformerName("-");
            dto.setScoreVariation(0);
            dto.setClassAverage(0);
            dto.setCurrentRank(0);
            dto.setTopStudents(new ArrayList<>());
        }

        // 4. Equipa
        Team team = teamRepo.findTeamByUserId(studentId);
        List<Project> studentProjects = new ArrayList<>();
        if (team != null) {
            dto.setTeamName(team.getName());
            if (team.getScrumMaster() != null && team.getScrumMaster().getId().equals(studentId)) {
                dto.setRoleInTeam("Scrum Master");
            } else if (team.getProductOwner() != null && team.getProductOwner().getId().equals(studentId)) {
                dto.setRoleInTeam("Product Owner");
            } else {
                dto.setRoleInTeam("Developer");
            }
            if (team.getProjects() != null) {
                if (!team.getProjects().isEmpty()) {
                    dto.setProjectId(team.getProjects().get(0).getId());
                }
                studentProjects.addAll(team.getProjects());
            }
        }
        dto.setProjects(studentProjects);

        return dto;
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
        dto.setSprints(project.getSprints());
        dto.setCourseId(project.getCourse().getId());
        dto.setAvailableTeams(teamRepo.findAvailableTeamsByCourse(project.getCourse().getId()));

        // Dados da Equipa (Assumindo 1 equipa por projeto para simplificar visualização)
        if (project.getTeams() != null && !project.getTeams().isEmpty()) {
            Team team = project.getTeams().get(0);
            dto.setTeamName(team.getName());

            // 1. XP e Prémios da Equipa
            List<TeamAward> tAwards = teamAwardRepo.findByTeamId(team.getId());
            dto.setTeamAwards(tAwards);
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
}
