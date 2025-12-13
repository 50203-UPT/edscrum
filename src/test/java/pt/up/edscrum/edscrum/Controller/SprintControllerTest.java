package pt.up.edscrum.edscrum.Controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.Sprint;
import pt.up.edscrum.repository.ProjectRepository;
import pt.up.edscrum.repository.SprintRepository;
import pt.up.edscrum.service.SprintService;

@SpringBootTest
@Transactional
public class SprintControllerTest {

    @Autowired
    private SprintService sprintService;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private Project testProject;

    @BeforeEach
    void setUp() {
        sprintRepository.deleteAll();
        projectRepository.deleteAll();

        Project project = new Project();
        project.setName("Test Project");
        testProject = projectRepository.save(project);
    }

    @Test
    void testCreateSprintFlow() {
        // This test covers the service logic that the SprintController would use.

        // Arrange
        Sprint newSprint = new Sprint();
        newSprint.setName("Sprint #1");
        newSprint.setDescription("Initial description"); // Corrected from setGoals
        newSprint.setProject(testProject);

        // Act
        Sprint savedSprint = sprintService.createSprint(testProject.getId(), newSprint);

        // Assert
        assertNotNull(savedSprint);
        assertNotNull(savedSprint.getId());
        assertEquals("Sprint #1", savedSprint.getName());

        Sprint foundSprint = sprintRepository.findById(savedSprint.getId()).orElse(null);
        assertNotNull(foundSprint);
        assertEquals("Initial description", foundSprint.getDescription());
    }
}
