package pt.up.edscrum.edscrum.Controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.UserRepository;
import pt.up.edscrum.service.AuthService;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PasswordWebControllerTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Limpar todos os usuários antes de cada teste
        userRepository.deleteAll();

        // Criar e salvar usuário de teste
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("user@test.com");
        testUser.setPassword("oldPassword");
        testUser.setRole("STUDENT");
        testUser = userRepository.save(testUser);
    }

    @Test
    void testGenerateAndValidateResetCode_flow() {
        // 1. Gerar código de reset para usuário existente
        boolean codeGenerated = authService.generateResetCode(testUser.getEmail());
        assertTrue(codeGenerated);

        // 2. Recuperar usuário e verificar se o código foi salvo
        User userWithCode = userRepository.findById(testUser.getId()).orElseThrow();
        String generatedCode = userWithCode.getResetCode();
        assertNotNull(generatedCode);
        assertNotNull(userWithCode.getResetCodeExpiry());

        // 3. Validar o código correto
        boolean isValid = authService.validateResetCode(testUser.getEmail(), generatedCode);
        assertTrue(isValid);

        // 4. Validar um código incorreto
        boolean isInvalid = authService.validateResetCode(testUser.getEmail(), "wrong-code");
        assertFalse(isInvalid);
    }

    @Test
    void testUpdatePassword_flow() {
        // Gerar reset code primeiro (opcional, depende da lógica do AuthService)
        authService.generateResetCode(testUser.getEmail());

        // Atualizar a password
        authService.updatePassword(testUser.getEmail(), "newSecurePassword");

        // Verificar se a password foi atualizada
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("newSecurePassword", updatedUser.getPassword());

        // Verificar se o reset code foi limpo
        assertNull(updatedUser.getResetCode());
        assertNull(updatedUser.getResetCodeExpiry());
    }

    @Test
    void testValidateResetCode_withExpiredCode_returnsFalse() {
        // Gerar código de reset
        authService.generateResetCode(testUser.getEmail());
        User userWithCode = userRepository.findById(testUser.getId()).orElseThrow();
        String generatedCode = userWithCode.getResetCode();

        // Forçar expiração do código
        userWithCode.setResetCodeExpiry(LocalDateTime.now().minusMinutes(10));
        userRepository.save(userWithCode);

        // Validar código expirado
        boolean isValid = authService.validateResetCode(testUser.getEmail(), generatedCode);
        assertFalse(isValid);
    }
}

