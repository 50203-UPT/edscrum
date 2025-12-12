package pt.up.edscrum.edscrum.Controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pt.up.edscrum.dto.dashboard.RankingDTO;
import pt.up.edscrum.model.Course;
import pt.up.edscrum.model.Enrollment;
import pt.up.edscrum.model.Score;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.CourseRepository;
import pt.up.edscrum.repository.EnrollmentRepository;
import pt.up.edscrum.repository.ScoreRepository;
import pt.up.edscrum.repository.TeamRepository;
import pt.up.edscrum.repository.UserRepository;
import pt.up.edscrum.service.DashboardService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DashboardControllerTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ScoreRepository scoreRepository;

    @Autowired
    private TeamRepository teamRepository;

    private Course testCourse;
    private User teacher;

    @BeforeEach
    void setUp() {
        // Limpar dados na ordem correta para respeitar as constraints
        scoreRepository.deleteAll();
        enrollmentRepository.deleteAll();
        // Se houver times associados aos cursos, remova-os primeiro
        if (teamRepository != null) {
            teamRepository.deleteAll();
        }
        courseRepository.deleteAll();
        userRepository.deleteAll();

        // Criar Teacher
        teacher = new User();
        teacher.setName("Test Teacher");
        teacher.setEmail("teacher@test.com");
        teacher.setRole("TEACHER");
        teacher = userRepository.save(teacher);

        // Criar Course associado ao teacher
        testCourse = new Course();
        testCourse.setName("Test Course");
        testCourse.setTeacher(teacher);
        testCourse = courseRepository.save(testCourse);
    }

    private User createAndSaveStudent(String name, String email) {
        User student = new User();
        student.setName(name);
        student.setEmail(email);
        student.setRole("STUDENT");
        return userRepository.save(student);
    }

    private void createAndSaveEnrollment(User student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.save(enrollment);
    }

    private void createAndSaveScore(User student, int points) {
        Score score = new Score();
        score.setUser(student);
        score.setTotalPoints(points);
        scoreRepository.save(score);
    }

    @Test
    void testGetStudentRankingsFlow() {
        // --- Criar estudantes e pontuações ---
        User student1 = createAndSaveStudent("Student A", "student.a@test.com");
        User student2 = createAndSaveStudent("Student B", "student.b@test.com");

        createAndSaveEnrollment(student1, testCourse);
        createAndSaveEnrollment(student2, testCourse);

        createAndSaveScore(student1, 150);
        createAndSaveScore(student2, 200);

        // --- Chamar o método correto do serviço ---
        List<RankingDTO> rankings = dashboardService.getStudentRanking(testCourse.getId());

        // --- Assert ---
        assertNotNull(rankings);
        assertEquals(2, rankings.size());

        // Verificar ordem decrescente
        assertEquals("Student B", rankings.get(0).getName());
        assertEquals(200L, rankings.get(0).getTotalPoints());
        assertEquals("Student A", rankings.get(1).getName());
        assertEquals(150L, rankings.get(1).getTotalPoints());
    }
}
