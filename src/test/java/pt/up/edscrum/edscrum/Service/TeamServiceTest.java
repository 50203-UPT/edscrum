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
        courseService.createCourse(c);

        Project p = new Project();
        p.setName("Projeto 1");
        p.setCourse(c);
        projectService.createProject(p);

        User sm = new User();
        sm.setName("Scrum Master");
        sm.setRole("STUDENT");
        userService.createUser(sm);

        User po = new User();
        po.setName("Product Owner");
        po.setRole("STUDENT");
        userService.createUser(po);

        User dev1 = new User();
        dev1.setName("Dev 1");
        dev1.setRole("STUDENT");
        userService.createUser(dev1);

        User dev2 = new User();
        dev2.setName("Dev 2");
        dev2.setRole("STUDENT");
        userService.createUser(dev2);

        Team team = new Team();
        team.setName("Team Alpha");
        team.setProjects(List.of(p));
        team.setScrumMaster(sm);
        team.setProductOwner(po);
        team.setDevelopers(List.of(dev1, dev2));

        Team saved = teamService.createTeam(team);
        assertNotNull(saved.getId());
        assertEquals("Team Alpha", saved.getName());
        assertEquals(2, saved.getDevelopers().size());
    }

    @Test
    void testUpdateTeam() {
        Team t = new Team();
        t.setName("Velha Equipa");
        teamService.createTeam(t);

        Team update = new Team();
        update.setName("Nova Equipa");
        Team result = teamService.updateTeam(t.getId(), update);
        assertEquals("Nova Equipa", result.getName());
    }
}
