package pt.up.edscrum.edscrum.Controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.model.Course;
import pt.up.edscrum.model.Project;
import pt.up.edscrum.service.CourseService;
import pt.up.edscrum.service.ProjectService;

@SpringBootTest
@Transactional
class ProjectControllerTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private CourseService courseService;

    @Test
    void testCreateAndGetProject() {
        // Criar um curso associado
        Course c = new Course();
        c.setName("Engenharia de Software");
        c.setDescription("Curso associado ao projeto");
        Course savedCourse = courseService.createCourse(c);

        // Criar projeto
        Project p = new Project();
        p.setName("EduScrum Platform");
        p.setSprintGoals("Melhorar o fluxo das sprints");
        p.setCourse(savedCourse);

        Project saved = projectService.createProject(p);

        assertNotNull(saved.getId());
        assertEquals("EduScrum Platform", saved.getName());
        assertEquals("Melhorar o fluxo das sprints", saved.getSprintGoals());
        assertNotNull(saved.getCourse());
        assertEquals(savedCourse.getName(), saved.getCourse().getName());

        // Buscar o mesmo projeto
        Project found = projectService.getProjectById(saved.getId());
        assertEquals(saved.getName(), found.getName());
        assertEquals(saved.getSprintGoals(), found.getSprintGoals());
    }

    @Test
    void testGetAllProjects() {
        Project p1 = new Project();
        p1.setName("Projeto A");
        projectService.createProject(p1);

        Project p2 = new Project();
        p2.setName("Projeto B");
        projectService.createProject(p2);

        List<Project> projects = projectService.getAllProjects();
        assertTrue(projects.size() >= 2);
    }

    @Test
    void testUpdateProject() {
        Project p = new Project();
        p.setName("Versão Antiga");
        p.setSprintGoals("Objetivo antigo");
        Project saved = projectService.createProject(p);

        Project update = new Project();
        update.setName("Versão Atualizada");
        update.setSprintGoals("Novo objetivo da sprint");

        Project updated = projectService.updateProject(saved.getId(), update);

        assertEquals("Versão Atualizada", updated.getName());
        assertEquals("Novo objetivo da sprint", updated.getSprintGoals());
    }

    @Test
    void testDeleteProject() {
        Project p = new Project();
        p.setName("Projeto a eliminar");
        Project saved = projectService.createProject(p);

        projectService.deleteProject(saved.getId());

        assertThrows(RuntimeException.class, () -> projectService.getProjectById(saved.getId()));
    }
}
