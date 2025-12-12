package pt.up.edscrum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // Adicionei a anotação para boas práticas

import pt.up.edscrum.model.Enrollment;
import pt.up.edscrum.model.User;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByCourseId(Long courseId);

    int countByCourseId(Long courseId);

    // Método antigo (podes manter para compatibilidade)
    Enrollment findByStudent(User student);

    // Retorna LISTA de inscrições do aluno
    List<Enrollment> findAllByStudent(User student);

    // --- NOVO: Verifica se já existe uma inscrição deste aluno neste curso ---
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
}