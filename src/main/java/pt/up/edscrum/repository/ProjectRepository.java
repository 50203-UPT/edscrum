package pt.up.edscrum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pt.up.edscrum.model.Project;

/**
 * Repositório para operações sobre `Project`.
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Encontra projetos pertencentes a um dado curso.
     *
     * @param courseId id do curso
     * @return lista de `Project`
     */
    List<Project> findByCourseId(Long courseId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.course.id = :courseId")
    /**
     * Conta o número de projetos de um curso.
     *
     * @param courseId id do curso
     * @return número de projetos
     */
    long countByCourseId(Long courseId);
}
