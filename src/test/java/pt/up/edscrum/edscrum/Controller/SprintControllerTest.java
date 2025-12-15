package pt.up.edscrum.edscrum.Controller;

import static org.junit.jupiter.api.Assertions.*;

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
import pt.up.edscrum.controller.SprintController;
import pt.up.edscrum.enums.ProjectStatus;
import pt.up.edscrum.model.*;
import pt.up.edscrum.repository.*;

@SpringBootTest
@Transactional
class SprintControllerTest {

    @Autowired private SprintController sprintController;
    @Autowired private EntityManager entityManager;

    @Autowired private UserRepository userRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private SprintRepository sprintRepo;
    
    // Repositórios para limpeza
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private UserStoryRepository userStoryRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private AwardRepository awardRepo;

    private User teacher;
    private User creator;
    private User member;
    private User stranger;
    
    private Course course;
    private Project project;
    private Team team;
    private Sprint sprint;
    
    private MockHttpSession session;

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
        creator = createUser("Creator", "c@upt.pt", "STUDENT");
        member = createUser("Member", "m@upt.pt", "STUDENT");
        stranger = createUser("Stranger", "s@upt.pt", "STUDENT");

        // Hierarchy
        course = new Course();
        course.setName("Agile 101");
        course.setTeacher(teacher);
        course = courseRepo.save(course);

        project = new Project();
        project.setName("Project X");
        project.setCourse(course);
        project.setStatus(ProjectStatus.EM_CURSO);
        project = projectRepo.save(project);

        team = new Team();
        team.setName("Team X");
        team.setCourse(course);
        team.setProject(project);
        team.setScrumMaster(creator); // Creator is SM
        
        // Add member as developer
        List<User> devs = new ArrayList<>();
        devs.add(member);
        team.setDevelopers(devs);
        
        team = teamRepo.save(team);

        sprint = new Sprint();
        sprint.setName("Sprint 1");
        sprint.setProject(project);
        sprint.setCreatedBy(creator);
        sprint = sprintRepo.save(sprint);

        session = new MockHttpSession();
        
        entityManager.flush();
        entityManager.clear();
    }

    // ==========================================
    // GET SPRINTS
    // ==========================================

    @Test
    void testGetSprints_NoSession() {
        ResponseEntity<?> resp = sprintController.getSprints(project.getId(), session);
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void testGetSprints_Success() {
        setSession(creator);
        ResponseEntity<List<Sprint>> resp = sprintController.getSprints(project.getId(), session);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(1, resp.getBody().size());
    }

    // ==========================================
    // CREATE SPRINT
    // ==========================================

    @Test
    void testCreateSprint_NoSession() {
        ResponseEntity<?> resp = sprintController.createSprint(project.getId(), new Sprint(), session);
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void testCreateSprint_Success_Creator() {
        setSession(creator);
        Sprint newSprint = new Sprint();
        newSprint.setName("S2");
        
        // Simular envio sem createdBy (deve assumir current user)
        ResponseEntity<Sprint> resp = sprintController.createSprint(project.getId(), newSprint, session);
        
        assertEquals(201, resp.getStatusCode().value());
        assertNotNull(resp.getBody().getCreatedBy());
        assertEquals(creator.getId(), resp.getBody().getCreatedBy().getId());
    }

    @Test
    void testCreateSprint_Success_Teacher() {
        setSession(teacher);
        Sprint newSprint = new Sprint();
        newSprint.setName("S_Teacher");
        
        // Professor a criar em nome de outro (ex: creator)
        newSprint.setCreatedBy(creator);
        
        ResponseEntity<Sprint> resp = sprintController.createSprint(project.getId(), newSprint, session);
        assertEquals(201, resp.getStatusCode().value());
    }

    @Test
    void testCreateSprint_Forbidden_Impersonation() {
        setSession(stranger);
        Sprint newSprint = new Sprint();
        newSprint.setName("Hacked Sprint");
        newSprint.setCreatedBy(creator); // Stranger tenta criar como Creator
        
        ResponseEntity<Sprint> resp = sprintController.createSprint(project.getId(), newSprint, session);
        assertEquals(403, resp.getStatusCode().value());
    }

    // ==========================================
    // UPDATE SPRINT
    // ==========================================

    @Test
    void testUpdateSprint_NoSession() {
        ResponseEntity<?> resp = sprintController.updateSprint(sprint.getId(), new Sprint(), session);
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void testUpdateSprint_Success_Creator() {
        setSession(creator);
        Sprint update = new Sprint();
        update.setName("Updated Name");
        
        ResponseEntity<Sprint> resp = sprintController.updateSprint(sprint.getId(), update, session);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("Updated Name", resp.getBody().getName());
    }

    @Test
    void testUpdateSprint_Success_Teacher() {
        setSession(teacher);
        Sprint update = new Sprint();
        update.setName("Teacher Update");
        
        ResponseEntity<Sprint> resp = sprintController.updateSprint(sprint.getId(), update, session);
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void testUpdateSprint_Forbidden_Stranger() {
        setSession(stranger);
        Sprint update = new Sprint();
        update.setName("Hacked");
        
        ResponseEntity<Sprint> resp = sprintController.updateSprint(sprint.getId(), update, session);
        assertEquals(403, resp.getStatusCode().value());
    }

    // ==========================================
    // COMPLEX PERMISSIONS (Complete/Start/Reopen/Delete)
    // ==========================================
    // Nota: A lógica de permissão é idêntica para estes 4 métodos.
    // Vamos testar exaustivamente no 'complete' e validar o básico nos outros.

    @Test
    void testCompleteSprint_Success_Teacher() {
        setSession(teacher);
        ResponseEntity<Sprint> resp = sprintController.completeSprint(sprint.getId(), session);
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void testCompleteSprint_Success_Creator( ){
        setSession(creator);
        ResponseEntity<Sprint> resp = sprintController.completeSprint(sprint.getId(), session);
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void testCompleteSprint_Success_ProjectMember() {
        // 'member' é developer na equipa do projeto
        setSession(member);
        ResponseEntity<Sprint> resp = sprintController.completeSprint(sprint.getId(), session);
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void testCompleteSprint_Forbidden_Stranger() {
        setSession(stranger);
        ResponseEntity<Sprint> resp = sprintController.completeSprint(sprint.getId(), session);
        assertEquals(403, resp.getStatusCode().value());
    }
    
    @Test
    void testReopenSprint_Success_Member() {
        setSession(member);
        ResponseEntity<Sprint> resp = sprintController.reopenSprint(sprint.getId(), session);
        assertEquals(200, resp.getStatusCode().value());
    }
    
    @Test
    void testStartSprint_Success_Member() {
        setSession(member);
        ResponseEntity<Sprint> resp = sprintController.startSprint(sprint.getId(), session);
        assertEquals(200, resp.getStatusCode().value());
    }
    
    @Test
    void testDeleteSprint_Success_Teacher() {
        setSession(teacher);
        ResponseEntity<Void> resp = sprintController.deleteSprint(sprint.getId(), session);
        assertEquals(204, resp.getStatusCode().value());
        assertFalse(sprintRepo.existsById(sprint.getId()));
    }
    
    @Test
    void testDeleteSprint_Forbidden_Stranger() {
        setSession(stranger);
        ResponseEntity<Void> resp = sprintController.deleteSprint(sprint.getId(), session);
        assertEquals(403, resp.getStatusCode().value());
    }

    // ==========================================
    // EXCEPTION & EDGE CASES (Catch Blocks)
    // ==========================================

    @Test
    void testPermissionLogic_OrphanSprint_CatchBlocks() {
        // Criar Sprint sem projeto (Orphan) para forçar NullPointerException
        // dentro dos blocos try-catch do controller ao tentar aceder a sprint.getProject().getTeams()
        Sprint orphan = new Sprint();
        orphan.setName("Orphan");
        orphan.setCreatedBy(creator);
        orphan = sprintRepo.save(orphan);
        
        entityManager.flush();
        entityManager.clear();

        // 1. Testar como Stranger (deve dar 403, pois não é teacher e o catch ignora o erro de projecto)
        setSession(stranger);
        
        // completeSprint
        ResponseEntity<Sprint> resp = sprintController.completeSprint(orphan.getId(), session);
        assertEquals(403, resp.getStatusCode().value());
        
        // updateSprint (também tem try-catch para teacher do curso)
        ResponseEntity<Sprint> respUp = sprintController.updateSprint(orphan.getId(), new Sprint(), session);
        assertEquals(403, respUp.getStatusCode().value());
        
        // 2. Testar como Creator (deve dar 200, pois owner check passa fora do bloco try-catch)
        setSession(creator);
        ResponseEntity<Sprint> respOk = sprintController.completeSprint(orphan.getId(), session);
        assertEquals(200, respOk.getStatusCode().value());
    }
    
    @Test
    void testProjectMemberCheck_ProductOwner() {
        // Testar especificamente se um PO (que não é o criador do sprint) consegue aceder
        User po = createUser("PO User", "po@upt.pt", "STUDENT");
        Team t2 = new Team();
        t2.setCourse(course);
        t2.setProject(project);
        t2.setProductOwner(po); // PO set
        teamRepo.save(t2);
        
        entityManager.flush();
        entityManager.clear();
        
        setSession(po);
        ResponseEntity<Sprint> resp = sprintController.completeSprint(sprint.getId(), session);
        assertEquals(200, resp.getStatusCode().value());
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

    private void setSession(User u) {
        session.setAttribute("currentUserId", u.getId());
        session.setAttribute("currentUserRole", u.getRole());
    }
}