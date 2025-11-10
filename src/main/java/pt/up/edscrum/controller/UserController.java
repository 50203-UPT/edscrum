package pt.up.edscrum.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.UserRepository;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository repo) {
        this.userRepository = repo;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
