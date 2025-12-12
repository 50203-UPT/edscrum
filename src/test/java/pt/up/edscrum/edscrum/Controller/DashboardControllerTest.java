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

    private Course testCourse;
    private User teacher;

    @BeforeEach
    void setUp() {
        // Limpar dados na ordem correta para evitar FK violations
        scoreRepository.deleteAll();
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        // Criar Teacher e salvar antes de associar ao curso
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
        // Arrange: Criar dois estudantes, matriculá-los no curso e atribuir pontuações
        User student1 = createAndSaveStudent("Student A", "student.a@test.com");
        createAndSaveEnrollment(student1, testCourse);
        createAndSaveScore(student1, 150);

        User student2 = createAndSaveStudent("Student B", "student.b@test.com");
        createAndSaveEnrollment(student2, testCourse);
        createAndSaveScore(student2, 200);

        // Act: Chamar o método do serviço que retorna a lista de RankingDTOs
        List<RankingDTO> rankings = dashboardService.getStudentRanking(testCourse.getId());

        // Assert
        assertNotNull(rankings);
        assertEquals(2, rankings.size());
        // Verificar a ordem e conteúdo baseado no DTO
        assertEquals("Student B", rankings.get(0).getName());
        assertEquals(200, rankings.get(0).getTotalPoints());
        assertEquals("Student A", rankings.get(1).getName());
        assertEquals(150, rankings.get(1).getTotalPoints());
    }
}

