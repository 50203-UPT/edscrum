package pt.up.edscrum.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pt.up.edscrum.model.Team;

@Repository
/**
 * Repositório para operações sobre `Team` (equipas).
 */
public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("SELECT COUNT(t) FROM Team t WHERE t.course.id = :courseId")
    long countByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT t FROM Team t LEFT JOIN t.developers d WHERE d.id = :userId OR t.scrumMaster.id = :userId OR t.productOwner.id = :userId")
    /**
     * Encontra todas as equipas onde um utilizador participa (como SM, PO ou
     * Dev).
     *
     * @param userId id do utilizador
     * @return lista de `Team` onde o utilizador participa
     */
    List<Team> findTeamByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM Team t WHERE t.course.id = :courseId AND t.project IS NULL")
    /**
     * Equipas disponíveis (sem projeto) num curso.
     *
     * @param courseId id do curso
     * @return lista de equipas sem projeto
     */
    List<Team> findAvailableTeamsByCourse(@Param("courseId") Long courseId);

    @Query("SELECT COUNT(t) FROM Team t "
            + "LEFT JOIN t.developers d "
            + "WHERE t.course.id = :courseId "
            + "AND (d.id = :userId OR t.scrumMaster.id = :userId OR t.productOwner.id = :userId)")
    /**
     * Conta quantas equipas de um curso incluem um dado estudante.
     *
     * @param userId id do estudante
     * @param courseId id do curso
     * @return número de equipas que incluem o estudante no curso
     */
    long countStudentTeamsInCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    /**
     * Encontra equipes por id de curso.
     *
     * @param courseId id do curso
     * @return lista de equipas pertencentes ao curso
     */
    List<Team> findByCourseId(Long courseId);

    @Query("SELECT t FROM Team t "
            + "LEFT JOIN t.developers d "
            + "WHERE t.course.id = :courseId "
            + "AND (d.id = :userId OR t.scrumMaster.id = :userId OR t.productOwner.id = :userId)")
    /**
     * Encontra a equipa de um dado curso onde o utilizador participa (se
     * existir).
     *
     * @param courseId id do curso
     * @param userId id do utilizador
     * @return Optional contendo a `Team` se encontrada
     */
    Optional<Team> findTeamByCourseAndUser(@Param("courseId") Long courseId, @Param("userId") Long userId);
}
