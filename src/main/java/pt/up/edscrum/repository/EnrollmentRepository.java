package pt.up.edscrum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import pt.up.edscrum.model.Enrollment;
import pt.up.edscrum.model.User;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByCourseId(Long courseId);

    int countByCourseId(Long courseId);

    Enrollment findByStudent(User student);
}
