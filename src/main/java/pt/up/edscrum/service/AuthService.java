package pt.up.edscrum.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.stereotype.Service;

import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.UserRepository;

@Service
/**
 * Serviço responsável por operações de autenticação e recuperação de senha.
 * Contém métodos para login, geração/validação de códigos de recuperação e
 * atualização de palavra-passe dos utilizadores.
 */
public class AuthService {

    private final UserRepository userRepository;

    /**
     * Construtor do serviço de autenticação.
     *
     * @param userRepository repositório para operações com `User`
     */
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Tenta autenticar um utilizador pelo email e palavra-passe.
     *
     * @param email o email do utilizador
     * @param password a palavra-passe fornecida
     * @return o objeto `User` se a autenticação for bem sucedida, ou `null`
     * caso contrário
     */
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

    /**
     * Gera um código de recuperação para o utilizador identificado pelo email e
     * guarda-o na base de dados. O código é válido por um período curto (5
     * minutos) e é impresso no log para fins de teste.
     *
     * @param email o email do utilizador
     * @return `true` se o código foi gerado e guardado com sucesso, `false` se
     * o utilizador não existir
     */
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

            System.out.println("CÓDIGO RECUPERAÇÃO PARA " + email + ": " + code);
            return true;
        }
        return false;
    }

    /**
     * Valida um código de recuperação fornecido para o utilizador.
     *
     * @param email o email do utilizador
     * @param code o código de recuperação recebido
     * @return `true` se o código for válido e ainda estiver dentro do período
     * de validade, `false` caso contrário
     */
    public boolean validateResetCode(String email, String code) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Verifica se o código existe, se é igual ao inserido e se ainda é válido (data)
            if (user.getResetCode() != null
                    && user.getResetCode().equals(code)
                    && user.getResetCodeExpiry() != null
                    && user.getResetCodeExpiry().isAfter(LocalDateTime.now())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Atualiza a palavra-passe do utilizador identificado pelo email. Remove
     * também o código de recuperação e a sua data de expiração para impedir
     * reutilização.
     *
     * @param email o email do utilizador
     * @param newPassword a nova palavra-passe a definir
     */
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
