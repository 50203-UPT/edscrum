package pt.up.edscrum.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import pt.up.edscrum.model.Course;
import pt.up.edscrum.repository.CourseRepository;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseRepository courseRepository;

    public CourseController(CourseRepository repo) {
        this.courseRepository = repo;
    }

    @GetMapping
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }
}
