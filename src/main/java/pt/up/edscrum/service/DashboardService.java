package pt.up.edscrum.service;

import pt.up.edscrum.dto.dashboard.*;
import pt.up.edscrum.model.*;
import pt.up.edscrum.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final CourseRepository courseRepo;
    private final ProjectRepository projectRepo;
    private final TeamRepository teamRepo;
    private final StudentAwardRepository studentAwardRepo;
    private final UserRepository userRepo;
    private final ScoreRepository scoreRepo;

    public DashboardService(
        CourseRepository courseRepo,
        ProjectRepository projectRepo,
        TeamRepository teamRepo,
        StudentAwardRepository studentAwardRepo,
        UserRepository userRepo,
        ScoreRepository scoreRepo
    ) {
        this.courseRepo = courseRepo;
        this.projectRepo = projectRepo;
        this.teamRepo = teamRepo;
        this.studentAwardRepo = studentAwardRepo;
        this.userRepo = userRepo;
        this.scoreRepo = scoreRepo;
    }

    // ===================== DASHBOARD PROFESSOR =====================
    public TeacherDashboardDTO getTeacherDashboard(Long courseId) {

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Curso não encontrado"));

        TeacherDashboardDTO dto = new TeacherDashboardDTO();
        dto.setCourseId(course.getId());
        dto.setCourseName(course.getName());

        dto.setTotalStudents((int) course.getEnrollments().size()); // se precisares, ajusta
        dto.setTotalTeams((int) teamRepo.countByCourseId(courseId));
        dto.setTotalProjects((int) projectRepo.countByCourseId(courseId));

        dto.setProjects(
            projectRepo.findByCourseId(courseId).stream()
                .map(p -> {
                    ProjectProgressDTO pp = new ProjectProgressDTO();
                    pp.setProjectId(p.getId());
                    pp.setProjectName(p.getName());
                    pp.setCompletionPercentage(calculateProjectCompletion(p));
                    return pp;
                })
                .collect(Collectors.toList())
        );

        dto.setAwardStats(studentAwardRepo.countAwardsByCourse(courseId));

        return dto;
    }

    private double calculateProjectCompletion(Project project) {
        long total = project.getSprints().size();
        long done = project.getSprints().stream()
                .filter(s -> s.getStatus().name().equals("DONE"))
                .count();
        return total == 0 ? 0 : (done * 100.0 / total);
    }

    // ===================== DASHBOARD ESTUDANTE =====================
    public StudentDashboardDTO getStudentDashboard(Long studentId) {

        User user = userRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("User não encontrado"));

        StudentDashboardDTO dto = new StudentDashboardDTO();
        dto.setUserId(user.getId());
        dto.setName(user.getName());

        dto.setTotalPoints(scoreRepo.sumPointsByStudent(studentId));
        dto.setAwards(studentAwardRepo.findAwardsForStudent(studentId));
        dto.setPointHistory(scoreRepo.getPointHistory(studentId));

        return dto;
    }

    // ===================== RANKINGS =====================
    public List<RankingDTO> getStudentRanking(Long courseId) {
        return scoreRepo.getRankingForCourse(courseId);
    }

    public List<RankingDTO> getTeamRanking(Long courseId) {
        return scoreRepo.getTeamRanking(courseId);
    }
}
