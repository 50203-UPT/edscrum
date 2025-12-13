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

    // Encontrar Score do Aluno
    Score findByUser(User user);

    // Encontrar Score da Equipa
    Score findByTeamAndUserIsNull(Team team);

    @Query("SELECT COALESCE(SUM(s.totalPoints), 0) FROM Score s WHERE s.user.id = :studentId")
    int sumPointsByStudent(Long studentId);

    @Query("SELECT new pt.up.edscrum.dto.dashboard.PointHistoryDTO(s.user.id, s.totalPoints) "
            + "FROM Score s WHERE s.user.id = :studentId ORDER BY s.id ASC")
    List<PointHistoryDTO> getPointHistory(Long studentId);

    // Ranking de Alunos
    @Query("SELECT new pt.up.edscrum.dto.dashboard.RankingDTO(s.user.id, s.user.name, SUM(s.totalPoints)) "
            + "FROM Score s WHERE s.user.id IS NOT NULL AND s.user.id IN :studentIds "
            + "GROUP BY s.user.id, s.user.name ORDER BY SUM(s.totalPoints) DESC")
    List<RankingDTO> getRankingForStudents(List<Long> studentIds);

    // Ranking de Equipas
    @Query("SELECT new pt.up.edscrum.dto.dashboard.RankingDTO(s.team.id, s.team.name, s.totalPoints) "
            + "FROM Score s WHERE s.team.id IS NOT NULL AND s.user.id IS NULL "
            + "AND s.team.project.course.id = :courseId "
            + "ORDER BY s.totalPoints DESC")
    List<RankingDTO> getTeamRanking(Long courseId);

    List<Score> findAllByUserIsNotNullOrderByTotalPointsDesc();
    
    // Encontrar Scores por Team ID (para eliminar antes de apagar equipa)
    List<Score> findByTeamId(Long teamId);
}
