package pt.up.edscrum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pt.up.edscrum.model.StudentAward;
import pt.up.edscrum.dto.dashboard.AwardStatsDTO;

public interface StudentAwardRepository extends JpaRepository<StudentAward, Long> {

    // Método para dashboard professor
    @Query("SELECT new pt.up.edscrum.dto.dashboard.AwardStatsDTO(sa.award.name, COUNT(sa)) " +
           "FROM StudentAward sa WHERE sa.award.id = :courseId GROUP BY sa.award.name")
    List<AwardStatsDTO> countAwardsByCourse(Long courseId);

    // Método para dashboard estudante
    @Query("SELECT new pt.up.edscrum.dto.dashboard.AwardStatsDTO(sa.award.name, sa.pointsEarned) " +
           "FROM StudentAward sa WHERE sa.student.id = :studentId")
    List<AwardStatsDTO> findAwardsForStudent(Long studentId);

    // NOVO: método que retorna StudentAward (necessário para calculateTotalPoints)
    List<StudentAward> findAllByStudentId(Long studentId);
}
