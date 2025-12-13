package pt.up.edscrum.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pt.up.edscrum.model.Course;
import pt.up.edscrum.model.Enrollment;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.CourseRepository;
import pt.up.edscrum.repository.EnrollmentRepository;
import pt.up.edscrum.repository.UserRepository;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository; // Novo
    private final UserRepository userRepository;             // Novo

    // Construtor atualizado com as novas dependências
    public CourseService(CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    // --- Métodos Existentes ---
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Curso não encontrado"));
    }

    public List<Course> getCoursesByTeacher(Long teacherId) {
        List<Course> courses = courseRepository.findByTeacherIdWithProjects(teacherId);
        // Initialize collections to avoid lazy loading issues
        courses.forEach(course -> {
            if (course.getProjects() != null) {
                course.getProjects().forEach(project -> {
                    if (project.getSprints() != null) {
                        project.getSprints().size(); // Force initialization
                    }
                    if (project.getTeams() != null) {
                        project.getTeams().size(); // Force initialization
                    }
                });
            }
        });
        return courses;
    }

    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    public Course updateCourse(Long id, Course courseDetails) {
        Course course = getCourseById(id);
        course.setName(courseDetails.getName());
        course.setDescription(courseDetails.getDescription());
        course.setCode(courseDetails.getCode());
        course.setSemester(courseDetails.getSemester());
        course.setYear(courseDetails.getYear());
        return courseRepository.save(course);
    }

    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    // --- NOVO: Método de Inscrição ---
    public void enrollStudent(Long courseId, Long studentId) {
        Course course = getCourseById(courseId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Estudante não encontrado"));
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setStudent(student);

        enrollmentRepository.save(enrollment);
    }

    // --- NOVO: Obter lista de alunos inscritos num curso ---
    public List<User> getEnrolledStudentsByCourse(Long courseId) {
        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
        return enrollments.stream()
                .map(Enrollment::getStudent)
                .filter(user -> "STUDENT".equals(user.getRole()))
                .collect(java.util.stream.Collectors.toList());
    }
}
