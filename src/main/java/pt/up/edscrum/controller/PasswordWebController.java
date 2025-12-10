package pt.up.edscrum.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.UserRepository;

import java.util.Optional;

@Controller // <--- Nota: @Controller e não @RestController, porque serve HTML
public class PasswordWebController {

    private final UserRepository userRepository;

    public PasswordWebController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 1. MOSTRAR A PÁGINA DE RESET (Resolve o teu erro 404)
    // Este método apanha o pedido GET /resetPassword?email=...
    @GetMapping("/resetPassword")
    public String showResetPasswordPage(@RequestParam("email") String email, Model model) {
        // Passamos o email para o HTML para ele colocar no input hidden
        model.addAttribute("email", email);
        
        // Retorna o nome do ficheiro HTML (sem .html) que está em resources/templates
        return "resetPassword"; 
    }

    // 2. PROCESSAR O FORMULÁRIO DE NOVA PALAVRA-PASSE
    // Este método apanha o POST do formulário
    @PostMapping("/resetPassword")
    public String processResetPassword(@RequestParam("email") String email,
                                       @RequestParam("password") String password,
                                       Model model) {
        
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return "redirect:/error"; // Ou outra página de erro
        }

        User user = userOpt.get();

        // --- ATUALIZAR A PASSWORD ---
        // Se estiveres a usar BCryptPasswordEncoder, deves fazer: user.setPassword(encoder.encode(password));
        // Como não vi config de segurança, vou guardar direto (texto simples):
        user.setPassword(password);

        // Limpar o código de recuperação para não ser usado novamente
        user.setResetCode(null);
        user.setResetCodeExpiry(null);

        userRepository.save(user);

        // Redireciona para o login (ou raiz) com um parâmetro de sucesso
        return "redirect:/?resetSuccess"; 
    }
}
