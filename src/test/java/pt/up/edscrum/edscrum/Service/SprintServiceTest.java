package pt.up.edscrum.edscrum.Service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

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
import pt.up.edscrum.service.SprintService;

@SpringBootTest
@Transactional
class SprintServiceTest {

    @Autowired private SprintService sprintService;
    @Autowired private EntityManager entityManager;

    // Repositórios
    @Autowired private SprintRepository sprintRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private UserStoryRepository userStoryRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private AwardRepository awardRepo;

    private Course course;
    private Project project;

    @BeforeEach
    void setUp() {
        // 1. Limpeza por ordem de dependência (Filhos -> Pais)
        notificationRepo.deleteAll();
        userStoryRepo.deleteAll(); // UserStories dependem de Sprints
        sprintRepo.deleteAll();    // Sprints dependem de Projects
        teamAwardRepo.deleteAll();
        studentAwardRepo.deleteAll();
        scoreRepo.deleteAll();
        teamRepo.deleteAll();      // Teams dependem de Projects e Users
        projectRepo.deleteAll();   // Projects dependem de Courses
        enrollmentRepo.deleteAll();
        awardRepo.deleteAll();
        courseRepo.deleteAll();
        userRepo.deleteAll();

        // 2. Setup Base
        course = new Course();
        course.setName("Engenharia de Software");
        course = courseRepo.save(course);

        project = new Project();
        project.setName("Projeto Teste");
        project.setCourse(course);
        project.setStatus(ProjectStatus.EM_CURSO);
        project = projectRepo.save(project);

        // Limpar cache para garantir ID frescos e estado limpo
        entityManager.flush();
        entityManager.clear();
    }

    // ===========================================
    // TESTES DE CRIAÇÃO E UPDATE
    // ===========================================

    @Test
    void testCreateSprint_AndStartProject() {
        // Preparar projeto em PLANEAMENTO
        Project planningProject = new Project();
        planningProject.setName("Projeto Futuro");
        planningProject.setCourse(course);
        planningProject.setStatus(ProjectStatus.PLANEAMENTO);
        planningProject = projectRepo.save(planningProject);

        Sprint sprint = new Sprint();
        sprint.setName("Sprint 1");
        sprint.setStartDate(LocalDate.now());
        sprint.setEndDate(LocalDate.now().plusWeeks(2));
        sprint.setStatus(SprintStatus.PLANEAMENTO);

        // Ação
        Sprint created = sprintService.createSprint(planningProject.getId(), sprint);

        // Verificações
        assertNotNull(created.getId());
        assertEquals("Sprint 1", created.getName());
        assertEquals(planningProject.getId(), created.getProject().getId());

        // Verificar se o projeto mudou de estado automaticamente
        Project updatedProject = projectRepo.findById(planningProject.getId()).orElseThrow();
        assertEquals(ProjectStatus.EM_CURSO, updatedProject.getStatus(), "Projeto deve mudar para EM_CURSO ao criar sprint");
    }

    @Test
    void testUpdateSprint() {
        Sprint sprint = new Sprint();
        sprint.setProject(project);
        sprint.setName("Old Name");
        sprint.setStatus(SprintStatus.PLANEAMENTO);
        Sprint saved = sprintRepo.save(sprint);

        Sprint updateData = new Sprint();
        updateData.setName("New Name");
        updateData.setStatus(SprintStatus.EM_CURSO);
        updateData.setStartDate(LocalDate.now());
        updateData.setEndDate(LocalDate.now().plusDays(10));

        Sprint updated = sprintService.updateSprint(saved.getId(), updateData);

        assertEquals("New Name", updated.getName());
        assertEquals(SprintStatus.EM_CURSO, updated.getStatus());
        assertNotNull(updated.getStartDate());
    }

    // ===========================================
    // TESTES DE PROGRESSO
    // ===========================================

    @Test
    void testCalculateSprintProgress() {
        Sprint sprint = new Sprint();
        sprint.setProject(project);
        sprint.setName("Progresso Sprint");
        sprint = sprintRepo.save(sprint);

        // Criar 4 User Stories para facilitar contas
        // 1. DONE (100%)
        createUserStory(sprint, "US1", UserStoryStatus.DONE);
        // 2. TESTING (75%)
        createUserStory(sprint, "US2", UserStoryStatus.TESTING);
        // 3. IN_PROGRESS (25%)
        createUserStory(sprint, "US3", UserStoryStatus.IN_PROGRESS);
        // 4. TODO (0%)
        createUserStory(sprint, "US4", UserStoryStatus.TODO);

        // Flush para garantir que o serviço vê as US na BD
        entityManager.flush();
        entityManager.clear();

        // Cálculo esperado: (100 + 75 + 25 + 0) / 4 = 200 / 4 = 50%
        int progress = sprintService.calculateSprintProgress(sprint.getId());

        assertEquals(50, progress);
    }

    @Test
    void testCalculateSprintProgress_EmptySprint() {
        Sprint sprint = new Sprint();
        sprint.setProject(project);
        sprint.setName("Empty Sprint");
        sprint = sprintRepo.save(sprint);

        int progress = sprintService.calculateSprintProgress(sprint.getId());
        assertEquals(0, progress, "Sprint sem US deve ter 0% de progresso");
    }

    // ===========================================
    // TESTES DE CONCLUSÃO (COMPLETE)
    // ===========================================

    @Test
    void testCompleteSprint_Success_AndNotifications() {
        // 1. Setup Equipa e Membros (para receber notificações)
        User sm = createUser("ScrumMaster", "sm@test.com");
        User dev = createUser("Developer", "dev@test.com");
        
        Team team = new Team();
        team.setName("Notification Team");
        team.setCourse(course);
        team.setProject(project);
        team.setScrumMaster(sm);
        team.setDevelopers(List.of(dev));
        teamRepo.save(team);

        // 2. Setup Sprint com todas as US DONE
        Sprint sprint = new Sprint();
        sprint.setProject(project);
        sprint.setName("Sprint Final");
        sprint.setStatus(SprintStatus.EM_CURSO);
        sprint = sprintRepo.save(sprint);

        createUserStory(sprint, "US Done 1", UserStoryStatus.DONE);
        createUserStory(sprint, "US Done 2", UserStoryStatus.DONE);

        // Limpar notificações antigas e cache
        notificationRepo.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // 3. Ação
        Sprint completed = sprintService.completeSprint(sprint.getId());

        // 4. Verificações
        assertEquals(SprintStatus.CONCLUIDO, completed.getStatus());
        
        // Verificar Notificações (1 para SM, 1 para Dev)
        assertEquals(2, notificationRepo.count(), "Deve notificar membros da equipa");
    }

    @Test
    void testCompleteSprint_Fail_NotAllDone() {
        Sprint sprint = new Sprint();
        sprint.setProject(project);
        sprint.setStatus(SprintStatus.EM_CURSO);
        final Sprint savedSprint = sprintRepo.save(sprint); // Final para lambda

        createUserStory(savedSprint, "US Done", UserStoryStatus.DONE);
        createUserStory(savedSprint, "US Todo", UserStoryStatus.TODO); // Impede conclusão

        entityManager.flush();
        entityManager.clear();

        Exception e = assertThrows(RuntimeException.class, () -> {
            sprintService.completeSprint(savedSprint.getId());
        });

        assertTrue(e.getMessage().contains("Todas as User Stories devem estar concluídas"));
    }

    // ===========================================
    // TESTES DE REABERTURA E ELIMINAÇÃO
    // ===========================================

    @Test
    void testReopenSprint_AndProject() {
        // Configurar Projeto CONCLUIDO
        Project completedProj = new Project();
        completedProj.setCourse(course);
        completedProj.setName("Closed Proj");
        completedProj.setStatus(ProjectStatus.CONCLUIDO);
        completedProj = projectRepo.save(completedProj);

        Sprint sprint = new Sprint();
        sprint.setProject(completedProj);
        sprint.setStatus(SprintStatus.CONCLUIDO);
        sprint = sprintRepo.save(sprint);

        // Ação
        Sprint reopened = sprintService.reopenSprint(sprint.getId());

        // Verificações
        assertEquals(SprintStatus.EM_CURSO, reopened.getStatus());
        
        Project checkProj = projectRepo.findById(completedProj.getId()).orElseThrow();
        assertEquals(ProjectStatus.EM_CURSO, checkProj.getStatus(), "Projeto deve reabrir se sprint reabrir");
    }

    @Test
    void testDeleteSprint_CascadesToUserStories() {
        Sprint sprint = new Sprint();
        sprint.setProject(project);
        sprint.setName("To Delete");
        sprint = sprintRepo.save(sprint);

        UserStory us = createUserStory(sprint, "US to die", UserStoryStatus.TODO);
        Long usId = us.getId();

        entityManager.flush();
        entityManager.clear();

        // Ação
        sprintService.deleteSprint(sprint.getId());

        // Verificação
        assertFalse(sprintRepo.existsById(sprint.getId()));
        assertFalse(userStoryRepo.existsById(usId), "User Stories devem ser apagadas com a Sprint");
    }
    
    @Test
    void testUpdateUserStoryStatus() {
        Sprint s = new Sprint();
        s.setProject(project);
        s = sprintRepo.save(s);
        
        UserStory us = createUserStory(s, "Status Check", UserStoryStatus.TODO);
        
        // Ação
        sprintService.updateUserStoryStatus(us.getId(), "DONE");
        
        // Verificação
        UserStory updated = userStoryRepo.findById(us.getId()).get();
        assertEquals(UserStoryStatus.DONE, updated.getStatus());
    }

   // --- Helpers ---

    private UserStory createUserStory(Sprint sprint, String title, UserStoryStatus status) {
        UserStory us = new UserStory();
        us.setSprint(sprint);
        // Usamos setName conforme validado nos erros anteriores
        us.setName(title); 
        us.setStatus(status);
        
        // Se a tua UserStory tiver prioridade, mantém esta linha. 
    
        us.setPriority(UserStoryPriority.MEDIUM);
        
        return userStoryRepo.save(us);
    }


    private User createUser(String name, String email) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword("123");
        u.setRole("STUDENT");
        return userRepo.save(u);
    }
}