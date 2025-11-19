package pt.up.edscrum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pt.up.edscrum.model.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

        @Query("SELECT COUNT(t) FROM Team t WHERE t.project.course.id = :courseId")
    long countByCourseId(Long courseId);

    // Conta equipas num dado curso
    long countByProjectCourseId(Long courseId);

    // Caso precises da lista
    List<Team> findByProjectCourseId(Long courseId);
}
