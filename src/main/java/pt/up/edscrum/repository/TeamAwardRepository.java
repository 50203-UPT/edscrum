package pt.up.edscrum.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.up.edscrum.model.TeamAward;

/**
 * Repositório para atribuições de prémios a equipas (`TeamAward`).
 */
public interface TeamAwardRepository extends JpaRepository<TeamAward, Long> {

    /**
     * Encontra todos os TeamAwards relacionados com uma equipa.
     *
     * @param teamId id da equipa
     * @return lista de `TeamAward`
     */
    List<TeamAward> findByTeamId(Long teamId);

    /**
     * Procura um `TeamAward` por equipa, prémio e projeto.
     *
     * @param teamId id da equipa
     * @param awardId id do prémio
     * @param projectId id do projeto
     * @return Optional com o TeamAward se existir
     */
    Optional<TeamAward> findByTeamIdAndAwardIdAndProjectId(Long teamId, Long awardId, Long projectId);

    /**
     * Verifica se já existe uma atribuição de prémio para a equipa no projeto.
     *
     * @param teamId id da equipa
     * @param awardId id do prémio
     * @param projectId id do projeto
     * @return true se existir, false caso contrário
     */
    boolean existsByTeamIdAndAwardIdAndProjectId(Long teamId, Long awardId, Long projectId);

    /**
     * Lista todas as atribuições de prémios de uma equipa num projeto.
     *
     * @param teamId id da equipa
     * @param projectId id do projeto
     * @return lista de `TeamAward`
     */
    List<TeamAward> findByTeamIdAndProjectId(Long teamId, Long projectId);
}
