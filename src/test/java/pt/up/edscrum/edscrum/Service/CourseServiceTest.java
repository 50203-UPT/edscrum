package pt.up.edscrum.edscrum.Service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.enums.ProjectStatus; // Importante para criar Projetos válidos
import pt.up.edscrum.model.*; // Importa User, Project, Team, Sprint, Enrollment, Course
import pt.up.edscrum.repository.*;
import pt.up.edscrum.service.CourseService;

@SpringBootTest
@Transactional
class CourseServiceTest {

    @Autowired private CourseService courseService;
    @Autowired private EntityManager entityManager;

    // Repositórios necessários APENAS para limpar a BD no setUp
    @Autowired private CourseRepository courseRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private AwardRepository awardRepo;
    @Autowired private SprintRepository sprintRepo;
    @Autowired private UserStoryRepository userStoryRepo;

    @BeforeEach
    void setUp() {
        // 1. Limpeza profunda por ordem de dependência (Filhos -> Pais)
        notificationRepo.deleteAll();
        userStoryRepo.deleteAll();
        sprintRepo.deleteAll();
        teamAwardRepo.deleteAll();
        studentAwardRepo.deleteAll();
        scoreRepo.deleteAll();
        teamRepo.deleteAll();
        projectRepo.deleteAll();
        enrollmentRepo.deleteAll();
        awardRepo.deleteAll();
        
        // Agora sim, podemos limpar os cursos e users
        courseRepo.deleteAll();
        userRepo.deleteAll();

        // Limpar a cache do Hibernate para garantir testes reais
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void testCreateAndGetCourse() {
        Course c = new Course();
        c.setName("Engenharia de Software");
        c.setDescription("Curso de testes com Spring Boot");
        
        // Criação
        Course saved = courseService.createCourse(c);

        assertNotNull(saved.getId());
        assertEquals("Engenharia de Software", saved.getName());

        // Forçar ida à base de dados (limpar cache)
        entityManager.flush();
        entityManager.clear();

        // Recuperação
        Course found = courseService.getCourseById(saved.getId());
        assertEquals(saved.getName(), found.getName());
        assertEquals(saved.getDescription(), found.getDescription());
    }

    @Test
    void testGetAllCourses() {
        Course c1 = new Course();
        c1.setName("Curso 1");
        courseService.createCourse(c1);

        Course c2 = new Course();
        c2.setName("Curso 2");
        courseService.createCourse(c2);

        entityManager.flush();
        entityManager.clear();

        List<Course> courses = courseService.getAllCourses();
        
        // Como limpamos a BD no setUp, deve haver EXATAMENTE 2 cursos
        assertEquals(2, courses.size());
    }

    @Test
    void testUpdateCourse() {
        Course c = new Course();
        c.setName("Antigo");
        c.setDescription("Descrição antiga");
        Course saved = courseService.createCourse(c);

        Course update = new Course();
        update.setName("Novo Nome");
        update.setDescription("Nova descrição");

        // Ação
        Course updated = courseService.updateCourse(saved.getId(), update);
        
        // Verificação do retorno
        assertEquals("Novo Nome", updated.getName());
        assertEquals("Nova descrição", updated.getDescription());

        // Verificação da persistência (fetch fresco)
        entityManager.flush();
        entityManager.clear();
        
        Course fromDb = courseService.getCourseById(saved.getId());
        assertEquals("Novo Nome", fromDb.getName());
    }

    @Test
    void testDeleteCourse() {
        Course c = new Course();
        c.setName("Curso para apagar");
        Course saved = courseService.createCourse(c);
        
        // Variável final para usar no lambda
        final Long courseId = saved.getId();

        // Ação
        courseService.deleteCourse(courseId);
        
        entityManager.flush();
        entityManager.clear();

        // Verificação: Tentar obter o curso deve lançar exceção
        assertThrows(RuntimeException.class, () -> {
            courseService.getCourseById(courseId);
        });
        
        // Verificação alternativa direta no repositório
        assertFalse(courseRepo.existsById(courseId));
    }

    @Test
    void testEnrollStudent() {
        // 1. Preparar Dados
        Course c = new Course();
        c.setName("Curso de Inscrição");
        Course savedCourse = courseService.createCourse(c);

        User student = new User();
        student.setName("Aluno Novo");
        student.setEmail("aluno@upt.pt");
        student.setPassword("123");
        student.setRole("STUDENT");
        User savedStudent = userRepo.save(student);

        // 2. Ação
        courseService.enrollStudent(savedCourse.getId(), savedStudent.getId());

        // 3. Verificação
        assertTrue(enrollmentRepo.existsByStudentIdAndCourseId(savedStudent.getId(), savedCourse.getId()));
    }

    @Test
    void testEnrollStudent_Fail_StudentNotFound() {
        Course c = new Course();
        c.setName("Curso Erro");
        Course savedCourse = courseService.createCourse(c);

        // Ação com ID de estudante inexistente (999L)
        // Usamos savedCourse.getId() que é efetivamente final ou já guardado
        final Long cId = savedCourse.getId();
        
        Exception e = assertThrows(RuntimeException.class, () -> {
            courseService.enrollStudent(cId, 999L);
        });

        assertEquals("Estudante não encontrado", e.getMessage());
    }

    @Test
    void testGetEnrolledStudentsByCourse() {
        // 1. Setup
        Course c = new Course();
        c.setName("Matemática");
        Course savedCourse = courseService.createCourse(c);

        User s1 = createTestUser("S1", "s1@t.com", "STUDENT");
        User s2 = createTestUser("S2", "s2@t.com", "STUDENT");
        User t1 = createTestUser("Teacher", "teach@t.com", "TEACHER"); // Não deve aparecer na lista

        // Inscrever manualmente via repositório auxiliar
        enrollUser(s1, savedCourse);
        enrollUser(s2, savedCourse);
        enrollUser(t1, savedCourse); // Inscrever professor (cenário atípico, mas serve para testar o filtro)

        entityManager.flush();
        entityManager.clear();

        // 2. Ação
        List<User> students = courseService.getEnrolledStudentsByCourse(savedCourse.getId());

        // 3. Verificação
        assertEquals(2, students.size(), "Deve retornar apenas os 2 estudantes, ignorando o professor");
        assertTrue(students.stream().anyMatch(u -> u.getEmail().equals("s1@t.com")));
        assertTrue(students.stream().anyMatch(u -> u.getEmail().equals("s2@t.com")));
    }

    @Test
    void testGetCoursesByTeacher_WithDeepStructure() {
        // 1. Setup Complexo (Professor -> Curso -> Projeto -> Sprints/Equipas)
        User teacher = createTestUser("Prof. Java", "prof@upt.pt", "TEACHER");
        
        Course c = new Course();
        c.setName("Java Avançado");
        c.setTeacher(teacher); 
        c = courseRepo.save(c);

        Project p = new Project();
        p.setName("Projeto Final");
        p.setCourse(c);
        p.setStatus(ProjectStatus.EM_CURSO); // Definir status para garantir validade
        p = projectRepo.save(p);

        Sprint s = new Sprint();
        s.setProject(p);
        s.setName("Sprint 1");
        sprintRepo.save(s);

        Team t = new Team();
        t.setProject(p);
        t.setCourse(c);
        t.setName("Equipa A");
        teamRepo.save(t);

        // Limpar cache para obrigar o Service a fazer fetch da base de dados
        entityManager.flush();
        entityManager.clear();

        // 2. Ação
        List<Course> teacherCourses = courseService.getCoursesByTeacher(teacher.getId());

        // 3. Verificações
        assertNotNull(teacherCourses);
        assertEquals(1, teacherCourses.size());
        Course fetchedCourse = teacherCourses.get(0);
        
        assertEquals("Java Avançado", fetchedCourse.getName());
        
        // Verificar se os dados aninhados foram carregados (o método do service percorre .size())
        assertNotNull(fetchedCourse.getProjects());
        assertEquals(1, fetchedCourse.getProjects().size());
        
        Project fetchedProject = fetchedCourse.getProjects().get(0);
        assertNotNull(fetchedProject.getSprints());
        assertEquals(1, fetchedProject.getSprints().size());
        
        assertNotNull(fetchedProject.getTeams());
        assertEquals(1, fetchedProject.getTeams().size());
    }

    // --- Helpers locais para este teste ---

    private User createTestUser(String name, String email, String role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword("pass");
        u.setRole(role);
        return userRepo.save(u);
    }

    private void enrollUser(User u, Course c) {
        Enrollment e = new Enrollment();
        e.setStudent(u);
        e.setCourse(c);
        enrollmentRepo.save(e);
    }
}