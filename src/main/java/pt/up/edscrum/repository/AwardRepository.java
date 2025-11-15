package pt.up.edscrum.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.up.edscrum.model.Award;

public interface AwardRepository extends JpaRepository<Award, Long> {
}
