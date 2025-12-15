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
import pt.up.edscrum.controller.UserController;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.*;

/**
 * Testes de integração para o UserController.
 * <p>
 * Verifica as operações CRUD sobre Utilizadores e valida rigorosamente
 * as regras de controlo de acesso (RBAC) baseadas em sessão e papéis (TEACHER vs STUDENT).
 * </p>
 */
@SpringBootTest
@Transactional
class UserControllerTest {

    @Autowired private UserController userController;
    @Autowired private EntityManager entityManager;

    @Autowired private UserRepository userRepo;
    
    // Repositórios extra para limpeza de dependências (FKs)
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private UserStoryRepository userStoryRepo;
    @Autowired private SprintRepository sprintRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private AwardRepository awardRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private CourseRepository courseRepo;

    private User teacher;
    private User student;
    private User otherStudent;
    
    private MockHttpSession session;

    /**
     * Configuração inicial: Limpa a base de dados e cria utilizadores de teste.
     */
    @BeforeEach
    void setUp() {
        // 1. Limpeza profunda por ordem de dependência
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
        teacher = createUser("Prof Test", "prof@upt.pt", "TEACHER");
        student = createUser("Aluno Test", "aluno@upt.pt", "STUDENT");
        otherStudent = createUser("Outro Aluno", "outro@upt.pt", "STUDENT");

        session = new MockHttpSession();
        
        entityManager.flush();
        entityManager.clear();
    }

    // ==========================================
    // CREATE USER (POST)
    // ==========================================

    /**
     * Testa a criação de um novo utilizador (registo público).
     */
    @Test
    void testCreateUser() {
        User newUser = new User();
        newUser.setName("New User");
        newUser.setEmail("new@upt.pt");
        newUser.setPassword("123");
        newUser.setRole("STUDENT");

        ResponseEntity<User> resp = userController.createUser(newUser);

        assertEquals(201, resp.getStatusCode().value());
        assertNotNull(resp.getBody().getId());
        assertTrue(userRepo.existsById(resp.getBody().getId()));
    }

    // ==========================================
    // GET ALL USERS (GET)
    // ==========================================

    /**
     * Testa a tentativa de listar utilizadores sem sessão (401).
     */
    @Test
    void testGetAllUsers_NoSession() {
        ResponseEntity<List<User>> resp = userController.getAllUsers(session);
        assertEquals(401, resp.getStatusCode().value());
    }

    /**
     * Testa a tentativa de listar utilizadores com papel de estudante (403).
     */
    @Test
    void testGetAllUsers_Forbidden_Student() {
        setSession(student);
        ResponseEntity<List<User>> resp = userController.getAllUsers(session);
        assertEquals(403, resp.getStatusCode().value());
    }

    /**
     * Testa a listagem de utilizadores por um professor (200).
     */
    @Test
    void testGetAllUsers_Success_Teacher() {
        setSession(teacher);
        ResponseEntity<List<User>> resp = userController.getAllUsers(session);
        
        assertEquals(200, resp.getStatusCode().value());
        assertFalse(resp.getBody().isEmpty());
        assertTrue(resp.getBody().size() >= 3); // Teacher + Student + Other
    }

    // ==========================================
    // GET USER BY ID (GET /{id})
    // ==========================================

    /**
     * Testa obter utilizador sem sessão (401).
     */
    @Test
    void testGetUserById_NoSession() {
        ResponseEntity<User> resp = userController.getUserById(student.getId(), session);
        assertEquals(401, resp.getStatusCode().value());
    }

    /**
     * Testa um estudante a aceder ao seu próprio perfil (200).
     */
    @Test
    void testGetUserById_Success_Self() {
        setSession(student);
        ResponseEntity<User> resp = userController.getUserById(student.getId(), session);
        
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Aluno Test", resp.getBody().getName());
    }

    /**
     * Testa um professor a aceder ao perfil de um aluno (200).
     */
    @Test
    void testGetUserById_Success_Teacher() {
        setSession(teacher);
        ResponseEntity<User> resp = userController.getUserById(student.getId(), session);
        
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Aluno Test", resp.getBody().getName());
    }

    /**
     * Testa um estudante a tentar aceder ao perfil de outro (403).
     */
    @Test
    void testGetUserById_Forbidden_Other() {
        setSession(student);
        ResponseEntity<User> resp = userController.getUserById(otherStudent.getId(), session);
        assertEquals(403, resp.getStatusCode().value());
    }

    // ==========================================
    // UPDATE USER (PUT /{id})
    // ==========================================

    /**
     * Testa atualização sem sessão (401).
     */
    @Test
    void testUpdateUser_NoSession() {
        ResponseEntity<User> resp = userController.updateUser(student.getId(), new User(), session);
        assertEquals(401, resp.getStatusCode().value());
    }

    /**
     * Testa atualização do próprio perfil (200).
     */
    @Test
    void testUpdateUser_Success_Self() {
        setSession(student);
        User updateData = new User();
        updateData.setName("Aluno Updated");
        
        ResponseEntity<User> resp = userController.updateUser(student.getId(), updateData, session);
        
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Aluno Updated", resp.getBody().getName());
        
        User dbUser = userRepo.findById(student.getId()).get();
        assertEquals("Aluno Updated", dbUser.getName());
    }

    /**
     * Testa professor a atualizar perfil de aluno (200).
     */
    @Test
    void testUpdateUser_Success_Teacher() {
        setSession(teacher);
        User updateData = new User();
        updateData.setName("Updated by Teacher");
        
        ResponseEntity<User> resp = userController.updateUser(student.getId(), updateData, session);
        
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Updated by Teacher", resp.getBody().getName());
    }

    /**
     * Testa estudante a tentar atualizar perfil de outro (403).
     */
    @Test
    void testUpdateUser_Forbidden_Other() {
        setSession(student);
        User updateData = new User();
        updateData.setName("Hacked");
        
        ResponseEntity<User> resp = userController.updateUser(otherStudent.getId(), updateData, session);
        assertEquals(403, resp.getStatusCode().value());
    }

    // ==========================================
    // DELETE USER (DELETE /{id})
    // ==========================================

    /**
     * Testa eliminação sem sessão (401).
     */
    @Test
    void testDeleteUser_NoSession() {
        ResponseEntity<Void> resp = userController.deleteUser(student.getId(), session);
        assertEquals(401, resp.getStatusCode().value());
    }

    /**
     * Testa eliminação da própria conta (204).
     */
    @Test
    void testDeleteUser_Success_Self() {
        setSession(otherStudent); // Usar otherStudent para não afetar testes seguintes se a ordem mudar
        
        ResponseEntity<Void> resp = userController.deleteUser(otherStudent.getId(), session);
        
        assertEquals(204, resp.getStatusCode().value());
        assertFalse(userRepo.existsById(otherStudent.getId()));
    }

    /**
     * Testa professor a eliminar conta de aluno (204).
     */
    @Test
    void testDeleteUser_Success_Teacher() {
        setSession(teacher);
        
        ResponseEntity<Void> resp = userController.deleteUser(student.getId(), session);
        
        assertEquals(204, resp.getStatusCode().value());
        assertFalse(userRepo.existsById(student.getId()));
    }

    /**
     * Testa estudante a tentar eliminar conta de outro (403).
     */
    @Test
    void testDeleteUser_Forbidden_Other() {
        setSession(student);
        
        ResponseEntity<Void> resp = userController.deleteUser(teacher.getId(), session); // Tentar apagar o professor
        assertEquals(403, resp.getStatusCode().value());
        assertTrue(userRepo.existsById(teacher.getId()));
    }

    // --- Helpers ---

    private User createUser(String name, String email, String role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword("123");
        u.setRole(role);
        return userRepo.save(u);
    }

    private void setSession(User u) {
        session.setAttribute("currentUserId", u.getId());
        session.setAttribute("currentUserRole", u.getRole());
    }
}