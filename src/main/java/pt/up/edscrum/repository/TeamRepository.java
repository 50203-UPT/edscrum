package pt.up.edscrum.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.up.edscrum.model.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {
}
