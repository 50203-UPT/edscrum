package pt.up.edscrum.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpSession;

import pt.up.edscrum.model.User;
import pt.up.edscrum.service.UserService;

@RestController
@RequestMapping("/users")
/**
 * API para gestão de utilizadores (CRUD) com controlo de acesso em endpoints
 * que requerem sessão/role.
 */
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Cria um novo utilizador.
     *
     * @param user Objeto User no corpo da requisição
     * @return Utilizador criado
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User created = userService.createUser(user);
        return ResponseEntity.status(201).body(created);
    }

    /**
     * Obtém a lista de todos os utilizadores.
     *
     * @return Lista de Users
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        if (!"TEACHER".equals(currentUserRole)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Obtém um utilizador por ID.
     *
     * @param id ID do utilizador
     * @return User encontrado
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        if (!currentUserId.equals(id) && !"TEACHER".equals(currentUserRole)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * Atualiza um utilizador existente.
     *
     * @param id ID do utilizador a atualizar
     * @param updatedUser Dados actualizados do utilizador
     * @return Utilizador atualizado
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User updatedUser, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        if (!currentUserId.equals(id) && !"TEACHER".equals(currentUserRole)) return ResponseEntity.status(403).build();
        User u = userService.updateUser(id, updatedUser);
        return ResponseEntity.ok(u);
    }

    /**
     * Elimina um utilizador por ID.
     *
     * @param id ID do utilizador a eliminar
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        if (!currentUserId.equals(id) && !"TEACHER".equals(currentUserRole)) return ResponseEntity.status(403).build();
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
