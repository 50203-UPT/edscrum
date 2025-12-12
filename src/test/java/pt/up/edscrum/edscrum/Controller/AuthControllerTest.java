package pt.up.edscrum.edscrum.Controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.CourseRepository;
import pt.up.edscrum.repository.TeamRepository;
import pt.up.edscrum.repository.UserRepository;
import pt.up.edscrum.repository.ProjectRepository;
import pt.up.edscrum.service.AuthService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AuthControllerTest {

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

        // Salvar User isoladamente para evitar transient property error
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
}

