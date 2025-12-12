package pt.up.edscrum.edscrum.Service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.model.Course;
import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.User;
import pt.up.edscrum.service.CourseService;
import pt.up.edscrum.service.ProjectService;
import pt.up.edscrum.service.TeamService;
import pt.up.edscrum.service.UserService;

@SpringBootTest
@Transactional
class TeamServiceTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

    @Test
    void testCreateTeamWithMembers() {
        Course c = new Course();
        c.setName("Engenharia");
        c = courseService.createCourse(c); // Ensure course is managed

        Project p = new Project();
        p.setName("Projeto 1");
        p.setCourse(c);
        p = projectService.createProject(p); // Ensure project is managed

        User sm = new User();
        sm.setName("Scrum Master");
        sm.setRole("STUDENT");
        sm = userService.createUser(sm); // Assign the returned persisted user

        User po = new User();
        po.setName("Product Owner");
        po.setRole("STUDENT");
        po = userService.createUser(po); // Assign the returned persisted user

        User dev1 = new User();
        dev1.setName("Dev 1");
        dev1.setRole("STUDENT");
        dev1 = userService.createUser(dev1); // Assign the returned persisted user

        User dev2 = new User();
        dev2.setName("Dev 2");
        dev2.setRole("STUDENT");
        dev2 = userService.createUser(dev2); // Assign the returned persisted user

        Team team = new Team();
        team.setName("Team Alpha");
        team.setProject(p);
        team.setCourse(c); // FIX: Set the course for the team
        team.setScrumMaster(sm);
        team.setProductOwner(po);
        team.setDevelopers(List.of(dev1, dev2));

        Team saved = teamService.createTeam(team); // Line 85 (or close to it)
        assertNotNull(saved.getId());
        assertEquals("Team Alpha", saved.getName());
        assertEquals(2, saved.getDevelopers().size());
        assertNotNull(saved.getCourse());
        assertEquals(c.getId(), saved.getCourse().getId());
        assertNotNull(saved.getProject());
        assertEquals(p.getId(), saved.getProject().getId());
    }

    @Test
    void testUpdateTeam() {
        // Arrange: Create Course and Project for the team
        Course c = new Course();
        c.setName("Course for Update");
        c = courseService.createCourse(c);

        Project p = new Project();
        p.setName("Project for Update");
        p.setCourse(c);
        p = projectService.createProject(p);

        Team t = new Team();
        t.setName("Velha Equipa");
        t.setCourse(c); // Set course for the initial team
        t.setProject(p); // Set project for the initial team
        t = teamService.createTeam(t); // Ensure team is managed

        // Act: Create an update object that preserves existing associations
        Team update = new Team();
        update.setName("Nova Equipa");
        update.setCourse(t.getCourse()); // Preserve existing course
        update.setProject(t.getProject()); // Preserve existing project

        Team result = teamService.updateTeam(t.getId(), update);

        // Assert
        assertEquals("Nova Equipa", result.getName());
        assertNotNull(result.getCourse());
        assertEquals(c.getName(), result.getCourse().getName());
        assertNotNull(result.getProject());
        assertEquals(p.getName(), result.getProject().getName());
    }
}
