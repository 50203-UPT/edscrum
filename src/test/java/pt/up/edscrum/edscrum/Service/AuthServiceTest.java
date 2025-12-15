package pt.up.edscrum.edscrum.Service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.UserRepository;
import pt.up.edscrum.service.AuthService;

@SpringBootTest
@Transactional // Garante rollback após cada teste (base de dados sempre limpa)
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Criar um utilizador real para teste
        testUser = new User();
        testUser.setName("Teste Auth");
        testUser.setEmail("auth@test.com");
        testUser.setPassword("12345"); // Em produção estaria encriptada, mas o serviço compara strings diretas na tua implementação atual
        testUser.setRole("STUDENT");
        userRepository.save(testUser);
    }

    @Test
    void testLogin_Success() {
        User loggedIn = authService.login("auth@test.com", "12345");
        assertNotNull(loggedIn, "O login devia ter sucesso");
        assertEquals(testUser.getId(), loggedIn.getId());
    }

    @Test
    void testLogin_Failure_WrongPassword() {
        User loggedIn = authService.login("auth@test.com", "errada");
        assertNull(loggedIn, "O login devia falhar com password errada");
    }

    @Test
    void testLogin_Failure_UserNotFound() {
        User loggedIn = authService.login("naoexiste@test.com", "12345");
        assertNull(loggedIn, "O login devia falhar para utilizador inexistente");
    }

    @Test
    void testGenerateResetCode_Success() {
        boolean result = authService.generateResetCode("auth@test.com");
        assertTrue(result, "Devia gerar código de reset");

        User updatedUser = userRepository.findByEmail("auth@test.com").get();
        assertNotNull(updatedUser.getResetCode());
        assertNotNull(updatedUser.getResetCodeExpiry());
    }

    @Test
    void testGenerateResetCode_UserNotFound() {
        boolean result = authService.generateResetCode("naoexiste@test.com");
        assertFalse(result, "Não devia gerar código para email inexistente");
    }

    @Test
    void testValidateResetCode_Success() {
        // Configurar código manualmente
        testUser.setResetCode("54321");
        testUser.setResetCodeExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(testUser);

        boolean isValid = authService.validateResetCode("auth@test.com", "54321");
        assertTrue(isValid, "Código devia ser válido");
    }

    @Test
    void testValidateResetCode_Expired() {
        testUser.setResetCode("54321");
        testUser.setResetCodeExpiry(LocalDateTime.now().minusMinutes(1)); // Expirado
        userRepository.save(testUser);

        boolean isValid = authService.validateResetCode("auth@test.com", "54321");
        assertFalse(isValid, "Código expirado não devia ser válido");
    }

    @Test
    void testValidateResetCode_WrongCode() {
        testUser.setResetCode("54321");
        testUser.setResetCodeExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(testUser);

        boolean isValid = authService.validateResetCode("auth@test.com", "00000");
        assertFalse(isValid, "Código incorreto não devia passar");
    }

    @Test
    void testUpdatePassword() {
        testUser.setResetCode("12345");
        userRepository.save(testUser);

        authService.updatePassword("auth@test.com", "novaPass123");

        User updatedUser = userRepository.findByEmail("auth@test.com").get();
        assertEquals("novaPass123", updatedUser.getPassword());
        assertNull(updatedUser.getResetCode(), "O código de reset devia ser limpo após atualizar a password");
    }
}