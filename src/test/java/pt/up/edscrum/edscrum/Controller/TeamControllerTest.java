package pt.up.edscrum.edscrum.Controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import pt.up.edscrum.controller.TeamController;
import pt.up.edscrum.model.*;
import pt.up.edscrum.repository.*;

/**
 * Testes de integração para o TeamController.
 * <p>
 * Verifica as operações de gestão de equipas via API REST,
 * incluindo permissões, adesão de membros e listagens.
 * </p>
 */
@SpringBootTest
@Transactional
class TeamControllerTest {

    @Autowired private TeamController teamController;
    @Autowired private EntityManager entityManager;

    @Autowired private UserRepository userRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    
    // Repositórios para limpeza
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private UserStoryRepository userStoryRepo;
    @Autowired private SprintRepository sprintRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private AwardRepository awardRepo;

    private User teacher;
    private User student;
    private User student2;
    private Course course;
    private Team team;
    
    private MockHttpSession session;

    /**
     * Configuração inicial do cenário de teste.
     */
    @BeforeEach
    void setUp() {
        // Limpeza
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

        // Users
        teacher = createUser("Teacher", "t@upt.pt", "TEACHER");
        student = createUser("Student1", "s1@upt.pt", "STUDENT");
        student2 = createUser("Student2", "s2@upt.pt", "STUDENT");

        // Course & Enrollments
        course = new Course();
        course.setName("Agile");
        course.setTeacher(teacher);
        course = courseRepo.save(course);
        
        enroll(student, course);
        enroll(student2, course);

        // Team (Student is SM)
        team = new Team();
        team.setName("Team Alpha");
        team.setCourse(course);
        team.setScrumMaster(student);
        team.setMaxMembers(5);
        team = teamRepo.save(team);

        session = new MockHttpSession();
        
        entityManager.flush();
        entityManager.clear();
    }

    // ==========================================
    // GET TEAMS (ALL & BY ID)
    // ==========================================

    @Test
    void testGetAllTeams() {
        List<Team> teams = teamController.getAllTeams();
        assertFalse(teams.isEmpty());
        assertEquals("Team Alpha", teams.get(0).getName());
    }

    @Test
    void testGetTeamById() {
        ResponseEntity<Team> resp = teamController.getTeamById(team.getId());
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Team Alpha", resp.getBody().getName());
    }

    // ==========================================
    // CRUD TEAMS (CREATE/UPDATE/DELETE)
    // ==========================================

    @Test
    void testCreateTeam() {
        Team t = new Team();
        t.setName("New Team");
        t.setCourse(course);
        
        // Simular envio de objeto com SM
        t.setScrumMaster(student2); 
        
        ResponseEntity<Team> resp = teamController.createTeam(t);
        assertEquals(201, resp.getStatusCode().value());
        assertNotNull(resp.getBody().getId());
    }

    @Test
    void testUpdateTeam() {
        Team details = new Team();
        details.setName("Updated Name");
        
        ResponseEntity<Team> resp = teamController.updateTeam(team.getId(), details);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Updated Name", resp.getBody().getName());
    }

    @Test
    void testDeleteTeam() {
        ResponseEntity<Void> resp = teamController.deleteTeam(team.getId());
        assertEquals(204, resp.getStatusCode().value());
        assertFalse(teamRepo.existsById(team.getId()));
    }

    // ==========================================
    // STUDENT TEAM IN COURSE
    // ==========================================

    @Test
    void testGetStudentTeamInCourse_Success_Student() {
        setSession(student);
        ResponseEntity<?> resp = teamController.getStudentTeamInCourse(student.getId(), course.getId(), session);
        assertEquals(200, resp.getStatusCode().value());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertEquals("Team Alpha", body.get("name"));
    }

    @Test
    void testGetStudentTeamInCourse_Forbidden() {
        setSession(student2); // Student 2 tries to see Student 1's team logic (ID mismatch)
        ResponseEntity<?> resp = teamController.getStudentTeamInCourse(student.getId(), course.getId(), session);
        assertEquals(403, resp.getStatusCode().value());
    }

    @Test
    void testGetStudentTeamInCourseSession_Success() {
        setSession(student);
        ResponseEntity<?> resp = teamController.getStudentTeamInCourseSession(course.getId(), null, session);
        assertEquals(200, resp.getStatusCode().value());
    }

    // ==========================================
    // AVAILABLE TEAMS & COURSEMATES
    // ==========================================

    @Test
    void testGetAvailableTeamsByCourse() {
        ResponseEntity<List<Map<String, Object>>> resp = teamController.getAvailableTeamsByCourse(course.getId());
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().size()); // Team Alpha is open
    }

    @Test
    void testGetCoursemates() {
        // Student2 ainda não tem equipa
        ResponseEntity<Map<String, Object>> resp = teamController.getCoursemates(course.getId());
        assertEquals(200, resp.getStatusCode().value());
        
        @SuppressWarnings("unchecked")
        List<User> students = (List<User>) resp.getBody().get("students");
        // Deve conter Student2, mas não Student (que já é SM)
        assertTrue(students.stream().anyMatch(u -> u.getId().equals(student2.getId())));
        assertFalse(students.stream().anyMatch(u -> u.getId().equals(student.getId())));
    }

    // ==========================================
    // JOIN TEAM & ADD MEMBER
    // ==========================================

    @Test
    void testJoinTeam_Success() {
        setSession(student2);
        Map<String, Object> payload = new HashMap<>();
        payload.put("role", "DEVELOPER");
        
        ResponseEntity<Map<String, String>> resp = teamController.joinTeam(team.getId(), payload, session);
        assertEquals(200, resp.getStatusCode().value());
        
        // Verificar se foi adicionado
        Team t = teamRepo.findById(team.getId()).get();
        assertEquals(1, t.getDevelopers().size());
    }

    @Test
    void testJoinTeam_Forbidden() {
        setSession(student); // Tentar adicionar outro aluno
        Map<String, Object> payload = new HashMap<>();
        payload.put("studentId", student2.getId());
        
        ResponseEntity<Map<String, String>> resp = teamController.joinTeam(team.getId(), payload, session);
        assertEquals(403, resp.getStatusCode().value());
    }

    @Test
    void testAddMemberByTeacher_Success() {
        setSession(teacher);
        Map<String, Object> payload = new HashMap<>();
        payload.put("studentId", student2.getId());
        payload.put("role", "DEVELOPER");
        
        ResponseEntity<?> resp = teamController.addMemberByTeacher(team.getId(), payload, session);
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void testAddMemberByTeacher_Forbidden() {
        setSession(student); // Aluno tenta fingir ser professor
        Map<String, Object> payload = new HashMap<>();
        payload.put("studentId", student2.getId());
        payload.put("role", "DEVELOPER");
        
        ResponseEntity<?> resp = teamController.addMemberByTeacher(team.getId(), payload, session);
        assertEquals(403, resp.getStatusCode().value());
    }
    
   @Test
    void testAddMemberByTeacher_Unauthorized() {
        // Passar null como sessão
        Map<String, Object> payload = new HashMap<>();
        payload.put("studentId", 1L);
        payload.put("role", "DEVELOPER");
        
        ResponseEntity<?> resp = teamController.addMemberByTeacher(team.getId(), payload, null);
        assertEquals(401, resp.getStatusCode().value());
        
        // Passar sessão vazia (sem atributos de login)
        MockHttpSession emptySession = new MockHttpSession();
        resp = teamController.addMemberByTeacher(team.getId(), payload, emptySession);
        assertEquals(401, resp.getStatusCode().value());
    }

    // --- Helpers ---

    private User createUser(String name, String email, String role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword("pass");
        u.setRole(role);
        return userRepo.save(u);
    }

    private void enroll(User u, Course c) {
        Enrollment e = new Enrollment();
        e.setStudent(u);
        e.setCourse(c);
        enrollmentRepo.save(e);
    }

    private void setSession(User u) {
        session.setAttribute("currentUserId", u.getId());
        session.setAttribute("currentUserRole", u.getRole());
    }
}