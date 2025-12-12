package pt.up.edscrum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pt.up.edscrum.model.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("SELECT COUNT(t) FROM Team t WHERE t.course.id = :courseId")
    long countByCourseId(Long courseId);

    @Query("SELECT t FROM Team t LEFT JOIN t.developers d WHERE d.id = :userId OR t.scrumMaster.id = :userId OR t.productOwner.id = :userId")
    Team findTeamByUserId(Long userId);

    // NOVO: Equipas disponíveis (sem projeto) num curso
    @Query("SELECT t FROM Team t WHERE t.course.id = :courseId AND t.project IS NULL")
    List<Team> findAvailableTeamsByCourse(Long courseId);

    @Query("SELECT COUNT(t) FROM Team t " +
           "LEFT JOIN t.developers d " +
           "WHERE t.course.id = :courseId " +
           "AND (d.id = :userId OR t.scrumMaster.id = :userId OR t.productOwner.id = :userId)")
    long countStudentTeamsInCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    List<Team> findByCourseId(Long courseId);
}
