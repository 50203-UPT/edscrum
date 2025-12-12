package pt.up.edscrum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pt.up.edscrum.model.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByTeacherId(Long teacherId);
    
    @Query("SELECT DISTINCT c FROM Course c " +
           "LEFT JOIN FETCH c.projects " +
           "WHERE c.teacher.id = :teacherId")
    List<Course> findByTeacherIdWithProjects(@Param("teacherId") Long teacherId);
}
