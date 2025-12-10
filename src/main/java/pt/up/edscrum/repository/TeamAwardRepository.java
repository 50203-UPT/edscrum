package pt.up.edscrum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.up.edscrum.model.TeamAward;

public interface TeamAwardRepository extends JpaRepository<TeamAward, Long> {

    List<TeamAward> findByTeamId(Long teamId);
}
