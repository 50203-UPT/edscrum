package pt.up.edscrum.repository;

import java.util.List; // Importar List

import org.springframework.data.jpa.repository.JpaRepository;

import pt.up.edscrum.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);

    // Buscar utilizadores por papel (para listar apenas alunos nos selects)
    List<User> findByRole(String role);
}
