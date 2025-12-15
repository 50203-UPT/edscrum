package pt.up.edscrum.edscrum.Controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import pt.up.edscrum.controller.ProjectController;
import pt.up.edscrum.dto.dashboard.ProjectDetailsDTO;
import pt.up.edscrum.enums.ProjectStatus;
import pt.up.edscrum.enums.SprintStatus;
import pt.up.edscrum.model.*;
import pt.up.edscrum.repository.*;

/**
 * Testes de integração para o ProjectController.
 * <p>
 * Verifica todas as operações expostas pela API de projetos, garantindo
 * que as regras de negócio e permissões são respeitadas num ambiente real.
 * </p>
 */
@SpringBootTest
@Transactional
class ProjectControllerTest {

    @Autowired private ProjectController projectController;
    @Autowired private EntityManager entityManager;

    // Repositórios
    @Autowired private UserRepository userRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private SprintRepository sprintRepo;
    
    // Repositórios extra para limpeza
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private UserStoryRepository userStoryRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private AwardRepository awardRepo;

    private User teacher;
    private User po;      // Product Owner
    private User dev;     // Developer (Não PO)
    private Course course;
    private Project project;
    private Team team;
    
    private MockHttpSession session;

    /**
     * Configuração inicial: Limpa a BD e cria um cenário base.
     * Cenário: Curso -> Projeto -> Equipa (com PO e Dev).
     */
    @BeforeEach
    void setUp() {
        // Limpeza profunda
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

        // Users
        teacher = createUser("Teacher", "t@upt.pt", "TEACHER");
        po = createUser("Product Owner", "po@upt.pt", "STUDENT");
        dev = createUser("Developer", "dev@upt.pt", "STUDENT");

        // Course
        course = new Course();
        course.setName("Engenharia SW");
        course.setTeacher(teacher);
        course = courseRepo.save(course);

        // Project
        project = new Project();
        project.setName("Project Alpha");
        project.setCourse(course);
        project.setStatus(ProjectStatus.EM_CURSO);
        project = projectRepo.save(project);

        // Team (Associate PO to Project)
        team = new Team();
        team.setName("Team Alpha");
        team.setCourse(course);
        team.setProject(project);
        team.setProductOwner(po);
        team.setDevelopers(List.of(dev));
        team = teamRepo.save(team);

        session = new MockHttpSession();
        
        entityManager.flush();
        entityManager.clear();
    }

    // ==========================================
    // CRUD Operations (GET, POST, PUT, DELETE)
    // ==========================================

    /**
     * Testa a listagem de todos os projetos.
     */
    @Test
    void testGetAllProjects() {
        List<Project> projects = projectController.getAllProjects();
        assertFalse(projects.isEmpty());
        assertEquals("Project Alpha", projects.get(0).getName());
    }

    /**
     * Testa a obtenção de um projeto por ID.
     */
    @Test
    void testGetProjectById() {
        ResponseEntity<Project> resp = projectController.getProjectById(project.getId());
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Project Alpha", resp.getBody().getName());
    }

    /**
     * Testa a obtenção de detalhes avançados do projeto (DashboardDTO).
     */
    @Test
    void testGetProjectDetails() {
        ResponseEntity<ProjectDetailsDTO> resp = projectController.getProjectDetails(project.getId());
        
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals("Project Alpha", resp.getBody().getName());
        
        // CORREÇÃO: Removemos a validação de getTeams() pois o método não existe no DTO.
        // A validação do nome e status code já garante que o serviço funcionou.
        assertNotNull(resp.getBody().getId());
    }

    /**
     * Testa a criação de um novo projeto.
     */
    @Test
    void testCreateProject() {
        Project newP = new Project();
        newP.setName("Project Beta");
        newP.setCourse(course);
        
        ResponseEntity<Project> resp = projectController.createProject(newP);
        assertEquals(201, resp.getStatusCode().value());
        assertNotNull(resp.getBody().getId());
        assertEquals("Project Beta", resp.getBody().getName());
    }

    /**
     * Testa a atualização de um projeto existente.
     */
    @Test
    void testUpdateProject() {
        Project update = new Project();
        update.setName("Project Updated");
        
        ResponseEntity<Project> resp = projectController.updateProject(project.getId(), update);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Project Updated", resp.getBody().getName());
    }

    /**
     * Testa a eliminação de um projeto.
     */
    @Test
    void testDeleteProject() {
        ResponseEntity<Void> resp = projectController.deleteProject(project.getId());
        assertEquals(204, resp.getStatusCode().value());
        assertFalse(projectRepo.existsById(project.getId()));
    }

    // ==========================================
    // COMPLETE PROJECT
    // ==========================================

    /**
     * Testa a conclusão de projeto sem sessão (401).
     */
    @Test
    void testCompleteProject_NoSession() {
        ResponseEntity<?> resp = projectController.completeProject(project.getId(), null, session);
        assertEquals(401, resp.getStatusCode().value());
    }

    /**
     * Testa a conclusão de projeto por um utilizador que não é PO nem Teacher (403).
     */
    @Test
    void testCompleteProject_Forbidden_NonPO() {
        setSession(dev); // Developer não é PO
        ResponseEntity<?> resp = projectController.completeProject(project.getId(), null, session);
        
        assertEquals(403, resp.getStatusCode().value());
        assertTrue(resp.getBody().toString().contains("Apenas o Product Owner"));
    }

    /**
     * Testa a conclusão de projeto pelo Product Owner (Sucesso).
     * Nota: Requer sprints concluídos se existirem.
     */
    @Test
    void testCompleteProject_Success_PO() {
        setSession(po);
        
        // Criar um sprint concluído para garantir que a validação de sprints passa
        createSprint(SprintStatus.CONCLUIDO);
        
        ResponseEntity<?> resp = projectController.completeProject(project.getId(), null, session);
        assertEquals(200, resp.getStatusCode().value());
        
        Project p = projectRepo.findById(project.getId()).get();
        assertEquals(ProjectStatus.CONCLUIDO, p.getStatus());
    }

    /**
     * Testa o bloqueio se o ID do aluno na query não bater certo com a sessão.
     */
    @Test
    void testCompleteProject_Forbidden_IdMismatch() {
        setSession(dev);
        // Tentar agir em nome do PO
        ResponseEntity<?> resp = projectController.completeProject(project.getId(), po.getId(), session);
        assertEquals(403, resp.getStatusCode().value());
    }

    /**
     * Testa falha se existirem sprints não concluídos (400 Bad Request).
     */
    @Test
    void testCompleteProject_Fail_SprintsOpen() {
        setSession(po);
        createSprint(SprintStatus.EM_CURSO);
        
        ResponseEntity<?> resp = projectController.completeProject(project.getId(), null, session);
        assertEquals(400, resp.getStatusCode().value());
    }
    
    /**
     * Testa excepção genérica no completeProject (500).
     */
    @Test
    void testCompleteProject_Error500() {
        setSession(po);
        // Passar ID inexistente faz o serviço lançar RuntimeException("Project not found")
        // O controller apanha Exception e devolve 500.
        ResponseEntity<?> resp = projectController.completeProject(9999L, null, session);
        assertEquals(500, resp.getStatusCode().value());
    }

    // ==========================================
    // REOPEN PROJECT
    // ==========================================

    /**
     * Testa a reabertura de projeto sem sessão (401).
     */
    @Test
    void testReopenProject_NoSession() {
        ResponseEntity<?> resp = projectController.reopenProject(project.getId(), null, session);
        assertEquals(401, resp.getStatusCode().value());
    }

  /**
     * Testa reabertura pelo PO com sucesso.
     */
    @Test
    void testReopenProject_Success_PO() {
        // 1. Preparar o cenário: O projeto tem de estar CONCLUIDO na BD
        // Recuperamos uma instância fresca para garantir integridade
        Project pToUpdate = projectRepo.findById(project.getId()).get();
        pToUpdate.setStatus(ProjectStatus.CONCLUIDO);
        projectRepo.save(pToUpdate);
        
        // --- CRÍTICO ---
        // Forçar a gravação e limpar a memória.
        // Isto garante que quando o Controller chamar o Service, o Service vai fazer 
        // um novo SELECT à base de dados e vai carregar corretamente as Equipas associadas.
        entityManager.flush();
        entityManager.clear();
        // ----------------

        setSession(po);
        
        // 2. Ação
        ResponseEntity<?> resp = projectController.reopenProject(project.getId(), null, session);
        
        // 3. Verificação
        assertEquals(200, resp.getStatusCode().value());
        
        Project pResult = projectRepo.findById(project.getId()).get();
        assertEquals(ProjectStatus.EM_CURSO, pResult.getStatus());
    }

    /**
     * Testa reabertura por utilizador não autorizado (403).
     */
    @Test
    void testReopenProject_Forbidden() {
        setSession(dev);
        ResponseEntity<?> resp = projectController.reopenProject(project.getId(), null, session);
        assertEquals(403, resp.getStatusCode().value());
    }
    
    /**
     * Testa erro 500 ao reabrir (ID inválido).
     */
    @Test
    void testReopenProject_Error500() {
        setSession(po);
        ResponseEntity<?> resp = projectController.reopenProject(9999L, null, session);
        assertEquals(500, resp.getStatusCode().value());
    }

    // ==========================================
    // REMOVE TEAM FROM PROJECT
    // ==========================================

    /**
     * Testa a remoção de equipa de um projeto.
     */
    @Test
    void testRemoveTeamFromProject_Success() {
        ResponseEntity<?> resp = projectController.removeTeamFromProject(project.getId(), team.getId());
        assertEquals(200, resp.getStatusCode().value());
        
        Team t = teamRepo.findById(team.getId()).get();
        assertNull(t.getProject());
    }

    /**
     * Testa falha ao remover equipa (Ex: ID inválido -> 500).
     */
    @Test
    void testRemoveTeamFromProject_Fail() {
        ResponseEntity<?> resp = projectController.removeTeamFromProject(project.getId(), 9999L);
        assertEquals(500, resp.getStatusCode().value());
    }

    // --- Helpers ---

    private User createUser(String name, String email, String role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword("pass");
        u.setRole(role);
        return userRepo.save(u);
    }

    private void setSession(User u) {
        session.setAttribute("currentUserId", u.getId());
        session.setAttribute("currentUserRole", u.getRole());
    }
    
    private void createSprint(SprintStatus status) {
        Sprint s = new Sprint();
        s.setProject(project);
        s.setStatus(status);
        s.setName("S1");
        sprintRepo.save(s);
        entityManager.flush();
        entityManager.clear();
        // Recarregar projecto para a sessão ter dados frescos
        project = projectRepo.findById(project.getId()).get();
    }
}