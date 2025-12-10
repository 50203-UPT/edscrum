package pt.up.edscrum.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    // Construtor manual (injeção de dependência)
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Listar todos os usuários
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Listar apenas estudantes
    public List<User> getAllStudents() {
        return userRepository.findByRole("STUDENT");
    }

    // Buscar usuário por ID
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User não encontrado"));
    }

    // Buscar usuário por Email
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Criar novo usuário
    public User createUser(User user) {
        return userRepository.save(user);
    }

    // Atualizar usuário existente
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);
        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        user.setPassword(userDetails.getPassword());
        user.setRole(userDetails.getRole());
        return userRepository.save(user);
    }

    // Apagar usuário
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
