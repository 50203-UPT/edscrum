package pt.up.edscrum.controller;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.up.edscrum.dto.LoginRequest;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.UserRepository;
import pt.up.edscrum.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    /**
     * Autentica um utilizador com email e password.
     *
     * @param request Objeto com email e password
     * @return ResponseEntity com o User autenticado ou erro 401
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        User user = authService.login(request.getEmail(), request.getPassword());

        if (user == null) {
            return ResponseEntity.status(401).body("Credenciais inválidas");
        }

        return ResponseEntity.ok(user);
    }

    /**
     * Gera e envia (simulado) um código de recuperação para o email.
     *
     * @param email Email do utilizador
     * @return ResponseEntity com resultado da operação
     */
    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Email não encontrado.");
        }

        User user = userOpt.get();

        // Gerar código de 5 dígitos
        String code = String.format("%05d", new Random().nextInt(100000));

        // Guardar na BD com validade de 1 minuto
        user.setResetCode(code);
        user.setResetCodeExpiry(LocalDateTime.now().plusMinutes(1));
        userRepository.save(user);

        // --- SIMULAÇÃO DE EMAIL NA CONSOLA ---
        System.out.println("=========================================");
        System.out.println("EMAIL PARA: " + email);
        System.out.println("CÓDIGO DE RECUPERAÇÃO: " + code);
        System.out.println("=========================================");

        return ResponseEntity.ok("Código enviado com sucesso.");
    }

    /**
     * Reenvia o código de recuperação (reaproveita a mesma lógica).
     *
     * @param email Email do utilizador
     * @return ResponseEntity com o resultado
     */
    @PostMapping("/resend-code")
    public ResponseEntity<?> resendCode(@RequestParam String email) {
        return sendCode(email);
    }

    /**
     * Verifica se o código de recuperação é válido e não expirou.
     *
     * @param email Email do utilizador
     * @param code Código submetido
     * @return ResponseEntity com resultado da validação
     */
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestParam String email, @RequestParam String code) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Utilizador não encontrado.");
        }

        User user = userOpt.get();

        // Verificar se o código existe e corresponde
        if (user.getResetCode() == null || !user.getResetCode().equals(code)) {
            return ResponseEntity.badRequest().body("Código incorreto.");
        }

        // Verificar se o código já expirou
        if (user.getResetCodeExpiry() != null && user.getResetCodeExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("O código expirou. Por favor, peça um novo.");
        }

        // Se chegou aqui, está validado
        return ResponseEntity.ok("Código válido.");
    }
}
