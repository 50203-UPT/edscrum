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
import pt.up.edscrum.controller.UserStoryController;
import pt.up.edscrum.enums.ProjectStatus;
import pt.up.edscrum.enums.SprintStatus;
import pt.up.edscrum.enums.UserStoryPriority;
import pt.up.edscrum.enums.UserStoryStatus;
import pt.up.edscrum.model.*;
import pt.up.edscrum.repository.*;

/**
 * Testes de integração para o UserStoryController.
 * <p>
 * Verifica o fluxo completo desde o pedido HTTP (simulado via chamada direta ao método)
 * até à persistência na base de dados H2, passando pelas validações de segurança
 * e regras de negócio do serviço.
 * </p>
 */
@SpringBootTest
@Transactional
class UserStoryControllerTest {

    @Autowired private UserStoryController userStoryController;
    @Autowired private EntityManager entityManager;

    // Repositórios
    @Autowired private UserRepository userRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private SprintRepository sprintRepo;
    @Autowired private UserStoryRepository userStoryRepo;
    
    // Repositórios extra para limpeza
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private AwardRepository awardRepo;

    private User teacher;
    private User creator; // Scrum Master
    private User dev;     // Developer
    private User stranger;
    
    private Course course;
    private Project project;
    private Team team;
    private Sprint sprint;
    
    private MockHttpSession session;

    /**
     * Configuração inicial do cenário de teste.
     * Limpa a BD e cria uma hierarquia completa:
     * User -> Course -> Project -> Team -> Sprint.
     */
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

        // 2. Criar Utilizadores
        teacher = createUser("Teacher", "teach@upt.pt", "TEACHER");
        creator = createUser("ScrumMaster", "sm@upt.pt", "STUDENT");
        dev = createUser("Dev", "dev@upt.pt", "STUDENT");
        stranger = createUser("Stranger", "stranger@upt.pt", "STUDENT");

        // 3. Criar Hierarquia
        course = new Course();
        course.setName("Agile Course");
        course.setTeacher(teacher);
        course = courseRepo.save(course);

        project = new Project();
        project.setName("Project US");
        project.setCourse(course);
        project.setStatus(ProjectStatus.EM_CURSO);
        project = projectRepo.save(project);

        team = new Team();
        team.setName("Team US");
        team.setCourse(course);
        team.setProject(project);
        team.setScrumMaster(creator); // Creator é dono
        // Adicionar dev à equipa
        List<User> devs = new java.util.ArrayList<>();
        devs.add(dev);
        team.setDevelopers(devs);
        team = teamRepo.save(team);

        sprint = new Sprint();
        sprint.setName("Sprint 1");
        sprint.setProject(project);
        sprint.setCreatedBy(creator);
        sprint.setStatus(SprintStatus.EM_CURSO);
        sprint = sprintRepo.save(sprint);

        session = new MockHttpSession();
        
        entityManager.flush();
        entityManager.clear();
    }

    // ==========================================
    // GET STORIES
    // ==========================================

    /**
     * Verifica se o endpoint bloqueia acesso sem sessão.
     */
    @Test
    void testGetSprintStories_NoSession() {
        ResponseEntity<List<UserStory>> resp = userStoryController.getSprintStories(sprint.getId(), session);
        assertEquals(401, resp.getStatusCode().value());
    }

    /**
     * Verifica a obtenção de user stories com sucesso.
     */
    @Test
    void testGetSprintStories_Success() {
        // Criar uma story manualmente
        createStoryInDb("Story 1", UserStoryStatus.TODO);
        
        // Recarregar sprint para garantir que a lista de user stories está atualizada
        entityManager.flush();
        entityManager.clear();
        sprint = sprintRepo.findById(sprint.getId()).get();

        setSession(creator);
        ResponseEntity<List<UserStory>> resp = userStoryController.getSprintStories(sprint.getId(), session);
        
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertFalse(resp.getBody().isEmpty());
        assertEquals("Story 1", resp.getBody().get(0).getName());
    }

    // ==========================================
    // CREATE STORY
    // ==========================================

    /**
     * Testa a criação de uma User Story por um membro da equipa (sucesso).
     */
    @Test
    void testCreateUserStory_Success_Member() {
        setSession(creator);
        
        UserStory us = new UserStory();
        us.setName("Nova Funcionalidade");
        us.setPriority(UserStoryPriority.HIGH);
        us.setStoryPoints(5);
        us.setSprint(sprint); // Define o sprint no objeto
        
        // Atribuir ao developer pelo ID (simulando payload do frontend)
        User assignee = new User(); assignee.setId(dev.getId());
        us.setAssignee(assignee);

        // CORREÇÃO: O controller só recebe (UserStory, Session)
        ResponseEntity<UserStory> resp = userStoryController.createUserStory(us, session);
        
        assertEquals(200, resp.getStatusCode().value()); // Controller retorna 200 OK
        assertNotNull(resp.getBody().getId());
        assertEquals("Nova Funcionalidade", resp.getBody().getName());
        assertEquals(dev.getId(), resp.getBody().getAssignee().getId());
    }

    /**
     * Testa a criação de uma User Story atribuindo o assignee por Email.
     */
    @Test
    void testCreateUserStory_Success_ByEmail() {
        setSession(creator);
        
        UserStory us = new UserStory();
        us.setName("Story Email");
        us.setSprint(sprint);
        User assignee = new User(); assignee.setEmail(dev.getEmail());
        us.setAssignee(assignee);

        ResponseEntity<UserStory> resp = userStoryController.createUserStory(us, session);
        
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(dev.getEmail(), resp.getBody().getAssignee().getEmail());
    }

    /**
     * Testa se um utilizador não autorizado (não membro) é bloqueado ao tentar criar story.
     */
    @Test
    void testCreateUserStory_Forbidden_Stranger() {
        setSession(stranger); // Não pertence à equipa do projeto
        
        UserStory us = new UserStory();
        us.setName("Hacked Story");
        // Se tentar atribuir a alguém da equipa
        User assignee = new User(); assignee.setId(dev.getId());
        us.setAssignee(assignee);
        us.setSprint(sprint);

        ResponseEntity<UserStory> resp = userStoryController.createUserStory(us, session);
        
        assertEquals(403, resp.getStatusCode().value());
    }

    // ==========================================
    // UPDATE STORY (Full Update)
    // ==========================================

    /**
     * Testa a atualização completa de uma história (nome, pontos, assignee).
     */
    @Test
    void testUpdateUserStory_Success() {
        UserStory original = createStoryInDb("Old Name", UserStoryStatus.TODO);
        
        setSession(creator);
        
        UserStory update = new UserStory();
        update.setName("New Name");
        update.setStoryPoints(8);
        update.setPriority(UserStoryPriority.LOW);
        // Manter o assignee atual ou mudar
        User assignee = new User(); assignee.setId(creator.getId());
        update.setAssignee(assignee);
        
        ResponseEntity<UserStory> resp = userStoryController.updateUserStory(original.getId(), update, session);
        
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("New Name", resp.getBody().getName());
        assertEquals(8, resp.getBody().getStoryPoints());
    }

    /**
     * Testa a permissão de edição (apenas membros ou professores).
     */
    @Test
    void testUpdateUserStory_Forbidden() {
        UserStory original = createStoryInDb("Protected", UserStoryStatus.TODO);
        // Atribuir a alguém para ativar verificação de permissão
        original.setAssignee(creator);
        userStoryRepo.save(original);
        
        setSession(stranger);
        
        UserStory update = new UserStory();
        update.setName("Hacked");
        User assignee = new User(); assignee.setId(creator.getId());
        update.setAssignee(assignee);
        
        ResponseEntity<UserStory> resp = userStoryController.updateUserStory(original.getId(), update, session);
        
        assertEquals(403, resp.getStatusCode().value());
    }

    // ==========================================
    // UPDATE STATUS (Drag & Drop)
    // ==========================================

    /**
     * Testa a atualização apenas do estado (movimento no quadro Kanban).
     */
    @Test
    void testMoveUserStory_Success() {
        UserStory original = createStoryInDb("Task", UserStoryStatus.TODO);
        
        setSession(dev); // Developer move a tarefa
        
        // CORREÇÃO: Usar moveUserStory e passar String
        ResponseEntity<UserStory> resp = userStoryController.moveUserStory(original.getId(), "IN_PROGRESS", session);
        
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(UserStoryStatus.IN_PROGRESS, resp.getBody().getStatus());
    }

    // ==========================================
    // DELETE STORY
    // ==========================================

    /**
     * Testa a eliminação de uma user story.
     */
    @Test
    void testDeleteUserStory_Success() {
        UserStory us = createStoryInDb("To Delete", UserStoryStatus.TODO);
        
        setSession(creator);
        
        ResponseEntity<Void> resp = userStoryController.deleteUserStory(us.getId(), session);
        
        assertEquals(200, resp.getStatusCode().value()); // Controller retorna 200 OK
        assertFalse(userStoryRepo.existsById(us.getId()));
    }

    /**
     * Testa se a eliminação é bloqueada para não membros.
     */
    @Test
    void testDeleteUserStory_Forbidden() {
        UserStory us = createStoryInDb("Protected", UserStoryStatus.TODO);
        
        setSession(stranger);
        
        ResponseEntity<Void> resp = userStoryController.deleteUserStory(us.getId(), session);
        
        assertEquals(403, resp.getStatusCode().value());
        assertTrue(userStoryRepo.existsById(us.getId()));
    }

    // ==========================================
    // HELPERS
    // ==========================================

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
    
    private UserStory createStoryInDb(String name, UserStoryStatus status) {
        UserStory us = new UserStory();
        us.setName(name);
        us.setStatus(status);
        us.setSprint(sprint); // Importante associar ao sprint do setup
        // us.setProject(project); // REMOVIDO: Método não existe
        return userStoryRepo.save(us);
    }
}