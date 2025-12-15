package pt.up.edscrum.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.UserRepository;

/**
 * Controlador para páginas de redefinição de password (vistas HTML).
 */
@Controller
public class PasswordWebController {

    private final UserRepository userRepository;

    public PasswordWebController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Mostra a página de redefinição de password e passa o email para o
     * formulário.
     *
     * @param email Email presente na querystring
     * @param model Modelo para a view
     * @return Nome da view de resetPassword
     */
    @GetMapping("/resetPassword")
    public String showResetPasswordPage(@RequestParam("email") String email, Model model) {
        model.addAttribute("email", email);
        return "resetPassword";
    }

    /**
     * Processa o formulário de redefinição de password, atualiza a password do
     * utilizador e limpa o código de reset.
     *
     * @param email Email do utilizador
     * @param password Nova password a definir
     * @param model Modelo (não utilizado aqui)
     * @return Redirecionamento para a página inicial com indicador de sucesso
     */
    @PostMapping("/resetPassword")
    public String processResetPassword(@RequestParam("email") String email,
            @RequestParam("password") String password,
            Model model) {

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return "redirect:/error";
        }

        User user = userOpt.get();

        user.setPassword(password);

        user.setResetCode(null);
        user.setResetCodeExpiry(null);

        userRepository.save(user);

        return "redirect:/?resetSuccess";
    }
}
