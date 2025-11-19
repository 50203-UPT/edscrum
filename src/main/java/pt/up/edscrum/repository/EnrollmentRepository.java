package pt.up.edscrum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import pt.up.edscrum.model.Enrollment;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByCourseId(Long courseId);

    int countByCourseId(Long courseId);
}
