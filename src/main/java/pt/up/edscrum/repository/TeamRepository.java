package pt.up.edscrum.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pt.up.edscrum.model.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("SELECT COUNT(t) FROM Team t WHERE t.course.id = :courseId")
    long countByCourseId(@Param("courseId") Long courseId);

    // ALTERAÇÃO DE SEGURANÇA: Mudei o retorno para List<Team>.
    // Como o aluno pode estar em várias equipas (em cursos diferentes), retornar apenas "Team" causava o erro.
    @Query("SELECT t FROM Team t LEFT JOIN t.developers d WHERE d.id = :userId OR t.scrumMaster.id = :userId OR t.productOwner.id = :userId")
    List<Team> findTeamByUserId(@Param("userId") Long userId);

    // Equipas disponíveis (sem projeto) num curso
    @Query("SELECT t FROM Team t WHERE t.course.id = :courseId AND t.project IS NULL")
    List<Team> findAvailableTeamsByCourse(@Param("courseId") Long courseId);

    @Query("SELECT COUNT(t) FROM Team t " +
           "LEFT JOIN t.developers d " +
           "WHERE t.course.id = :courseId " +
           "AND (d.id = :userId OR t.scrumMaster.id = :userId OR t.productOwner.id = :userId)")
    long countStudentTeamsInCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    List<Team> findByCourseId(Long courseId);

    // ESTE É O MÉTODO CRÍTICO QUE O NOVO DASHBOARD USA
    @Query("SELECT t FROM Team t " +
           "LEFT JOIN t.developers d " +
           "WHERE t.course.id = :courseId " +
           "AND (d.id = :userId OR t.scrumMaster.id = :userId OR t.productOwner.id = :userId)")
    Optional<Team> findTeamByCourseAndUser(@Param("courseId") Long courseId, @Param("userId") Long userId);
}