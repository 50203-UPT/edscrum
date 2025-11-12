package pt.up.edscrum.edscrum.Controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.model.User;
import pt.up.edscrum.service.UserService;

@SpringBootTest
@Transactional
class UserControllerTest {

    @Autowired
    private UserService userService;

    @Test
    void testCreateAndGetUser() {
        User u = new User();
        u.setName("Alice");
        u.setEmail("alice@example.com");
        u.setRole("STUDENT");
        User saved = userService.createUser(u);

        assertNotNull(saved.getId());
        assertEquals("Alice", saved.getName());

        User found = userService.getUserById(saved.getId());
        assertEquals(saved.getName(), found.getName());
    }

    @Test
    void testGetAllUsers() {
        User u1 = new User();
        u1.setName("User 1");
        u1.setEmail("user1@example.com");
        u1.setRole("STUDENT");
        userService.createUser(u1);

        User u2 = new User();
        u2.setName("User 2");
        u2.setEmail("user2@example.com");
        u2.setRole("TEACHER");
        userService.createUser(u2);

        List<User> users = userService.getAllUsers();
        assertTrue(users.size() >= 2);
    }

    @Test
    void testUpdateUser() {
        User u = new User();
        u.setName("Old Name");
        u.setEmail("old@example.com");
        u.setRole("STUDENT");
        User saved = userService.createUser(u);

        User update = new User();
        update.setName("New Name");
        update.setEmail("new@example.com");
        update.setRole("TEACHER");

        User updated = userService.updateUser(saved.getId(), update);
        assertEquals("New Name", updated.getName());
        assertEquals("new@example.com", updated.getEmail());
        assertEquals("TEACHER", updated.getRole());
    }

    @Test
    void testDeleteUser() {
        User u = new User();
        u.setName("Delete Me");
        u.setEmail("delete@example.com");
        u.setRole("STUDENT");
        User saved = userService.createUser(u);

        userService.deleteUser(saved.getId());

        assertThrows(RuntimeException.class, () -> userService.getUserById(saved.getId()));
    }
}
