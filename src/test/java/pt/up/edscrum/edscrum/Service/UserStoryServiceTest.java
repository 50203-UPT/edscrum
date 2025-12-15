package pt.up.edscrum.edscrum.Service;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.enums.ProjectStatus;
import pt.up.edscrum.enums.SprintStatus;
import pt.up.edscrum.enums.UserStoryPriority;
import pt.up.edscrum.enums.UserStoryStatus;
import pt.up.edscrum.model.*;
import pt.up.edscrum.repository.*;
import pt.up.edscrum.service.UserStoryService;

@SpringBootTest
@Transactional
class UserStoryServiceTest {

    @Autowired private UserStoryService userStoryService;
    @Autowired private EntityManager entityManager;

    // Repositórios para limpeza e setup
    @Autowired private UserStoryRepository userStoryRepo;
    @Autowired private SprintRepository sprintRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private AwardRepository awardRepo;

    private Course course;
    private Project project;
    private Sprint sprint;
    private User user;

    @BeforeEach
    void setUp() {
        // 1. Limpeza (Ordem inversa das dependências)
        notificationRepo.deleteAll();
        userStoryRepo.deleteAll();
        sprintRepo.deleteAll();
        teamAwardRepo.deleteAll();
        studentAwardRepo.deleteAll();
        scoreRepo.deleteAll();
        teamRepo.deleteAll();
        projectRepo.deleteAll();
        enrollmentRepo.deleteAll();
        awardRepo.deleteAll();
        courseRepo.deleteAll();
        userRepo.deleteAll();

        // 2. Setup Base
        course = new Course();
        course.setName("Engenharia SW");
        course = courseRepo.save(course);

        project = new Project();
        project.setName("Projeto Demo");
        project.setCourse(course);
        project.setStatus(ProjectStatus.EM_CURSO);
        project = projectRepo.save(project);

        sprint = new Sprint();
        sprint.setName("Sprint 1");
        sprint.setProject(project);
        sprint.setStatus(SprintStatus.EM_CURSO);
        sprint = sprintRepo.save(sprint);

        user = new User();
        user.setName("Dev");
        user.setEmail("dev@test.com");
        user.setPassword("123");
        user.setRole("STUDENT");
        user = userRepo.save(user);

        entityManager.flush();
        entityManager.clear();
    }

    // ===========================================
    // TESTES: CREATE USER STORY
    // ===========================================

    @Test
    void testCreateUserStory_Minimal() {
        // Cenário: Sem Sprint e Sem Assignee
        UserStory us = new UserStory();
        us.setName("Basic Story");
        
        UserStory created = userStoryService.createUserStory(us);

        assertNotNull(created.getId());
        assertEquals("Basic Story", created.getName());
        assertEquals(UserStoryStatus.TODO, created.getStatus(), "Deve assumir TODO por defeito");
        assertNull(created.getSprint());
        assertNull(created.getAssignee());
    }

    @Test
    void testCreateUserStory_Full_WithAssigneeId() {
        // Recarregar entidades para ter IDs frescos
        Sprint s = sprintRepo.findAll().get(0);
        User u = userRepo.findAll().get(0);

        UserStory us = new UserStory();
        us.setName("Full Story");
        us.setSprint(s);
        
        // Simular envio apenas com ID (como vem do frontend)
        User assigneeRef = new User();
        assigneeRef.setId(u.getId());
        us.setAssignee(assigneeRef);

        UserStory created = userStoryService.createUserStory(us);

        assertNotNull(created.getSprint());
        assertEquals(s.getId(), created.getSprint().getId());
        assertNotNull(created.getAssignee());
        assertEquals(u.getId(), created.getAssignee().getId());
    }

    @Test
    void testCreateUserStory_WithAssigneeEmail() {
        Sprint s = sprintRepo.findAll().get(0);
        
        UserStory us = new UserStory();
        us.setName("Email Story");
        us.setSprint(s);
        
        // Simular envio apenas com Email
        User assigneeRef = new User();
        assigneeRef.setEmail("dev@test.com"); // Email existente no setup
        us.setAssignee(assigneeRef);

        UserStory created = userStoryService.createUserStory(us);

        assertNotNull(created.getAssignee());
        assertEquals("dev@test.com", created.getAssignee().getEmail());
    }

    @Test
    void testCreateUserStory_TriggersStatusChange_SprintAndProject() {
        // Setup: Projeto e Sprint em PLANEAMENTO
        Project pPlan = new Project();
        pPlan.setCourse(courseRepo.findAll().get(0));
        pPlan.setName("Plan Proj");
        pPlan.setStatus(ProjectStatus.PLANEAMENTO);
        pPlan = projectRepo.save(pPlan);

        Sprint sPlan = new Sprint();
        sPlan.setProject(pPlan);
        sPlan.setName("Plan Sprint");
        sPlan.setStatus(SprintStatus.PLANEAMENTO);
        sPlan = sprintRepo.save(sPlan);

        entityManager.flush();
        entityManager.clear();

        // Ação: Criar US nessa Sprint
        UserStory us = new UserStory();
        us.setName("Trigger Story");
        us.setSprint(sPlan);
        
        userStoryService.createUserStory(us);

        // Verificação
        Sprint updatedSprint = sprintRepo.findById(sPlan.getId()).get();
        assertEquals(SprintStatus.EM_CURSO, updatedSprint.getStatus(), "Sprint deve passar a EM_CURSO");

        Project updatedProject = projectRepo.findById(pPlan.getId()).get();
        assertEquals(ProjectStatus.EM_CURSO, updatedProject.getStatus(), "Projeto deve passar a EM_CURSO");
    }

    // ===========================================
    // TESTES: EXCEPTIONS NO CREATE
    // ===========================================

   @Test
    void testCreateUserStory_Fail_SprintNotFound() {
        UserStory us = new UserStory();
        us.setName("Story Fail");
        
        // Criar um objeto Sprint com um ID fixo que sabemos não existir (e.g. 99999L)
        Sprint fakeSprint = new Sprint();
        fakeSprint.setId(99999L); 
        us.setSprint(fakeSprint);

        // O serviço vai tentar fazer sprintRepo.findById(99999L) e deve falhar
        Exception e = assertThrows(RuntimeException.class, () -> {
            userStoryService.createUserStory(us);
        });
        assertEquals("Sprint não encontrado", e.getMessage());
    }

    @Test
    void testCreateUserStory_Fail_AssigneeIdNotFound() {
        UserStory us = new UserStory();
        User fakeUser = new User();
        fakeUser.setId(999L);
        us.setAssignee(fakeUser);

        Exception e = assertThrows(RuntimeException.class, () -> {
            userStoryService.createUserStory(us);
        });
        assertEquals("Utilizador não encontrado", e.getMessage());
    }

    @Test
    void testCreateUserStory_Fail_AssigneeEmailNotFound() {
        UserStory us = new UserStory();
        User fakeUser = new User();
        fakeUser.setEmail("naoexiste@test.com");
        us.setAssignee(fakeUser);

        Exception e = assertThrows(RuntimeException.class, () -> {
            userStoryService.createUserStory(us);
        });
        assertEquals("Utilizador não encontrado", e.getMessage());
    }

    // ===========================================
    // TESTES: UPDATE STATUS
    // ===========================================

    @Test
    void testUpdateUserStoryStatus() {
        UserStory us = new UserStory();
        us.setName("Status Test");
        us.setStatus(UserStoryStatus.TODO);
        UserStory saved = userStoryRepo.save(us);

        UserStory updated = userStoryService.updateUserStoryStatus(saved.getId(), UserStoryStatus.DONE);

        assertEquals(UserStoryStatus.DONE, updated.getStatus());
    }

    @Test
    void testUpdateUserStoryStatus_Fail_NotFound() {
        assertThrows(RuntimeException.class, () -> {
            userStoryService.updateUserStoryStatus(999L, UserStoryStatus.DONE);
        });
    }

    // ===========================================
    // TESTES: GET & DELETE
    // ===========================================

    @Test
    void testGetUserStoryById() {
        UserStory us = new UserStory();
        us.setName("Get Me");
        UserStory saved = userStoryRepo.save(us);

        UserStory found = userStoryService.getUserStoryById(saved.getId());
        assertEquals("Get Me", found.getName());
    }

    @Test
    void testGetUserStoryById_Fail() {
        assertThrows(RuntimeException.class, () -> {
            userStoryService.getUserStoryById(999L);
        });
    }

    @Test
    void testDeleteUserStory() {
        UserStory us = new UserStory();
        us.setName("Delete Me");
        UserStory saved = userStoryRepo.save(us);

        userStoryService.deleteUserStory(saved.getId());

        assertFalse(userStoryRepo.existsById(saved.getId()));
    }

    // ===========================================
    // TESTES: UPDATE FULL (Edição)
    // ===========================================

    @Test
    void testUpdateUserStory_FieldsAndAssigneeById() {
        // Setup Inicial
        User u1 = userRepo.findAll().get(0);
        
        UserStory original = new UserStory();
        original.setName("Old Name");
        original.setDescription("Old Desc");
        original.setPriority(UserStoryPriority.LOW);
        original.setStoryPoints(1);
        original = userStoryRepo.save(original);

        // Dados para Update
        UserStory updateData = new UserStory();
        updateData.setName("New Name");
        updateData.setDescription("New Desc");
        updateData.setPriority(UserStoryPriority.HIGH);
        updateData.setStoryPoints(5);
        
        // Atribuir User por ID
        User assigneeRef = new User();
        assigneeRef.setId(u1.getId());
        updateData.setAssignee(assigneeRef);

        // Ação
        UserStory updated = userStoryService.updateUserStory(original.getId(), updateData);

        // Verificação
        assertEquals("New Name", updated.getName());
        assertEquals("New Desc", updated.getDescription());
        assertEquals(UserStoryPriority.HIGH, updated.getPriority());
        assertEquals(5, updated.getStoryPoints());
        assertEquals(u1.getId(), updated.getAssignee().getId());
    }

    @Test
    void testUpdateUserStory_AssigneeByEmail() {
        UserStory original = new UserStory();
        original.setName("Email Update");
        original = userStoryRepo.save(original);

        UserStory updateData = new UserStory();
        updateData.setName("Email Update"); // Manter nome
        
        User assigneeRef = new User();
        assigneeRef.setEmail("dev@test.com");
        updateData.setAssignee(assigneeRef);

        UserStory updated = userStoryService.updateUserStory(original.getId(), updateData);

        assertEquals("dev@test.com", updated.getAssignee().getEmail());
    }

    @Test
    void testUpdateUserStory_Fail_NotFound() {
        UserStory data = new UserStory();
        data.setName("X");
        assertThrows(RuntimeException.class, () -> {
            userStoryService.updateUserStory(999L, data);
        });
    }

    @Test
    void testUpdateUserStory_Fail_AssigneeIdNotFound() {
        UserStory original = new UserStory();
        original.setName("Orig");
        original = userStoryRepo.save(original);

        UserStory updateData = new UserStory();
        User fakeUser = new User();
        fakeUser.setId(999L);
        updateData.setAssignee(fakeUser);

        final Long id = original.getId();
        assertThrows(RuntimeException.class, () -> {
            userStoryService.updateUserStory(id, updateData);
        });
    }

    @Test
    void testUpdateUserStory_Fail_AssigneeEmailNotFound() {
        UserStory original = new UserStory();
        original.setName("Orig");
        original = userStoryRepo.save(original);

        UserStory updateData = new UserStory();
        User fakeUser = new User();
        fakeUser.setEmail("ghost@test.com");
        updateData.setAssignee(fakeUser);

        final Long id = original.getId();
        assertThrows(RuntimeException.class, () -> {
            userStoryService.updateUserStory(id, updateData);
        });
    }
}