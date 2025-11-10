package pt.up.edscrum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.up.edscrum.model.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {}
