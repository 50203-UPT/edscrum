package pt.up.edscrum.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.up.edscrum.model.Course;
import pt.up.edscrum.repository.CourseRepository;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseRepository courseRepository;

    public CourseController(CourseRepository repo) {
        this.courseRepository = repo;
    }

    /**
     * Obtém todos os cursos.
     *
     * @return Lista de Course
     */
    @GetMapping
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }
}
