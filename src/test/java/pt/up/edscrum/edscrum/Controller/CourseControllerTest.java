package pt.up.edscrum.edscrum.Controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.controller.CourseController;
import pt.up.edscrum.model.Course;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.*;

/**
 * Testes de integração para o CourseController.
 * <p>
 * Verifica a funcionalidade de listagem de cursos exposta pela API,
 * garantindo que a comunicação com a camada de dados funciona corretamente.
 * </p>
 */
@SpringBootTest
@Transactional
class CourseControllerTest {

    @Autowired
    private CourseController courseController;

    @Autowired private CourseRepository courseRepository;
    @Autowired private UserRepository userRepository;

    // Repositórios adicionais necessários para limpar dependências (Foreign Keys)
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private UserStoryRepository userStoryRepo;
    @Autowired private SprintRepository sprintRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private AwardRepository awardRepo;

    /**
     * Configuração inicial: Limpa a base de dados antes de cada teste
     * para garantir um estado limpo e determinístico.
     * <p>
     * A ordem de eliminação é crítica para evitar DataIntegrityViolationException.
     * Devemos apagar primeiro as tabelas dependentes (filhas) e só depois as pais.
     * </p>
     */
    @BeforeEach
    void setUp() {
        // 1. Limpar tabelas dependentes (Folhas)
        notificationRepo.deleteAll();
        teamAwardRepo.deleteAll();
        studentAwardRepo.deleteAll();
        userStoryRepo.deleteAll();
        scoreRepo.deleteAll();
        enrollmentRepo.deleteAll();
        
        // 2. Limpar tabelas intermédias
        sprintRepo.deleteAll();
        teamRepo.deleteAll();
        awardRepo.deleteAll();
        
        // 3. Limpar tabelas principais (Pais do erro original)
        projectRepo.deleteAll(); // Elimina projetos para libertar os cursos
        
        // 4. Limpar tabelas raiz
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Testa o método {@code getAllCourses} quando a base de dados está vazia.
     * <p>
     * Resultado esperado: Uma lista vazia (não nula).
     * </p>
     */
    @Test
    void testGetAllCourses_Empty() {
        // Ação
        List<Course> courses = courseController.getAllCourses();

        // Verificação
        assertNotNull(courses, "A lista de cursos não deve ser nula");
        assertTrue(courses.isEmpty(), "A lista de cursos deve estar vazia");
    }

    /**
     * Testa o método {@code getAllCourses} com dados persistidos.
     * <p>
     * Cenário:
     * 1. Cria um Professor (necessário para a FK do curso).
     * 2. Cria e persiste dois Cursos.
     * 3. Chama o controlador.
     * Resultado esperado: Uma lista contendo exatamente os cursos criados.
     * </p>
     */
    @Test
    void testGetAllCourses_WithData() {
        // 1. Preparar Dados (Professor)
        User teacher = new User();
        teacher.setName("Prof Test");
        teacher.setEmail("prof@test.com");
        teacher.setPassword("123");
        teacher.setRole("TEACHER");
        userRepository.save(teacher);

        // 2. Preparar Dados (Cursos)
        Course c1 = new Course();
        c1.setName("Engenharia de Software");
        c1.setTeacher(teacher);
        courseRepository.save(c1);

        Course c2 = new Course();
        c2.setName("Inteligência Artificial");
        c2.setTeacher(teacher);
        courseRepository.save(c2);

        // 3. Ação
        List<Course> courses = courseController.getAllCourses();

        // 4. Verificação
        assertNotNull(courses);
        assertEquals(2, courses.size(), "Devem existir 2 cursos na base de dados");
        
        // Verificar conteúdo
        boolean containsES = courses.stream().anyMatch(c -> c.getName().equals("Engenharia de Software"));
        boolean containsIA = courses.stream().anyMatch(c -> c.getName().equals("Inteligência Artificial"));
        
        assertTrue(containsES, "A lista deve conter 'Engenharia de Software'");
        assertTrue(containsIA, "A lista deve conter 'Inteligência Artificial'");
    }
}