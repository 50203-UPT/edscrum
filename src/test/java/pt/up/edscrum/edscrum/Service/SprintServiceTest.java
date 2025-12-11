package pt.up.edscrum.edscrum.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.Sprint;
import pt.up.edscrum.service.ProjectService;
import pt.up.edscrum.service.SprintService;

@SpringBootTest
@Transactional
class SprintServiceTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private SprintService sprintService;

    @Test
    void testCreateSprintWithinProject() {
        Project p = new Project();
        LocalDate today = LocalDate.now();
        p.setName("Projeto 1");
        p.setStartDate(today.plusDays(1));
        p.setEndDate(today.plusDays(30));
        projectService.createProject(p);

        Sprint s = new Sprint();
        s.setName("Sprint 1");
        s.setStartDate(p.getStartDate());
        s.setEndDate(p.getStartDate().plusDays(7));

        Sprint created = sprintService.createSprint(p.getId(), s);
        assertNotNull(created.getId());
        assertEquals("Sprint 1", created.getName());
    }

    @Test
    void testCreateSprintOutsideProjectRange() {
        Project p = new Project();
        LocalDate today = LocalDate.now();
        p.setName("Projeto 2");
        p.setStartDate(today.plusDays(5));
        p.setEndDate(today.plusDays(10));
        projectService.createProject(p);

        Sprint s = new Sprint();
        s.setName("Sprint Fora");
        s.setStartDate(today);
        s.setEndDate(today.plusDays(3));

        assertThrows(ResponseStatusException.class, () -> sprintService.createSprint(p.getId(), s));
    }

    @Test
    void testCreateSprintOverlap() {
        Project p = new Project();
        LocalDate today = LocalDate.now();
        p.setName("Projeto 3");
        p.setStartDate(today.plusDays(1));
        p.setEndDate(today.plusDays(20));
        projectService.createProject(p);

        Sprint s1 = new Sprint();
        s1.setName("Sprint A");
        s1.setStartDate(p.getStartDate());
        s1.setEndDate(p.getStartDate().plusDays(5));
        sprintService.createSprint(p.getId(), s1);

        Sprint s2 = new Sprint();
        s2.setName("Sprint B");
        // Overlap with s1
        s2.setStartDate(p.getStartDate().plusDays(3));
        s2.setEndDate(p.getStartDate().plusDays(8));

        assertThrows(ResponseStatusException.class, () -> sprintService.createSprint(p.getId(), s2));
    }

}
