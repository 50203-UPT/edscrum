package pt.up.edscrum.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

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
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    public boolean generateResetCode(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Gerar código de 5 dígitos
            String code = String.format("%05d", new Random().nextInt(100000));
            
            // Guardar na BD (Válido por 5 minutos)
            user.setResetCode(code);
            user.setResetCodeExpiry(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);

            // LOG para veres o código (já que não há envio de email real)
            System.out.println("CÓDIGO RECUPERAÇÃO PARA " + email + ": " + code);
            return true;
        }
        return false;
    }

    public boolean validateResetCode(String email, String code) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Verifica se o código existe, se é igual ao inserido e se ainda é válido (data)
            if (user.getResetCode() != null && 
                user.getResetCode().equals(code) && 
                user.getResetCodeExpiry() != null && 
                user.getResetCodeExpiry().isAfter(LocalDateTime.now())) {
                return true;
            }
        }
        return false;
    }

    public void updatePassword(String email, String newPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(newPassword);
            
            // Limpa o código para não ser usado outra vez
            user.setResetCode(null);
            user.setResetCodeExpiry(null);
            
            userRepository.save(user);
        }
    }
}