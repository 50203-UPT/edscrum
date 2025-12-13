package pt.up.edscrum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pt.up.edscrum.dto.dashboard.AwardStatsDTO;
import pt.up.edscrum.dto.dashboard.StudentDashboardDTO.AwardDisplayDTO;
import pt.up.edscrum.model.StudentAward; // Importar a classe interna

public interface StudentAwardRepository extends JpaRepository<StudentAward, Long> {

    // Método antigo (podes manter ou não, mas o dashboard professor usa este)
    @Query("SELECT new pt.up.edscrum.dto.dashboard.AwardStatsDTO(sa.award.name, COUNT(sa)) "
            + "FROM StudentAward sa WHERE sa.award.id = :courseId GROUP BY sa.award.name")
    List<AwardStatsDTO> countAwardsByCourse(Long courseId);

    // NOVO MÉTODO COMPLETO PARA O ESTUDANTE
    @Query("SELECT new pt.up.edscrum.dto.dashboard.StudentDashboardDTO$AwardDisplayDTO(sa.award.name, sa.pointsEarned, sa.award.description, sa.award.type) "
            + "FROM StudentAward sa WHERE sa.student.id = :studentId")
    List<AwardDisplayDTO> findFullAwardsForStudent(Long studentId);

    // ... outros métodos ...
    @Query("SELECT new pt.up.edscrum.dto.dashboard.AwardStatsDTO(sa.award.name, sa.pointsEarned) "
            + "FROM StudentAward sa WHERE sa.student.id = :studentId")
    List<AwardStatsDTO> findAwardsForStudent(Long studentId);

    List<StudentAward> findAllByStudentId(Long studentId);
    
    // Verificar se já existe prémio atribuído ao estudante no projeto
    boolean existsByStudentIdAndAwardIdAndProjectId(Long studentId, Long awardId, Long projectId);
    
    // Obter todos os prémios atribuídos ao estudante num projeto
    List<StudentAward> findByStudentIdAndProjectId(Long studentId, Long projectId);
}
