package pt.up.edscrum.edscrum.Controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.Course;
import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.User;
import pt.up.edscrum.service.ProjectService;
import pt.up.edscrum.service.TeamService;
import pt.up.edscrum.service.UserService;

@SpringBootTest
@Transactional
class TeamControllerTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private pt.up.edscrum.service.CourseService courseService;

    @Autowired
    private UserService userService;

    @Test
    void testCreateAndGetTeam() {
        // Criar projeto associado
        Project p = new Project();
        p.setName("Projeto Teste");
        p.setSprintGoals("Objetivo do projeto");
        // Criar e associar um curso
        Course c = new Course();
        c.setName("Curso Teste");
        Course savedCourse = courseService.createCourse(c);
        p.setCourse(savedCourse);
        Project savedProject = projectService.createProject(p);

        // Criar usuários
        User scrumMaster = new User();
        scrumMaster.setName("João SM");
        userService.createUser(scrumMaster);

        User productOwner = new User();
        productOwner.setName("Maria PO");
        userService.createUser(productOwner);

        // Criar time
        Team t = new Team();
        t.setName("Team Alpha");
        t.setProjects(List.of(savedProject));
        t.setScrumMaster(scrumMaster);
        t.setProductOwner(productOwner);
        t.setDevelopers(new ArrayList<>());

        Team saved = teamService.createTeam(t);

        assertNotNull(saved.getId());
        assertEquals("Team Alpha", saved.getName());
        assertNotNull(saved.getProjects());
        assertEquals(1, saved.getProjects().size());
        assertEquals(savedProject.getName(), saved.getProjects().get(0).getName());
        assertNotNull(saved.getScrumMaster());
        assertEquals("João SM", saved.getScrumMaster().getName());
        assertNotNull(saved.getProductOwner());
        assertEquals("Maria PO", saved.getProductOwner().getName());
        assertNotNull(saved.getDevelopers());
        assertEquals(0, saved.getDevelopers().size());

        // Buscar o mesmo team
        Team found = teamService.getTeamById(saved.getId());
        assertEquals(saved.getName(), found.getName());

        // Conferir associação ao curso e ao projeto
        assertNotNull(saved.getCourse());
        assertEquals(savedProject.getCourse(), saved.getCourse());
        assertTrue(saved.getProjects().stream().anyMatch(prj -> prj.getId().equals(savedProject.getId())));
    }

    @Test
    void testCannotCreateTeamIfScrumMasterAlreadyInCourse() {
        // Criar curso
        Course c = new Course();
        c.setName("Curso X");
        Course savedCourse = courseService.createCourse(c);

        // Criar dois projetos no mesmo curso
        Project p1 = new Project(); p1.setName("P1"); p1.setCourse(savedCourse);
        Project p2 = new Project(); p2.setName("P2"); p2.setCourse(savedCourse);
        projectService.createProject(p1);
        projectService.createProject(p2);

        // Criar usuário
        User sm = new User(); sm.setName("SM1"); userService.createUser(sm);

        // Criar primeira equipa com SM
        Team t1 = new Team(); t1.setName("T1"); t1.setCourse(savedCourse); t1.setScrumMaster(sm);
        teamService.createTeam(t1);

        // Tentar criar segunda equipa no mesmo curso com o mesmo SM
        Team t2 = new Team(); t2.setName("T2"); t2.setCourse(savedCourse); t2.setScrumMaster(sm);
        assertThrows(pt.up.edscrum.exception.TeamValidationException.class, () -> teamService.createTeam(t2));
    }

    @Test
    void testGetAllTeams() {
        Team t1 = new Team();
        t1.setName("Team A");
        teamService.createTeam(t1);

        Team t2 = new Team();
        t2.setName("Team B");
        teamService.createTeam(t2);

        List<Team> teams = teamService.getAllTeams();
        assertTrue(teams.size() >= 2);
    }

    @Test
    void testUpdateTeam() {
        Team t = new Team();
        t.setName("Velho Nome");
        Team saved = teamService.createTeam(t);

        Team update = new Team();
        update.setName("Novo Nome");

        Team updated = teamService.updateTeam(saved.getId(), update);
        assertEquals("Novo Nome", updated.getName());
    }

    @Test
    void testDeleteTeam() {
        Team t = new Team();
        t.setName("Team para apagar");
        Team saved = teamService.createTeam(t);

        teamService.deleteTeam(saved.getId());

        assertThrows(RuntimeException.class, () -> teamService.getTeamById(saved.getId()));
    }
}

