package pt.up.edscrum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pt.up.edscrum.model.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("SELECT COUNT(t) FROM Team t WHERE t.course.id = :courseId")
    long countByCourseId(Long courseId);

    // Encontra a equipa onde o user é Developer, SM ou PO
    @Query("SELECT t FROM Team t LEFT JOIN t.developers d WHERE d.id = :userId OR t.scrumMaster.id = :userId OR t.productOwner.id = :userId")
    Team findTeamByUserId(Long userId);
}