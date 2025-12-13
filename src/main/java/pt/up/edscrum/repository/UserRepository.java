package pt.up.edscrum.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pt.up.edscrum.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Procura um utilizador por email.
     *
     * @param email Email a procurar
     * @return Optional contendo o User se existir
     */
    Optional<User> findByEmail(String email);

    /**
     * Busca utilizadores por papel (ex.: "STUDENT" ou "TEACHER").
     *
     * @param role Nome do papel
     * @return Lista de User com o papel solicitado
     */
    List<User> findByRole(String role);

    /**
     * Procura um utilizador pelo código de reset (útil no fluxo de forgot
     * password).
     *
     * @param resetCode Código de reset
     * @return Optional contendo o User se o código existir
     */
    Optional<User> findByResetCode(String resetCode);
}
