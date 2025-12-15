package pt.up.edscrum.edscrum.Controller;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.controller.AuthController;
import pt.up.edscrum.dto.LoginRequest;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.*; // Importar todos

/**
 * Testes de integração para o AuthController.
 * <p>
 * Verifica o processo de login e recuperação de password,
 * garantindo a limpeza correta da base de dados para evitar conflitos de FK.
 * </p>
 */
@SpringBootTest
@Transactional
class AuthControllerTest {

    @Autowired private AuthController authController;
    @Autowired private UserRepository userRepository;

    // Repositórios necessários para limpeza (Foreign Keys que bloqueiam delete user)
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private UserStoryRepository userStoryRepo;
    @Autowired private SprintRepository sprintRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private AwardRepository awardRepo;

    private User testUser;
    private MockHttpSession session;

    /**
     * Configuração inicial: Limpa todas as tabelas dependentes antes dos utilizadores.
     * Isto resolve o erro "Constraint Violation" na tabela notifications.
     */
    @BeforeEach
    void setUp() {
        // 1. Limpar dependências (filhos)
        notificationRepo.deleteAll(); // <--- CRÍTICO: Resolve o erro principal
        studentAwardRepo.deleteAll();
        teamAwardRepo.deleteAll();
        scoreRepo.deleteAll();
        userStoryRepo.deleteAll();
        enrollmentRepo.deleteAll();
        
        // 2. Limpar tabelas intermédias
        sprintRepo.deleteAll();
        teamRepo.deleteAll();
        awardRepo.deleteAll();
        projectRepo.deleteAll();
        courseRepo.deleteAll();
        
        // 3. Agora é seguro limpar utilizadores
        userRepository.deleteAll();

        // Criar utilizador para o teste atual
        testUser = new User();
        testUser.setName("Auth Test");
        testUser.setEmail("auth@upt.pt");
        testUser.setPassword("securePass");
        testUser.setRole("STUDENT");
        userRepository.save(testUser);

        session = new MockHttpSession();
    }

    // ==========================================
    // LOGIN TESTS
    // ==========================================

    @Test
    void testLogin_Success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("auth@upt.pt");
        req.setPassword("securePass");

        ResponseEntity<?> resp = authController.login(req, session);

        assertEquals(200, resp.getStatusCode().value());
        User loggedUser = (User) resp.getBody();
        assertEquals("Auth Test", loggedUser.getName());
        
        assertEquals(testUser.getId(), session.getAttribute("currentUserId"));
    }

    @Test
    void testLogin_Fail_WrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setEmail("auth@upt.pt");
        req.setPassword("wrong");

        ResponseEntity<?> resp = authController.login(req, session);
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void testLogin_Fail_UserNotFound() {
        LoginRequest req = new LoginRequest();
        req.setEmail("404@upt.pt");
        req.setPassword("123");

        ResponseEntity<?> resp = authController.login(req, session);
        assertEquals(401, resp.getStatusCode().value());
    }

    // ==========================================
    // RECOVERY TESTS
    // ==========================================

    @Test
    void testSendCode_Success() {
        ResponseEntity<?> resp = authController.sendCode("auth@upt.pt");
        assertEquals(200, resp.getStatusCode().value());
        
        User u = userRepository.findById(testUser.getId()).get();
        assertNotNull(u.getResetCode());
    }

    @Test
    void testSendCode_NotFound() {
        ResponseEntity<?> resp = authController.sendCode("ghost@upt.pt");
        assertEquals(404, resp.getStatusCode().value());
    }

    @Test
    void testResendCode() {
        ResponseEntity<?> resp = authController.resendCode("auth@upt.pt");
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void testVerifyCode_Success() {
        testUser.setResetCode("12345");
        testUser.setResetCodeExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(testUser);

        ResponseEntity<?> resp = authController.verifyCode("auth@upt.pt", "12345");
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void testVerifyCode_Fail_WrongCode() {
        testUser.setResetCode("12345");
        testUser.setResetCodeExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(testUser);

        ResponseEntity<?> resp = authController.verifyCode("auth@upt.pt", "99999");
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void testVerifyCode_Fail_Expired() {
        testUser.setResetCode("12345");
        testUser.setResetCodeExpiry(LocalDateTime.now().minusMinutes(1));
        userRepository.save(testUser);

        ResponseEntity<?> resp = authController.verifyCode("auth@upt.pt", "12345");
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void testVerifyCode_UserNotFound() {
        ResponseEntity<?> resp = authController.verifyCode("ghost@upt.pt", "12345");
        assertEquals(404, resp.getStatusCode().value());
    }
    
    @Test
    void testVerifyCode_NoCodeSet() {
        testUser.setResetCode(null);
        userRepository.save(testUser);
        
        ResponseEntity<?> resp = authController.verifyCode("auth@upt.pt", "12345");
        assertEquals(400, resp.getStatusCode().value());
    }
}