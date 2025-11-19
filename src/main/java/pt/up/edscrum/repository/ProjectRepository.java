package pt.up.edscrum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.up.edscrum.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByCourseId(Long courseId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.course.id = :courseId")
    long countByCourseId(Long courseId);
}
