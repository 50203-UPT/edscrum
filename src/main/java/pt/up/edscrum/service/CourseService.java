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
/**
 * Serviço responsável pela gestão de cursos e ações relacionadas, incluindo
 * CRUD de cursos, inscrição de estudantes e obtenção de alunos inscritos por
 * curso.
 */
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    /**
     * Construtor do serviço de cursos.
     *
     * @param courseRepository repositório de cursos
     * @param enrollmentRepository repositório de inscrições
     * @param userRepository repositório de utilizadores
     */
    public CourseService(CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    /**
     * Retorna a lista de todos os cursos.
     *
     * @return lista de `Course`
     */
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    /**
     * Obtém um curso pelo seu id.
     *
     * @param id id do curso
     * @return `Course` correspondente
     */
    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Curso não encontrado"));
    }

    /**
     * Obtém a lista de cursos lecionados por um professor, inicializando
     * colecções necessárias para evitar problemas de lazy loading.
     *
     * @param teacherId id do professor
     * @return lista de `Course`
     */
    public List<Course> getCoursesByTeacher(Long teacherId) {
        List<Course> courses = courseRepository.findByTeacherIdWithProjects(teacherId);
        courses.forEach(course -> {
            if (course.getProjects() != null) {
                course.getProjects().forEach(project -> {
                    if (project.getSprints() != null) {
                        project.getSprints().size();
                    }
                    if (project.getTeams() != null) {
                        project.getTeams().size();
                    }
                });
            }
        });
        return courses;
    }

    /**
     * Cria um novo curso.
     *
     * @param course o objeto `Course` a criar
     * @return o curso persistido
     */
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    /**
     * Atualiza os dados de um curso.
     *
     * @param id id do curso a atualizar
     * @param courseDetails dados de atualização
     * @return curso atualizado
     */
    public Course updateCourse(Long id, Course courseDetails) {
        Course course = getCourseById(id);
        course.setName(courseDetails.getName());
        course.setDescription(courseDetails.getDescription());
        course.setCode(courseDetails.getCode());
        course.setSemester(courseDetails.getSemester());
        course.setYear(courseDetails.getYear());
        return courseRepository.save(course);
    }

    /**
     * Elimina um curso por id.
     *
     * @param id id do curso a eliminar
     */
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    /**
     * Inscreve um estudante num curso.
     *
     * @param courseId id do curso
     * @param studentId id do estudante
     */
    public void enrollStudent(Long courseId, Long studentId) {
        Course course = getCourseById(courseId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Estudante não encontrado"));
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setStudent(student);

        enrollmentRepository.save(enrollment);
    }

    /**
     * Obtém a lista de estudantes inscritos num curso.
     *
     * @param courseId id do curso
     * @return lista de `User` com role STUDENT
     */
    public List<User> getEnrolledStudentsByCourse(Long courseId) {
        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
        return enrollments.stream()
                .map(Enrollment::getStudent)
                .filter(user -> "STUDENT".equals(user.getRole()))
                .collect(java.util.stream.Collectors.toList());
    }
}
