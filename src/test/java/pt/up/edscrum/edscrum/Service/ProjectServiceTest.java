package pt.up.edscrum.edscrum.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pt.up.edscrum.model.Course;
import pt.up.edscrum.model.Project;
import pt.up.edscrum.service.CourseService;
import pt.up.edscrum.service.ProjectService;

@SpringBootTest
@Transactional
class ProjectServiceTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private CourseService courseService;

    @Test
    void testCreateProjectWithCourse() {
        Course c = new Course();
        c.setName("Curso Teste");
        courseService.createCourse(c);

        Project p = new Project();
        p.setName("Projeto SDG");
        p.setSprintGoals("Implementar CRUDs");
        p.setCourse(c);

        Project saved = projectService.createProject(p);
        assertNotNull(saved.getId());
        assertEquals("Projeto SDG", saved.getName());
        assertEquals("Curso Teste", saved.getCourse().getName());
    }

    @Test
    void testCreateProjectInvalidDates() {
        Project p = new Project();
        p.setName("Projeto Datas");
        java.time.LocalDate today = java.time.LocalDate.now();
        p.setStartDate(today.plusDays(10));
        p.setEndDate(today.plusDays(5));

        assertThrows(ResponseStatusException.class, () -> projectService.createProject(p));
    }

    @Test
    void testCreateProjectStartNotAfterNow() {
        Project p = new Project();
        p.setName("Projeto Datas 2");
        java.time.LocalDate today = java.time.LocalDate.now();
        p.setStartDate(today);
        p.setEndDate(today.plusDays(5));

        assertThrows(ResponseStatusException.class, () -> projectService.createProject(p));
    }

    @Test
    void testUpdateProject() {
        Project p = new Project();
        p.setName("Antigo");
        p.setSprintGoals("Objetivo antigo");
        projectService.createProject(p);

        Project updated = new Project();
        updated.setName("Novo");
        updated.setSprintGoals("Objetivo novo");

        Project result = projectService.updateProject(p.getId(), updated);
        assertEquals("Novo", result.getName());
    }
}
