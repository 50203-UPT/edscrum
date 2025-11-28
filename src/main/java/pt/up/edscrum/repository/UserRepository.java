package pt.up.edscrum.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.up.edscrum.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);
}
