package pt.up.edscrum.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.UserRepository;

/**
 * Serviço para operações CRUD e utilitárias sobre utilizadores.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Obtém todos os utilizadores.
     *
     * @return Lista de User
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Obtém todos os utilizadores com papel STUDENT.
     *
     * @return Lista de User com role STUDENT
     */
    public List<User> getAllStudents() {
        return userRepository.findByRole("STUDENT");
    }

    /**
     * Obtém um utilizador pelo ID.
     *
     * @param id ID do utilizador
     * @return User encontrado
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User não encontrado"));
    }

    /**
     * Procura um utilizador por email.
     *
     * @param email Email a procurar
     * @return Optional contendo o User se existir
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Cria um novo utilizador.
     *
     * @param user Dados do utilizador
     * @return User criado
     */
    public User createUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Atualiza um utilizador existente com os dados fornecidos.
     *
     * @param id ID do utilizador a atualizar
     * @param userDetails Dados actualizados
     * @return User atualizado
     */
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);

        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());

        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(userDetails.getPassword());
        }

        user.setRole(userDetails.getRole());

        user.setNotificationAwards(userDetails.isNotificationAwards());
        user.setNotificationRankings(userDetails.isNotificationRankings());

        if (userDetails.getProfileImage() != null) {
            user.setProfileImage(userDetails.getProfileImage());
        }

        return userRepository.save(user);
    }

    /**
     * Elimina um utilizador pelo ID.
     *
     * @param id ID do utilizador a eliminar
     */
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
