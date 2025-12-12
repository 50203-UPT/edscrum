/*package pt.up.edscrum.edscrum.Controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pt.up.edscrum.model.Course;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.CourseRepository;
import pt.up.edscrum.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class WebControllerTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    private User testTeacher;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        courseRepository.deleteAll();
        userRepository.deleteAll();

        // Create test teacher
        testTeacher = new User();
        testTeacher.setName("Test Teacher");
        testTeacher.setEmail("teacher@test.com");
        testTeacher.setRole("TEACHER");
        testTeacher = userRepository.save(testTeacher);
    }

    @Test
    void testGetCoursesByTeacher() {
        // Arrange: Create test courses
        Course course1 = new Course();
        course1.setName("Software Quality");
        course1.setTeacher(testTeacher);  // Set the teacher reference
        course1 = courseRepository.save(course1);  // Save and update the reference

        Course course2 = new Course();
        course2.setName("Data Structures");
        course2.setTeacher(testTeacher);  // Set the teacher reference
        course2 = courseRepository.save(course2);  // Save and update the reference

        // Act
        List<Course> teacherCourses = courseRepository.findByTeacherId(testTeacher.getId());

        // Assert
        assertNotNull(teacherCourses);
        assertEquals(2, teacherCourses.size());
        assertTrue(teacherCourses.stream().anyMatch(c -> c.getName().equals("Software Quality")));
        assertTrue(teacherCourses.stream().anyMatch(c -> c.getName().equals("Data Structures")));
    }

    @Test
    void testGetCoursesByTeacher_WhenNoCourses_ReturnsEmptyList() {
        // Act
        List<Course> teacherCourses = courseRepository.findByTeacherId(testTeacher.getId());

        // Assert
        assertNotNull(teacherCourses);
        assertTrue(teacherCourses.isEmpty());
    }
}*/