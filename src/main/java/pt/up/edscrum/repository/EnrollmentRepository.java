package pt.up.edscrum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pt.up.edscrum.model.Enrollment;
import pt.up.edscrum.model.User;

@Repository
/**
 * Repositório para operações sobre inscrições (enrollments) de estudantes em
 * cursos.
 */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * Encontra inscrições por id de curso.
     *
     * @param courseId id do curso
     * @return lista de `Enrollment`
     */
    List<Enrollment> findByCourseId(Long courseId);

    /**
     * Conta inscrições de um curso.
     *
     * @param courseId id do curso
     * @return número de inscrições
     */
    int countByCourseId(Long courseId);

    /**
     * Encontra uma inscrição por estudante.
     *
     * @param student objeto `User`
     * @return `Enrollment` se existente
     */
    Enrollment findByStudent(User student);

    /**
     * Lista todas as inscrições de um estudante.
     *
     * @param student objeto `User`
     * @return lista de `Enrollment`
     */
    List<Enrollment> findAllByStudent(User student);

    /**
     * Verifica se um estudante já está inscrito num curso.
     *
     * @param studentId id do estudante
     * @param courseId id do curso
     * @return true se existir inscrição, false caso contrário
     */
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
}
