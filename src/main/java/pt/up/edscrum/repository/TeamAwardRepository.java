package pt.up.edscrum.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.up.edscrum.model.TeamAward;

public interface TeamAwardRepository extends JpaRepository<TeamAward, Long> {

    List<TeamAward> findByTeamId(Long teamId);
    
    // Verificar se já existe prémio atribuído à equipa no projeto
    Optional<TeamAward> findByTeamIdAndAwardIdAndProjectId(Long teamId, Long awardId, Long projectId);
    
    boolean existsByTeamIdAndAwardIdAndProjectId(Long teamId, Long awardId, Long projectId);
    
    // Obter todos os prémios atribuídos à equipa num projeto
    List<TeamAward> findByTeamIdAndProjectId(Long teamId, Long projectId);
}
