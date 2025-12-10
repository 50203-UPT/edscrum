package pt.up.edscrum.repository;

import java.util.List; // Importar List
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pt.up.edscrum.model.User;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // Buscar utilizadores por papel (para listar apenas alunos nos selects)
    List<User> findByRole(String role);
    Optional<User> findByResetCode(String resetCode); // Opcional, útil para validações
}

