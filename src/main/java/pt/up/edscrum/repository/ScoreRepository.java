package pt.up.edscrum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.up.edscrum.model.Score;
import pt.up.edscrum.dto.dashboard.PointHistoryDTO;
import pt.up.edscrum.dto.dashboard.RankingDTO;

public interface ScoreRepository extends JpaRepository<Score, Long> {

    @Query("SELECT COALESCE(SUM(s.totalPoints), 0) FROM Score s WHERE s.user.id = :studentId")
    int sumPointsByStudent(Long studentId);

    @Query("SELECT new pt.up.edscrum.dto.dashboard.PointHistoryDTO(s.user.id, s.totalPoints) " +
           "FROM Score s WHERE s.user.id = :studentId ORDER BY s.id ASC")
    List<PointHistoryDTO> getPointHistory(Long studentId);

    @Query("SELECT new pt.up.edscrum.dto.dashboard.RankingDTO(s.user.id, s.user.name, SUM(s.totalPoints)) " +
           "FROM Score s WHERE s.user.id = :courseId GROUP BY s.user.id, s.user.name ORDER BY SUM(s.totalPoints) DESC")
    List<RankingDTO> getRankingForCourse(Long courseId);

    @Query("SELECT new pt.up.edscrum.dto.dashboard.RankingDTO(t.id, t.name, SUM(s.totalPoints)) " +
           "FROM Score s JOIN s.team t WHERE t.project.course.id = :courseId " +
           "GROUP BY t.id, t.name ORDER BY SUM(s.totalPoints) DESC")
    List<RankingDTO> getTeamRanking(Long courseId);
}
