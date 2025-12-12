/*package pt.up.edscrum.edscrum.Controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.support.BindingAwareModelMap;
import pt.up.edscrum.controller.PasswordWebController;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PasswordWebControllerTest {

    @Autowired
    private PasswordWebController passwordWebController;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Model model;

    @BeforeEach
    void setUp() {
        // Clear any existing test data
        userRepository.deleteAll();

        // Setup test user
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("oldPassword");
        testUser.setRole("STUDENT");
        testUser.setResetCode("12345");
        testUser.setResetCodeExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(testUser);

        // Setup model
        model = new BindingAwareModelMap();
    }

    @Test
    void processResetPassword_WithValidData_UpdatesPasswordAndClearsResetInfo() {
        // Given
        String newPassword = "newSecurePassword123";
        String email = testUser.getEmail();

        // When
        String result = passwordWebController.processResetPassword(email, newPassword, model);

        // Then
        assertEquals("redirect:/?resetSuccess", result);

        // Verify the user was updated in the database
        Optional<User> updatedUserOpt = userRepository.findByEmail(email);
        assertTrue(updatedUserOpt.isPresent());

        User updatedUser = updatedUserOpt.get();
        assertEquals(newPassword, updatedUser.getPassword());
        assertNull(updatedUser.getResetCode());
        assertNull(updatedUser.getResetCodeExpiry());
    }

    @Test
    void processResetPassword_WithNonExistentEmail_ReturnsErrorPage() {
        // Given
        String nonExistentEmail = "nonexistent@example.com";
        String newPassword = "newPassword123";

        // When
        String result = passwordWebController.processResetPassword(nonExistentEmail, newPassword, model);

        // Then
        assertEquals("redirect:/error", result);
    }

    @Test
    void processResetPassword_WithEmptyPassword_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            passwordWebController.processResetPassword(
                    testUser.getEmail(),
                    "",
                    model
            );
        });
    }

    @Test
    void processResetPassword_WithNullPassword_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            passwordWebController.processResetPassword(
                    testUser.getEmail(),
                    null,
                    model
            );
        });
    }

    @Test
    void processResetPassword_WithExpiredResetCode_StillProcesses() {
        // Given
        testUser.setResetCodeExpiry(LocalDateTime.now().minusHours(1));
        userRepository.save(testUser);

        String newPassword = "newPassword123";

        // When
        String result = passwordWebController.processResetPassword(
                testUser.getEmail(),
                newPassword,
                model
        );

        // Then - Should still process even with expired code
        assertEquals("redirect:/?resetSuccess", result);

        // Verify password was still updated
        Optional<User> updatedUser = userRepository.findByEmail(testUser.getEmail());
        assertTrue(updatedUser.isPresent());
        assertEquals(newPassword, updatedUser.get().getPassword());
    }
}*/