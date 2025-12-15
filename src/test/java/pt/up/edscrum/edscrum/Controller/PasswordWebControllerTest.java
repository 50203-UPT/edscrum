package pt.up.edscrum.edscrum.Controller;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import pt.up.edscrum.controller.PasswordWebController;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.*;

/**
 * Testes de integração para o PasswordWebController.
 * <p>
 * Verifica o fluxo de redefinição de password, garantindo que
 * a view correta é retornada e que a base de dados é atualizada
 * corretamente (password alterada e códigos de reset limpos).
 * </p>
 */
@SpringBootTest
@Transactional
class PasswordWebControllerTest {

    @Autowired
    private PasswordWebController passwordWebController;

    @Autowired
    private UserRepository userRepository;

    // Repositórios Adicionais para Limpeza de Dependências (Foreign Keys)
    // Necessário porque outros testes podem deixar "lixo" que impede apagar Users.
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
    private Model model;

    /**
     * Configuração inicial: Limpa a base de dados e cria um utilizador de teste.
     * A ordem de limpeza é crítica para evitar DataIntegrityViolationException.
     */
    @BeforeEach
    void setUp() {
        // 1. Limpar tabelas dependentes (Filhos)
        notificationRepo.deleteAll(); // <--- Resolve o erro específico das notificações
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
        
        // 3. Limpar tabela Raiz (Users)
        userRepository.deleteAll();

        // --- Setup do Cenário de Teste ---
        
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@upt.pt");
        testUser.setPassword("oldPassword");
        testUser.setRole("STUDENT");
        
        // Simular um estado onde o pedido de reset já foi feito
        testUser.setResetCode("123456");
        testUser.setResetCodeExpiry(LocalDateTime.now().plusMinutes(10));
        
        userRepository.save(testUser);

        // Modelo usado para passar atributos para a view
        model = new ConcurrentModel();
    }

    /**
     * Testa a exibição da página de reset (GET).
     */
    @Test
    void testShowResetPasswordPage() {
        String email = "test@upt.pt";
        
        String viewName = passwordWebController.showResetPasswordPage(email, model);

        assertEquals("resetPassword", viewName);
        assertTrue(model.containsAttribute("email"));
        assertEquals(email, model.getAttribute("email"));
    }

    /**
     * Testa o processamento do reset de password com sucesso (POST).
     */
    @Test
    void testProcessResetPassword_Success() {
        String newPassword = "newSecurePassword";
        
        String viewName = passwordWebController.processResetPassword("test@upt.pt", newPassword, model);

        // Verificar redirecionamento
        assertEquals("redirect:/?resetSuccess", viewName);

        // Verificar persistência na base de dados
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        
        assertEquals(newPassword, updatedUser.getPassword(), "A password deve ser atualizada.");
        assertNull(updatedUser.getResetCode(), "O código de reset deve ser limpo.");
        assertNull(updatedUser.getResetCodeExpiry(), "A validade do código deve ser limpa.");
    }

    /**
     * Testa o processamento do reset de password quando o utilizador não existe (POST).
     */
    @Test
    void testProcessResetPassword_UserNotFound() {
        String viewName = passwordWebController.processResetPassword("naoexiste@upt.pt", "newPass", model);

        assertEquals("redirect:/error", viewName);
    }
}