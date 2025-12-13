package pt.up.edscrum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pt.up.edscrum.dto.dashboard.PointHistoryDTO;
import pt.up.edscrum.dto.dashboard.RankingDTO;
import pt.up.edscrum.model.Score;
import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.User;

public interface ScoreRepository extends JpaRepository<Score, Long> {

    /**
     * Encontra o `Score` associado a um utilizador.
     *
     * @param user o utilizador
     * @return o `Score` do utilizador ou null
     */
    Score findByUser(User user);

    /**
     * Encontra o `Score` de uma equipa (user=null indica score agregado da
     * equipa).
     *
     * @param team a equipa
     * @return `Score` da equipa
     */
    Score findByTeamAndUserIsNull(Team team);

    @Query("SELECT COALESCE(SUM(s.totalPoints), 0) FROM Score s WHERE s.user.id = :studentId")
    /**
     * Soma dos pontos ao longo do tempo para um estudante.
     *
     * @param studentId id do estudante
     * @return soma de pontos
     */
    int sumPointsByStudent(Long studentId);

    @Query("SELECT new pt.up.edscrum.dto.dashboard.PointHistoryDTO(s.user.id, s.totalPoints) "
            + "FROM Score s WHERE s.user.id = :studentId ORDER BY s.id ASC")
    /**
     * Retorna o histórico de pontos de um estudante em DTOs.
     *
     * @param studentId id do estudante
     * @return lista de `PointHistoryDTO`
     */
    List<PointHistoryDTO> getPointHistory(Long studentId);

    @Query("SELECT new pt.up.edscrum.dto.dashboard.RankingDTO(s.user.id, s.user.name, SUM(s.totalPoints)) "
            + "FROM Score s WHERE s.user.id IS NOT NULL AND s.user.id IN :studentIds "
            + "GROUP BY s.user.id, s.user.name ORDER BY SUM(s.totalPoints) DESC")
    /**
     * Obtém o ranking de um conjunto de estudantes.
     *
     * @param studentIds lista de ids de estudantes
     * @return lista de `RankingDTO` ordenada por pontos decrescentes
     */
    List<RankingDTO> getRankingForStudents(List<Long> studentIds);

    @Query("SELECT new pt.up.edscrum.dto.dashboard.RankingDTO(s.team.id, s.team.name, s.totalPoints) "
            + "FROM Score s WHERE s.team.id IS NOT NULL AND s.user.id IS NULL "
            + "AND s.team.project.course.id = :courseId "
            + "ORDER BY s.totalPoints DESC")
    /**
     * Obtém o ranking das equipas para um curso.
     *
     * @param courseId id do curso
     * @return lista de `RankingDTO`
     */
    List<RankingDTO> getTeamRanking(Long courseId);

    /**
     * Lista todos os `Score` de utilizadores ordenados por pontos decrescentes.
     *
     * @return lista de `Score`
     */
    List<Score> findAllByUserIsNotNullOrderByTotalPointsDesc();
}
