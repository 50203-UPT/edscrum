package pt.up.edscrum.edscrum.Controller;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import pt.up.edscrum.controller.ExportController;
import pt.up.edscrum.enums.ProjectStatus;
import pt.up.edscrum.model.*;
import pt.up.edscrum.repository.*;

@SpringBootTest
@Transactional
class ExportControllerTest {

    @Autowired private ExportController exportController;
    @Autowired private EntityManager entityManager;

    @Autowired private UserRepository userRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private AwardRepository awardRepo;
    
    // Repositórios extra para limpeza
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private SprintRepository sprintRepo;
    @Autowired private UserStoryRepository userStoryRepo;

    private User teacher;
    private User student;
    private Course course;
    private Project project;
    private Team team;
    
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        // 1. Limpeza profunda
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
        courseRepo.deleteAll();
        userRepo.deleteAll();

        // 2. Setup Base
        teacher = new User();
        teacher.setName("Prof Export");
        teacher.setEmail("pe@upt.pt");
        teacher.setPassword("123");
        teacher.setRole("TEACHER");
        teacher = userRepo.save(teacher);

        student = new User();
        student.setName("Aluno Export");
        student.setEmail("ae@upt.pt");
        student.setPassword("123");
        student.setRole("STUDENT");
        student = userRepo.save(student);

        course = new Course();
        course.setName("Course CSV");
        course.setTeacher(teacher);
        course = courseRepo.save(course);
        
        Enrollment e = new Enrollment();
        e.setStudent(student);
        e.setCourse(course);
        enrollmentRepo.save(e);

        project = new Project();
        project.setName("Project CSV");
        project.setCourse(course);
        project.setStatus(ProjectStatus.EM_CURSO);
        project = projectRepo.save(project);

        // 3. CRIAÇÃO ROBUSTA DA EQUIPA
        team = new Team();
        team.setName("Team CSV");
        team.setCourse(course);
        team.setProject(project);
        team.setScrumMaster(student);
        
        // Inicializar a lista de developers explicitamente com ArrayList
        List<User> devs = new ArrayList<>();
        devs.add(student);
        team.setDevelopers(devs);
        
        team = teamRepo.save(team);
        
        // 4. Criação do Score
        Score score = new Score();
        score.setUser(student);
        score.setTeam(team); 
        score.setTotalPoints(150);
        scoreRepo.save(score);

        session = new MockHttpSession();
        
        // 5. Garantir Persistência
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void testExportCsv_NoSession() {
        ResponseEntity<byte[]> response = exportController.exportRankingsCsv(teacher.getId(), session);
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void testExportCsv_WrongUser() {
        session.setAttribute("currentUserId", student.getId());
        session.setAttribute("currentUserRole", "STUDENT");
        
        ResponseEntity<byte[]> response = exportController.exportRankingsCsv(teacher.getId(), session);
        assertEquals(403, response.getStatusCode().value());
    }
    
    @Test
    void testExportCsv_WrongRole_ButCorrectId() {
        session.setAttribute("currentUserId", teacher.getId());
        session.setAttribute("currentUserRole", "STUDENT");
        
        ResponseEntity<byte[]> response = exportController.exportRankingsCsv(teacher.getId(), session);
        assertEquals(403, response.getStatusCode().value());
    }

    //@Test
   // void testExportCsv_Success_FullData() {
        //session.setAttribute("currentUserId", teacher.getId());
       // session.setAttribute("currentUserRole", "TEACHER");

        // Executar exportação
       // ResponseEntity<byte[]> response = exportController.exportRankingsCsv(teacher.getId(), session);

        //assertEquals(200, response.getStatusCode().value());
       // assertNotNull(response.getBody());
        
       // String csvContent = new String(response.getBody(), StandardCharsets.UTF_8);
        
        // Logs para debug em caso de falha (ajuda a entender o que foi gerado)
        // System.out.println("CSV GENERATED:\n" + csvContent);

        // Validações
        //assertTrue(csvContent.startsWith("\uFEFF"));
        //assertTrue(csvContent.contains("Report Generated"));
        //assertTrue(csvContent.contains("\"Prof Export\""));
        
        // Validar Aluno e Score
        //assertTrue(csvContent.contains("\"Aluno Export\""));
        //assertTrue(csvContent.contains("150"));
        
        // Validar Equipa
        // Se este assert falhar, significa que o findTeamsByUserId não encontrou a associação
        //assertTrue(csvContent.contains("\"Team CSV\""), 
            //"O CSV devia conter o nome da equipa 'Team CSV'. Conteúdo: " + csvContent);
        
        // Validar Projeto/Curso na secção de equipas
        //assertTrue(csvContent.contains("\"Course CSV\""));
        //assertTrue(csvContent.contains("\"Project CSV\""));
    //}

    @Test
    void testExportCsv_Success_EmptyData() {
        User emptyProf = new User();
        emptyProf.setName("Empty");
        emptyProf.setRole("TEACHER");
        emptyProf.setEmail("e@e.com");
        emptyProf = userRepo.save(emptyProf);
        
        session.setAttribute("currentUserId", emptyProf.getId());
        session.setAttribute("currentUserRole", "TEACHER");

        ResponseEntity<byte[]> response = exportController.exportRankingsCsv(emptyProf.getId(), session);

        assertEquals(200, response.getStatusCode().value());
        String csv = new String(response.getBody(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Total Students;0"));
    }
    
    @Test
    void testExportCsv_CourseWithoutTeams() {
        Course c2 = new Course();
        c2.setName("Empty Course");
        c2.setTeacher(teacher);
        courseRepo.save(c2);
        
        session.setAttribute("currentUserId", teacher.getId());
        session.setAttribute("currentUserRole", "TEACHER");
        
        entityManager.flush();
        entityManager.clear();

        ResponseEntity<byte[]> response = exportController.exportRankingsCsv(teacher.getId(), session);
        assertEquals(200, response.getStatusCode().value());
        
        String csv = new String(response.getBody(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"Empty Course\""));
        assertTrue(csv.contains("(no teams)"));
    }

    @Test
    void testExportCsv_ExceptionHandling_UserNotFound() {
        User ghost = new User(); ghost.setName(null);
        ghost.setRole("STUDENT"); 
        ghost = userRepo.save(ghost);
        
        Enrollment e = new Enrollment(); e.setStudent(ghost); e.setCourse(course); enrollmentRepo.save(e);
        
        Score s = new Score(); s.setUser(ghost); s.setTotalPoints(10); scoreRepo.save(s);
        
        entityManager.flush();
        entityManager.clear();
        
        session.setAttribute("currentUserId", teacher.getId());
        session.setAttribute("currentUserRole", "TEACHER");
        
        ResponseEntity<byte[]> response = exportController.exportRankingsCsv(teacher.getId(), session);
        assertEquals(200, response.getStatusCode().value());
    }
    
    @Test
    void testGetStudentTeamMap_Logic() {
        // Criar NOVO Aluno
        User student2 = new User();
        student2.setName("Aluno 2");
        student2.setEmail("a2@upt.pt");
        student2.setRole("STUDENT");
        student2 = userRepo.save(student2);

        // Novo Curso e Projeto
        Course c2 = new Course(); c2.setName("C2"); c2.setTeacher(teacher); c2 = courseRepo.save(c2);
        Project p2 = new Project(); p2.setName("P2"); p2.setCourse(c2); p2 = projectRepo.save(p2);
        
        // Nova Equipa com o aluno novo (Importante: adicionar como Developer)
        Team t2 = new Team(); 
        t2.setName("Team C2"); 
        t2.setCourse(c2); 
        t2.setProject(p2); 
        t2.setScrumMaster(student2);
        t2.setDevelopers(new ArrayList<>(List.of(student2))); // Lista mutável
        t2 = teamRepo.save(t2);
        
        Enrollment e2 = new Enrollment(); e2.setStudent(student2); e2.setCourse(c2); enrollmentRepo.save(e2);
        
        // Score para o aluno novo
        Score s2 = new Score(); 
        s2.setUser(student2); 
        s2.setTeam(t2); 
        s2.setTotalPoints(50); 
        scoreRepo.save(s2);
        
        entityManager.flush();
        entityManager.clear();
        
        session.setAttribute("currentUserId", teacher.getId());
        session.setAttribute("currentUserRole", "TEACHER");
        
        ResponseEntity<byte[]> response = exportController.exportRankingsCsv(teacher.getId(), session);
        String csv = new String(response.getBody(), StandardCharsets.UTF_8);
        
        // Verifica se pelo menos uma das equipas aparece
        assertTrue(csv.contains("Team CSV") || csv.contains("Team C2"), 
                   "CSV devia conter Team CSV ou Team C2. Recebido: " + csv);
    }
}