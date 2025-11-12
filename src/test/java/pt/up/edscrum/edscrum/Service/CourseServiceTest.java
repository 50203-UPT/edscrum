package pt.up.edscrum.edscrum.Service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.model.Course;
import pt.up.edscrum.service.CourseService;

@SpringBootTest
@Transactional // para que os dados de teste sejam revertidos após cada teste
class CourseServiceTest {

    @Autowired
    private CourseService courseService;

    @Test
    void testCreateAndGetCourse() {
        Course c = new Course();
        c.setName("Engenharia de Software");
        c.setDescription("Curso de testes com Spring Boot");
        Course saved = courseService.createCourse(c);

        assertNotNull(saved.getId());
        assertEquals("Engenharia de Software", saved.getName());

        Course found = courseService.getCourseById(saved.getId());
        assertEquals(saved.getName(), found.getName());
    }

    @Test
    void testGetAllCourses() {
        Course c1 = new Course();
        c1.setName("Curso 1");
        courseService.createCourse(c1);

        Course c2 = new Course();
        c2.setName("Curso 2");
        courseService.createCourse(c2);

        List<Course> courses = courseService.getAllCourses();
        assertTrue(courses.size() >= 2);
    }

    @Test
    void testUpdateCourse() {
        Course c = new Course();
        c.setName("Antigo");
        c.setDescription("Descrição antiga");
        Course saved = courseService.createCourse(c);

        Course update = new Course();
        update.setName("Novo Nome");
        update.setDescription("Nova descrição");

        Course updated = courseService.updateCourse(saved.getId(), update);
        assertEquals("Novo Nome", updated.getName());
        assertEquals("Nova descrição", updated.getDescription());
    }

    @Test
    void testDeleteCourse() {
        Course c = new Course();
        c.setName("Curso para apagar");
        Course saved = courseService.createCourse(c);

        courseService.deleteCourse(saved.getId());

        // Está amarelo no IDE mas está correto, ele está se a queixar porque não faz nada depois. 
        assertThrows(RuntimeException.class, () -> courseService.getCourseById(saved.getId()));
    }
}
