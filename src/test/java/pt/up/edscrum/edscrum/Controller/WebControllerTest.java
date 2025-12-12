package pt.up.edscrum.edscrum.Controller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pt.up.edscrum.model.Course;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.CourseRepository;
import pt.up.edscrum.repository.UserRepository;
import pt.up.edscrum.service.CourseService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class WebControllerTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    private User testTeacher;

    @BeforeEach
    void setUp() {
        // Limpar dados na ordem correta para evitar FK violations
        courseRepository.deleteAll();
        userRepository.deleteAll();

        // Criar e salvar um teacher para ser usado nos testes
        testTeacher = new User();
        testTeacher.setName("Test Teacher");
        testTeacher.setEmail("teacher@test.com");
        testTeacher.setRole("TEACHER");
        testTeacher = userRepository.save(testTeacher);
    }

    @Test
    void testGetCoursesByTeacher_flow() {
        // Arrange: Criar dois cursos atribuídos ao teacher
        Course course1 = new Course();
        course1.setName("Software Quality");
        course1.setCode("QS");
        course1.setTeacher(testTeacher);
        courseRepository.save(course1);

        Course course2 = new Course();
        course2.setName("Data Structures");
        course2.setCode("ED");
        course2.setTeacher(testTeacher);
        courseRepository.save(course2);

        // Criar curso de outro teacher para garantir que a query é correta
        User otherTeacher = new User();
        otherTeacher.setName("Other Teacher");
        otherTeacher.setEmail("other@test.com");
        otherTeacher.setRole("TEACHER");
        otherTeacher = userRepository.save(otherTeacher);

        Course otherCourse = new Course();
        otherCourse.setName("Databases");
        otherCourse.setCode("BD");
        otherCourse.setTeacher(otherTeacher);
        courseRepository.save(otherCourse);

        // Act
        List<Course> teacherCourses = courseService.getCoursesByTeacher(testTeacher.getId());

        // Assert
        assertNotNull(teacherCourses);
        assertEquals(2, teacherCourses.size(), "Should only find courses for the test teacher");
        assertTrue(teacherCourses.stream().anyMatch(c -> c.getName().equals("Software Quality")));
        assertTrue(teacherCourses.stream().anyMatch(c -> c.getName().equals("Data Structures")));
        assertFalse(teacherCourses.stream().anyMatch(c -> c.getName().equals("Databases")));
    }

    @Test
    void testGetCoursesByTeacher_whenTeacherHasNoCourses_returnsEmptyList() {
        // Arrange: teacher sem cursos (já configurado no @BeforeEach)

        // Act
        List<Course> teacherCourses = courseService.getCoursesByTeacher(testTeacher.getId());

        // Assert
        assertNotNull(teacherCourses);
        assertTrue(teacherCourses.isEmpty(), "Should return an empty list for a teacher with no courses");
    }
}

