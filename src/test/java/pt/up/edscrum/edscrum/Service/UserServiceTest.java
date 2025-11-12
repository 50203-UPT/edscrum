package pt.up.edscrum.edscrum.Service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.model.User;
import pt.up.edscrum.service.UserService;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    void testCreateAndGetUser() {
        User user = new User();
        user.setName("João Silva");
        user.setEmail("joao@teste.com");
        user.setRole("STUDENT");

        User saved = userService.createUser(user);
        assertNotNull(saved.getId());
        assertEquals("João Silva", saved.getName());
    }

    @Test
    void testGetAllUsers() {
        userService.createUser(new User());
        userService.createUser(new User());
        List<User> users = userService.getAllUsers();
        assertTrue(users.size() >= 2);
    }

    @Test
    void testUpdateUser() {
        User u = new User();
        u.setName("Antigo");
        u.setEmail("antigo@mail.com");
        u.setRole("STUDENT");
        User saved = userService.createUser(u);

        User update = new User();
        update.setName("Novo Nome");
        update.setEmail("novo@mail.com");
        update.setRole("TEACHER");

        User updated = userService.updateUser(saved.getId(), update);
        assertEquals("Novo Nome", updated.getName());
        assertEquals("TEACHER", updated.getRole());
    }

    @Test
    void testDeleteUser() {
        User u = new User();
        u.setName("Apagar");
        User saved = userService.createUser(u);
        userService.deleteUser(saved.getId());

        // Está amarelo no IDE mas está correto, ele está se a queixar porque não faz nada depois. 
        assertThrows(RuntimeException.class, () -> userService.getUserById(saved.getId()));
    }
}
