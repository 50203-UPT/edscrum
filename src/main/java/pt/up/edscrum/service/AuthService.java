package pt.up.edscrum.service;

import org.springframework.stereotype.Service;

import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User login(String email, String password) {
        User user = userRepository.findByEmail(email);

        if (user == null || !user.getPassword().equals(password)) {
            return null; // credenciais inválidas
        }

        return user; // login OK
    }
}
