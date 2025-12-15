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
import pt.up.edscrum.model.*;
import pt.up.edscrum.repository.*;
import pt.up.edscrum.service.ProjectService;

@SpringBootTest
@Transactional
class ProjectServiceTest {

    @Autowired private ProjectService projectService;
    @Autowired private EntityManager entityManager;

    @Autowired private ProjectRepository projectRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private SprintRepository sprintRepo;
    @Autowired private AwardRepository awardRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private UserStoryRepository userStoryRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;

    private Course course;

    @BeforeEach
    void setUp() {
        // 1. Limpeza Segura
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
        
        // Setup de 5 alunos Dummy para bloquear prémios de ranking automático
        // Isto é CRÍTICO para evitar que o "Conquistador de Projetos" dispare prémios de ranking extra
        for (int i = 0; i < 5; i++) {
            User dummy = new User();
            dummy.setName("Dummy " + i);
            dummy.setEmail("d" + i + "@t.com");
            dummy.setRole("STUDENT");
            userRepo.save(dummy);
            Score s = new Score(); s.setUser(dummy); s.setTotalPoints(10000);
            scoreRepo.save(s);
        }

        entityManager.flush();
        entityManager.clear();
    }

    // ===========================================
    // TESTES CRUD BÁSICO
    // ===========================================

    @Test
    void testCreateAndGetProject() {
        Project p = new Project();
        p.setName("Novo Projeto");
        p.setCourse(course);
        p.setStatus(ProjectStatus.PLANEAMENTO);

        Project saved = projectService.createProject(p);

        assertNotNull(saved.getId());
        assertEquals("Novo Projeto", saved.getName());

        entityManager.flush();
        entityManager.clear();

        Project found = projectService.getProjectById(saved.getId());
        assertEquals(saved.getId(), found.getId());
    }

    @Test
    void testGetAllProjects() {
        Project p1 = new Project(); p1.setName("P1"); p1.setCourse(course); projectService.createProject(p1);
        Project p2 = new Project(); p2.setName("P2"); p2.setCourse(course); projectService.createProject(p2);

        List<Project> list = projectService.getAllProjects();
        assertEquals(2, list.size());
    }

    @Test
    void testGetProjectById_Fail_NotFound() {
        assertThrows(RuntimeException.class, () -> {
            projectService.getProjectById(999L);
        });
    }

    @Test
    void testUpdateProject() {
        Project p = new Project();
        p.setName("Old Name");
        p.setCourse(course);
        Project saved = projectService.createProject(p);

        Project updateData = new Project();
        updateData.setName("New Name");
        updateData.setSprintGoals("Goals");
        updateData.setCourse(course);
        updateData.setStartDate(LocalDate.now());
        updateData.setEndDate(LocalDate.now().plusMonths(1));

        Project updated = projectService.updateProject(saved.getId(), updateData);

        assertEquals("New Name", updated.getName());
        assertEquals("Goals", updated.getSprintGoals());
        assertNotNull(updated.getStartDate());
    }

    // ===========================================
    // TESTE DELETE (COM DESASSOCIAÇÃO DE EQUIPAS)
    // ===========================================

    @Test
    void testDeleteProject_DisassociatesTeams() {
        // Criar Projeto
        Project p = new Project();
        p.setName("To Delete");
        p.setCourse(course);
        Project savedProject = projectRepo.save(p);

        // Criar Equipa associada
        Team t = new Team();
        t.setName("Team X");
        t.setCourse(course);
        t.setProject(savedProject);
        Team savedTeam = teamRepo.save(t);

        entityManager.flush();
        entityManager.clear();

        // Ação
        projectService.deleteProject(savedProject.getId());

        // Verificações
        assertFalse(projectRepo.existsById(savedProject.getId()));
        
        // A equipa deve existir, mas sem projeto
        Team teamAfter = teamRepo.findById(savedTeam.getId()).get();
        assertNull(teamAfter.getProject(), "A equipa deve ser desassociada do projeto antes dele ser apagado");
    }

    // ===========================================
    // TESTE COMPLETE PROJECT (LÓGICA COMPLEXA)
    // ===========================================

    @Test
    void testCompleteProject_Success_WithAwards() {
        // 1. Setup Projeto e Sprints (Tudo Concluído)
        Project p = new Project();
        p.setName("Finished Project");
        p.setCourse(course);
        p.setStatus(ProjectStatus.EM_CURSO);
        Project savedProject = projectRepo.save(p);

        Sprint s1 = new Sprint(); s1.setProject(savedProject); s1.setStatus(SprintStatus.CONCLUIDO); sprintRepo.save(s1);
        Sprint s2 = new Sprint(); s2.setProject(savedProject); s2.setStatus(SprintStatus.CONCLUIDO); sprintRepo.save(s2);

        // 2. Setup Equipa Completa (SM, PO, Dev)
        User sm = createUser("SM", "sm@t.com");
        User po = createUser("PO", "po@t.com");
        User dev = createUser("Dev", "dev@t.com");

        Team t = new Team();
        t.setProject(savedProject);
        t.setCourse(course);
        t.setScrumMaster(sm);
        t.setProductOwner(po);
        t.setDevelopers(List.of(dev));
        teamRepo.save(t);

        entityManager.flush();
        entityManager.clear();

        // 3. Ação
        projectService.completeProject(savedProject.getId());

        // 4. Verificações
        Project updated = projectRepo.findById(savedProject.getId()).get();
        assertEquals(ProjectStatus.CONCLUIDO, updated.getStatus());

        // Verificar Prémios Individuais (Conquistador de Projetos)
        // Como criámos 5 Dummies com 10000 pontos no setUp, os nossos users ganham 100pts
        // e ficam com 100pts total (longe do Top 5).
        // Logo, só devem ter 1 prémio cada.
        assertEquals(1, studentAwardRepo.countByStudentIdAndProjectId(sm.getId(), savedProject.getId()));
        assertEquals(1, studentAwardRepo.countByStudentIdAndProjectId(po.getId(), savedProject.getId()));
        assertEquals(1, studentAwardRepo.countByStudentIdAndProjectId(dev.getId(), savedProject.getId()));

        // Verificar Prémio de Equipa
        assertFalse(teamAwardRepo.findByTeamIdAndProjectId(t.getId(), savedProject.getId()).isEmpty());
    }

    @Test
    void testCompleteProject_Fail_SprintsNotDone() {
        // 1. Criar Projeto
        Project p = new Project();
        p.setName("Unfinished Project");
        p.setCourse(course);
        p.setStatus(ProjectStatus.EM_CURSO);
        Project savedProject = projectRepo.save(p);

        // 2. Criar Sprints (Uma completa, uma em curso)
        Sprint s1 = new Sprint(); 
        s1.setProject(savedProject); 
        s1.setStatus(SprintStatus.CONCLUIDO); 
        s1.setName("S1");
        sprintRepo.save(s1);

        Sprint s2 = new Sprint(); 
        s2.setProject(savedProject); 
        s2.setStatus(SprintStatus.EM_CURSO); 
        s2.setName("S2");
        sprintRepo.save(s2);

        // --- CRÍTICO: Limpar a cache para o Service ver as Sprints novas ---
        entityManager.flush(); // Garante que as sprints vão para a BD
        entityManager.clear(); // Limpa a memória para obrigar o fetch novo
        // ------------------------------------------------------------------

        final Long pid = savedProject.getId();
        
        // 3. Verificar se lança exceção
        Exception e = assertThrows(IllegalStateException.class, () -> {
            projectService.completeProject(pid);
        });

        assertEquals("Todos os sprints devem estar concluídos antes de marcar o projeto como concluído.", e.getMessage());
    }
    // ===========================================
    // OUTROS TESTES DE NEGÓCIO
    // ===========================================

    @Test
    void testReopenProject() {
        Project p = new Project();
        p.setCourse(course);
        p.setStatus(ProjectStatus.CONCLUIDO);
        Project saved = projectRepo.save(p);

        projectService.reopenProject(saved.getId());

        Project updated = projectRepo.findById(saved.getId()).get();
        assertEquals(ProjectStatus.EM_CURSO, updated.getStatus());
    }

    @Test
    void testIsUserProductOwner() {
        Project p = new Project(); p.setCourse(course); Project savedP = projectRepo.save(p);
        User po = createUser("PO User", "po-check@t.com");
        User random = createUser("Random", "rnd@t.com");

        Team t = new Team();
        t.setProject(savedP);
        t.setCourse(course);
        t.setProductOwner(po);
        teamRepo.save(t);

        entityManager.flush();
        entityManager.clear();

        assertTrue(projectService.isUserProductOwner(po.getId(), savedP.getId()));
        assertFalse(projectService.isUserProductOwner(random.getId(), savedP.getId()));
    }

    @Test
    void testIsUserProductOwner_ProjectHasNoTeams() {
        Project p = new Project(); p.setCourse(course); Project savedP = projectRepo.save(p);
        User u = createUser("User", "u@t.com");
        
        assertFalse(projectService.isUserProductOwner(u.getId(), savedP.getId()));
    }

    @Test
    void testRemoveTeamFromProject_Success() {
        Project p = new Project(); p.setCourse(course); Project savedP = projectRepo.save(p);
        Team t = new Team(); t.setProject(savedP); t.setCourse(course); Team savedT = teamRepo.save(t);

        entityManager.flush();
        entityManager.clear();

        projectService.removeTeamFromProject(savedP.getId(), savedT.getId());

        Team updatedT = teamRepo.findById(savedT.getId()).get();
        assertNull(updatedT.getProject());
    }

    @Test
    void testRemoveTeamFromProject_Fail_NotAssociated() {
        Project p1 = new Project(); p1.setCourse(course); Project savedP1 = projectRepo.save(p1);
        Project p2 = new Project(); p2.setCourse(course); Project savedP2 = projectRepo.save(p2);
        
        Team t = new Team(); t.setProject(savedP2); t.setCourse(course); Team savedT = teamRepo.save(t);

        final Long p1Id = savedP1.getId();
        final Long tId = savedT.getId();

        Exception e = assertThrows(RuntimeException.class, () -> {
            projectService.removeTeamFromProject(p1Id, tId);
        });

        assertEquals("Esta equipa não está associada a este projeto.", e.getMessage());
    }

    // --- Helpers ---
    private User createUser(String name, String email) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword("123");
        u.setRole("STUDENT");
        return userRepo.save(u);
    }
}