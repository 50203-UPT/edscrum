package pt.up.edscrum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pt.up.edscrum.dto.dashboard.AwardStatsDTO;
import pt.up.edscrum.dto.dashboard.StudentDashboardDTO.AwardDisplayDTO;
import pt.up.edscrum.model.StudentAward;

public interface StudentAwardRepository extends JpaRepository<StudentAward, Long> {

    @Query("SELECT new pt.up.edscrum.dto.dashboard.AwardStatsDTO(sa.award.name, COUNT(sa)) "
            + "FROM StudentAward sa WHERE sa.award.id = :courseId GROUP BY sa.award.name")
    /**
     * Conta os prémios por nome num dado contexto de curso (usado pelo
     * dashboard do professor).
     *
     * @param courseId id do curso
     * @return lista de `AwardStatsDTO` com o nome do prémio e a contagem
     */
    List<AwardStatsDTO> countAwardsByCourse(Long courseId);

    @Query("SELECT new pt.up.edscrum.dto.dashboard.StudentDashboardDTO$AwardDisplayDTO(sa.award.name, sa.pointsEarned, sa.award.description, sa.award.type) "
            + "FROM StudentAward sa WHERE sa.student.id = :studentId")
    /**
     * Retorna DTOs detalhados dos prémios ganhos por um estudante.
     *
     * @param studentId id do estudante
     * @return lista de `AwardDisplayDTO`
     */
    List<AwardDisplayDTO> findFullAwardsForStudent(Long studentId);

    @Query("SELECT new pt.up.edscrum.dto.dashboard.AwardStatsDTO(sa.award.name, sa.pointsEarned) "
            + "FROM StudentAward sa WHERE sa.student.id = :studentId")
    /**
     * Retorna estatísticas simples dos prémios de um estudante.
     *
     * @param studentId id do estudante
     * @return lista de `AwardStatsDTO`
     */
    List<AwardStatsDTO> findAwardsForStudent(Long studentId);

    /**
     * Retorna todas as atribuições de prémios de um estudante.
     *
     * @param studentId id do estudante
     * @return lista de `StudentAward`
     */
    List<StudentAward> findAllByStudentId(Long studentId);

    /**
     * Verifica se um estudante já recebeu um prémio específico num projeto.
     *
     * @param studentId id do estudante
     * @param awardId id do prémio
     * @param projectId id do projeto
     * @return true se existir, false caso contrário
     */
    boolean existsByStudentIdAndAwardIdAndProjectId(Long studentId, Long awardId, Long projectId);

    /**
     * Lista as atribuições de prémios de um estudante num projeto.
     *
     * @param studentId id do estudante
     * @param projectId id do projeto
     * @return lista de `StudentAward`
     */
    List<StudentAward> findByStudentIdAndProjectId(Long studentId, Long projectId);
}
