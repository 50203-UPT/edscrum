package pt.up.edscrum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pt.up.edscrum.model.Course;

/**
 * Repositório para operações de leitura/escrita sobre `Course`.
 */
public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * Encontra cursos lecionados por um professor.
     *
     * @param teacherId id do professor
     * @return lista de `Course`
     */
    List<Course> findByTeacherId(Long teacherId);

    @Query("SELECT DISTINCT c FROM Course c "
            + "LEFT JOIN FETCH c.projects "
            + "WHERE c.teacher.id = :teacherId")
    /**
     * Encontra cursos do professor e pré-carrega os projetos para evitar lazy
     * loading.
     *
     * @param teacherId id do professor
     * @return lista de `Course` com projetos inicializados
     */
    List<Course> findByTeacherIdWithProjects(@Param("teacherId") Long teacherId);
}
