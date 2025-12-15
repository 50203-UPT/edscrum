package pt.up.edscrum.dto;

/**
 * DTO simples usado para transportar credenciais de autenticação (email
 * e password) em pedidos de login.
 */
public class LoginRequest {

    private String email;
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
