package pt.up.edscrum.edscrum.Service;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.CourseRepository;
import pt.up.edscrum.repository.ProjectRepository;
import pt.up.edscrum.repository.TeamRepository;
import pt.up.edscrum.repository.UserRepository;
import pt.up.edscrum.service.AuthService;

@SpringBootTest
@Transactional
public class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @BeforeEach
    void setUp() {
        // Limpar tabelas na ordem correta para não violar FK
        teamRepository.deleteAll();
        courseRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createAndSaveUser(String name, String email, String password, String role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password); // senha em texto para testes
        user.setRole(role);

        // Salvar isoladamente para evitar transient property errors
        return userRepository.save(user);
    }

    @Test
    void testLogin_withValidCredentials_returnsUser() {
        User u = createAndSaveUser("Test User", "user@test.com", "password123", "STUDENT");

        User loggedInUser = authService.login("user@test.com", "password123");

        assertNotNull(loggedInUser);
        assertEquals(u.getEmail(), loggedInUser.getEmail());
    }

    @Test
    void testLogin_withInvalidPassword_returnsNull() {
        createAndSaveUser("Test User", "user@test.com", "password123", "STUDENT");

        User loggedInUser = authService.login("user@test.com", "wrongpassword");

        assertNull(loggedInUser);
    }

    @Test
    void testLogin_withNonExistentUser_returnsNull() {
        User loggedInUser = authService.login("nonexistent@test.com", "anypassword");

        assertNull(loggedInUser);
    }

    @Test
    void testGenerateResetCode_forExistingUser_returnsTrueAndSavesCode() {
        User user = createAndSaveUser("Test User", "user@test.com", "password123", "STUDENT");

        boolean result = authService.generateResetCode("user@test.com");

        assertTrue(result);
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertNotNull(updatedUser.getResetCode());
        assertNotNull(updatedUser.getResetCodeExpiry());
    }

    @Test
    void testGenerateResetCode_forNonExistentUser_returnsFalse() {
        boolean result = authService.generateResetCode("nonexistent@test.com");

        assertFalse(result);
    }

    @Test
    void testValidateResetCode_withValidCode_returnsTrue() {
        User user = createAndSaveUser("Test User", "user@test.com", "password123", "STUDENT");
        user.setResetCode("12345");
        user.setResetCodeExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        boolean isValid = authService.validateResetCode("user@test.com", "12345");

        assertTrue(isValid);
    }

    @Test
    void testValidateResetCode_withInvalidCode_returnsFalse() {
        User user = createAndSaveUser("Test User", "user@test.com", "password123", "STUDENT");
        user.setResetCode("12345");
        user.setResetCodeExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        boolean isValid = authService.validateResetCode("user@test.com", "54321");

        assertFalse(isValid);
    }

    @Test
    void testValidateResetCode_withExpiredCode_returnsFalse() {
        User user = createAndSaveUser("Test User", "user@test.com", "password123", "STUDENT");
        user.setResetCode("12345");
        user.setResetCodeExpiry(LocalDateTime.now().minusMinutes(1));
        userRepository.save(user);

        boolean isValid = authService.validateResetCode("user@test.com", "12345");

        assertFalse(isValid);
    }

    @Test
    void testUpdatePassword_updatesPasswordAndClearsCode() {
        User user = createAndSaveUser("Test User", "user@test.com", "oldPassword", "STUDENT");
        user.setResetCode("12345");
        user.setResetCodeExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        authService.updatePassword("user@test.com", "newStrongPassword");

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals("newStrongPassword", updatedUser.getPassword());
        assertNull(updatedUser.getResetCode());
        assertNull(updatedUser.getResetCodeExpiry());
    }
}
