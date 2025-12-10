package pt.up.edscrum.service;

import java.util.Optional;

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
        // 1. Procurar o utilizador (retorna Optional porque pode não existir)
        Optional<User> userOpt = userRepository.findByEmail(email);

        // 2. Verificar se existe dentro do Optional
        if (userOpt.isPresent()) {
            User user = userOpt.get(); // "Desembrulha" o utilizador

            // 3. Verificar a palavra-passe
            // Nota: Se estiveres a usar encriptação no futuro, usa passwordEncoder.matches()
            if (user.getPassword().equals(password)) {
                return user; // Sucesso
            }
        }

        // Se o email não existir OU a password estiver errada, retorna null
        return null;
    }

    // Método auxiliar usado no AuthController antigo (opcional, mantido para compatibilidade)
    public boolean generateResetCode(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}