package pt.up.edscrum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pt.up.edscrum.model.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("SELECT COUNT(t) FROM Team t WHERE t.course.id = :courseId")
    long countByCourseId(Long courseId);

    @Query("SELECT t FROM Team t LEFT JOIN t.developers d WHERE d.id = :userId OR t.scrumMaster.id = :userId OR t.productOwner.id = :userId")
    Team findTeamByUserId(Long userId);

    @Query("SELECT t FROM Team t LEFT JOIN t.developers d WHERE (d.id = :userId OR t.scrumMaster.id = :userId OR t.productOwner.id = :userId) AND t.course.id = :courseId")
    Team findTeamByUserIdAndCourseId(Long userId, Long courseId);

    // Equipas disponíveis (sem projeto) num curso - teams sem projects
    @Query("SELECT t FROM Team t WHERE t.course.id = :courseId AND t.projects IS EMPTY")
    List<Team> findAvailableTeamsByCourse(Long courseId);

    // Equipas disponíveis para um projeto: teams do curso que ainda não estão no projeto
    @Query("SELECT t FROM Team t WHERE t.course.id = :courseId AND :projectId NOT IN (SELECT p.id FROM t.projects p)")
    List<Team> findAvailableTeamsForProject(Long courseId, Long projectId);
}
