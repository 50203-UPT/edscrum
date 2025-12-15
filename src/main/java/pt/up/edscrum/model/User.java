package pt.up.edscrum.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Representa um utilizador do sistema (professor ou estudante). Contém
 * dados de autenticação, preferências e informação de recuperação.
 */
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Column(unique = true)
    private String email;
    private String password;
    private String role;

    private boolean notificationAwards = true;
    private boolean notificationRankings = true;

    private String profileImage;

    @JsonIgnore
    private String resetCode;
    @JsonIgnore
    private LocalDateTime resetCodeExpiry;

    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isNotificationAwards() {
        return notificationAwards;
    }

    public void setNotificationAwards(boolean notificationAwards) {
        this.notificationAwards = notificationAwards;
    }

    public boolean isNotificationRankings() {
        return notificationRankings;
    }

    public void setNotificationRankings(boolean notificationRankings) {
        this.notificationRankings = notificationRankings;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getResetCode() {
        return resetCode;
    }

    public void setResetCode(String resetCode) {
        this.resetCode = resetCode;
    }

    public LocalDateTime getResetCodeExpiry() {
        return resetCodeExpiry;
    }

    public void setResetCodeExpiry(LocalDateTime resetCodeExpiry) {
        this.resetCodeExpiry = resetCodeExpiry;
    }

    /**
     * Retorna a tag de estudante no formato "{id}-UPT" quando o ID estiver
     * disponível. Se o ID ainda não foi gerado, devolve "Novo".
     *
     * @return tag de estudante ou "Novo"
     */
    public String getStudentTag() {
        if (this.id != null) {
            return this.id + "-UPT";
        }
        return "Novo";
    }

    /**
     * Representação em string do utilizador (útil para logs e debugging).
     *
     * @return String resumida do User
     */
    @Override
    public String toString() {
        return "User [id=" + id + ", name=" + name + ", email=" + email + ", role=" + role + "]";
    }
}
