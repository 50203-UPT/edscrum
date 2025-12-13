package pt.up.edscrum.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.up.edscrum.model.Award;

public interface AwardRepository extends JpaRepository<Award, Long> {

    Optional<Award> findByName(String name);
}
