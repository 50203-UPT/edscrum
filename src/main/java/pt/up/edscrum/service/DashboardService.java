package pt.up.edscrum.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import pt.up.edscrum.dto.dashboard.ProjectProgressDTO;
import pt.up.edscrum.dto.dashboard.RankingDTO;
import pt.up.edscrum.dto.dashboard.StudentDashboardDTO;
import pt.up.edscrum.dto.dashboard.TeacherDashboardDTO;
import pt.up.edscrum.model.Course;
import pt.up.edscrum.model.Enrollment;
import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.CourseRepository;
import pt.up.edscrum.repository.EnrollmentRepository;
import pt.up.edscrum.repository.ProjectRepository;
import pt.up.edscrum.repository.ScoreRepository;
import pt.up.edscrum.repository.StudentAwardRepository;
import pt.up.edscrum.repository.TeamRepository;
import pt.up.edscrum.repository.UserRepository;

@Service
public class DashboardService {

    private final CourseRepository courseRepo;
    private final ProjectRepository projectRepo;
    private final TeamRepository teamRepo;
    private final StudentAwardRepository studentAwardRepo;
    private final UserRepository userRepo;
    private final ScoreRepository scoreRepo;
    private final EnrollmentRepository enrollmentRepo;

    public DashboardService(CourseRepository courseRepo, ProjectRepository projectRepo, TeamRepository teamRepo, StudentAwardRepository studentAwardRepo, UserRepository userRepo, ScoreRepository scoreRepo, EnrollmentRepository enrollmentRepo) {
        this.courseRepo = courseRepo;
        this.projectRepo = projectRepo;
        this.teamRepo = teamRepo;
        this.studentAwardRepo = studentAwardRepo;
        this.userRepo = userRepo;
        this.scoreRepo = scoreRepo;
        this.enrollmentRepo = enrollmentRepo;
    }

    // ===================== DASHBOARD PROFESSOR =====================
    public TeacherDashboardDTO getTeacherDashboard(Long courseId) {
        Course course = courseRepo.findById(courseId).orElseThrow(() -> new RuntimeException("Curso não encontrado"));
        TeacherDashboardDTO dto = new TeacherDashboardDTO();
        dto.setCourseId(course.getId());
        dto.setCourseName(course.getName());
        dto.setTotalStudents((int) course.getEnrollments().size());
        dto.setTotalTeams((int) teamRepo.countByCourseId(courseId));
        dto.setTotalProjects((int) projectRepo.countByCourseId(courseId));
        dto.setProjects(projectRepo.findByCourseId(courseId).stream().map(p -> {
            ProjectProgressDTO pp = new ProjectProgressDTO();
            pp.setProjectId(p.getId());
            pp.setProjectName(p.getName());
            // Lógica simples de progresso (podes melhorar depois)
            long total = p.getSprints().size();
            long done = p.getSprints().stream().filter(s -> s.getStatus().name().equals("DONE")).count();
            pp.setCompletionPercentage(total == 0 ? 0 : (done * 100.0 / total));
            return pp;
        }).collect(Collectors.toList()));
        dto.setAwardStats(studentAwardRepo.countAwardsByCourse(courseId));
        return dto;
    }

    // ===================== DASHBOARD ESTUDANTE =====================
    public StudentDashboardDTO getStudentDashboard(Long studentId) {

        User user = userRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("User não encontrado"));

        StudentDashboardDTO dto = new StudentDashboardDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setProfileImage(user.getProfileImage());
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

            // Buscar ranking real da turma
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

                // Minha Posição
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
            if (team.getProject() != null) {
                dto.setProjectId(team.getProject().getId());
                studentProjects.add(team.getProject());
            }
        }
        dto.setProjects(studentProjects);

        return dto;
    }

    // ===================== RANKINGS (LÓGICA CORRIGIDA) =====================
    // Este método agora calcula o ranking corretamente baseando-se nos inscritos
    public List<RankingDTO> getStudentRanking(Long courseId) {
        List<RankingDTO> classRanking = new ArrayList<>();

        // 1. Buscar todos os alunos inscritos no curso
        List<Enrollment> courseEnrollments = enrollmentRepo.findByCourseId(courseId);

        // 2. Para cada aluno, buscar o Score já calculado
        for (Enrollment e : courseEnrollments) {
            User u = e.getStudent();

            // Busca o score direto na tabela
            pt.up.edscrum.model.Score s = scoreRepo.findByUser(u);
            int totalP = (s != null) ? s.getTotalPoints() : 0;

            classRanking.add(new RankingDTO(u.getId(), u.getName(), (long) totalP));
        }

        // 3. Ordenar
        classRanking.sort((r1, r2) -> Long.compare(r2.getTotalPoints(), r1.getTotalPoints()));

        return classRanking;
    }

    // Método auxiliar (não é mais necessário somar tudo aqui, pois o Score já tem o total)
    // Mas mantemos caso precises do valor isolado
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
