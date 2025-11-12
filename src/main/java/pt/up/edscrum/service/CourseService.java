package pt.up.edscrum.service;

import org.springframework.stereotype.Service;
import java.util.List;
import pt.up.edscrum.model.Course;
import pt.up.edscrum.repository.CourseRepository;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    // Construtor manual
    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    // Listar todos os cursos
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    // Buscar curso por ID
    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Curso não encontrado"));
    }

    // Criar novo curso
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    // Atualizar curso
    public Course updateCourse(Long id, Course courseDetails) {
        Course course = getCourseById(id);
        course.setName(courseDetails.getName());
        course.setDescription(courseDetails.getDescription());
        return courseRepository.save(course);
    }

    // Apagar curso
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }
}
