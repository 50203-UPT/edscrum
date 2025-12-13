package pt.up.edscrum.edscrum.Controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.ProjectRepository;
import pt.up.edscrum.repository.TeamRepository;
import pt.up.edscrum.repository.UserRepository;

@SpringBootTest
@Transactional
class TeamControllerTest {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private User testScrumMaster;
    private User testProductOwner;
    private Project testProject;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        teamRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        testScrumMaster = new User();
        testScrumMaster.setName("Test Scrum Master");
        testScrumMaster.setEmail("scrum.master@test.com");
        testScrumMaster.setRole("TEACHER");
        testScrumMaster = userRepository.save(testScrumMaster);

        testProductOwner = new User();
        testProductOwner.setName("Test Product Owner");
        testProductOwner.setEmail("product.owner@test.com");
        testProductOwner.setRole("STUDENT");
        testProductOwner = userRepository.save(testProductOwner);

        // Create test project
        testProject = new Project();
        testProject.setName("Test Project");
        testProject = projectRepository.save(testProject);
    }

    private Team createTestTeam(String name) {
        Team team = new Team();
        team.setName(name);
        team.setProject(testProject);
        team.setScrumMaster(testScrumMaster);
        team.setProductOwner(testProductOwner);
        return teamRepository.save(team);
    }

    @Test
    void testCreateAndGetTeam() {
        // Arrange
        Team team = new Team();
        team.setName("Test Team");
        team.setProject(testProject);
        team.setScrumMaster(testScrumMaster);
        team.setProductOwner(testProductOwner);

        // Act
        Team savedTeam = teamRepository.save(team);

        // Assert
        assertNotNull(savedTeam.getId());
        assertEquals("Test Team", savedTeam.getName());
        assertEquals(testProject.getId(), savedTeam.getProject().getId());
        assertEquals(testScrumMaster.getId(), savedTeam.getScrumMaster().getId());
        assertEquals(testProductOwner.getId(), savedTeam.getProductOwner().getId());
    }

    /* @Test
    void testGetAllTeams() {
        // Arrange
        createTestTeam("Team A");
        createTestTeam("Team B");

        // Act
        List<Team> teams = teamRepository.findAll();

        // Assert
        assertEquals(2, teams.size());
        assertTrue(teams.stream().anyMatch(t -> t.getName().equals("Team A")));
        assertTrue(teams.stream().anyMatch(t -> t.getName().equals("Team B")));
    }*/
    @Test
    void testUpdateTeam() {
        // Arrange
        Team team = createTestTeam("Old Name");

        // Act
        team.setName("Updated Name");
        Team updatedTeam = teamRepository.save(team);

        // Assert
        assertEquals("Updated Name", updatedTeam.getName());
        assertEquals(team.getId(), updatedTeam.getId());
    }

    @Test
    void testDeleteTeam() {
        // Arrange
        Team team = createTestTeam("Team to Delete");
        // Act
        teamRepository.delete(team);

        // Assert
    }

    @Test
    void testGetTeamById_WhenNotExists() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            teamRepository.findById(999L).orElseThrow(() -> new Exception("Team not found"));
        });
    }
}
