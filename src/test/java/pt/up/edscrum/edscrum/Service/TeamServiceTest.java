package pt.up.edscrum.edscrum.Service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.model.*;
import pt.up.edscrum.repository.*;
import pt.up.edscrum.service.TeamService;

@SpringBootTest
@Transactional
class TeamServiceTest {

    @Autowired private TeamService teamService;
    @Autowired private EntityManager entityManager;

    @Autowired private TeamRepository teamRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private AwardRepository awardRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;

    private Course course;
    private User s1, s2, s3, s4;

    @BeforeEach
    void setUp() {
        // 1. Limpeza Completa
        notificationRepo.deleteAll();
        teamAwardRepo.deleteAll();
        studentAwardRepo.deleteAll();
        scoreRepo.deleteAll();
        teamRepo.deleteAll();
        projectRepo.deleteAll();
        enrollmentRepo.deleteAll();
        awardRepo.deleteAll();
        courseRepo.deleteAll();
        userRepo.deleteAll();

        // 2. Criar "Alunos Fantasma" para ocupar o Top 5
        for (int i = 0; i < 5; i++) {
            User dummy = new User();
            dummy.setName("Dummy " + i);
            dummy.setEmail("dummy" + i + "@test.com");
            dummy.setRole("STUDENT");
            userRepo.save(dummy);
            Score s = new Score(); s.setUser(dummy); s.setTotalPoints(10000); 
            scoreRepo.save(s);
        }

        // 3. Setup Base
        course = new Course();
        course.setName("Engenharia SW");
        course = courseRepo.save(course);

        s1 = createUser("Student 1", "s1@test.com", "STUDENT");
        s2 = createUser("Student 2", "s2@test.com", "STUDENT");
        s3 = createUser("Student 3", "s3@test.com", "STUDENT");
        s4 = createUser("Student 4", "s4@test.com", "STUDENT");

        enroll(s1, course);
        enroll(s2, course);
        enroll(s3, course);

        entityManager.flush();
        entityManager.clear();
    }

    // ===========================================
    // TESTES: CREATE
    // ===========================================

    @Test
    void testCreateTeam_Success() {
        Course c = courseRepo.findById(course.getId()).get();
        User u1 = userRepo.findById(s1.getId()).get();
        User u2 = userRepo.findById(s2.getId()).get();

        Team team = new Team();
        team.setName("Alpha Team");
        team.setCourse(c);
        team.setScrumMaster(u1);
        team.setProductOwner(u2);
        team.setMaxMembers(5);
        
        notificationRepo.deleteAll(); // Limpar notificações anteriores
        entityManager.flush();
        
        Team created = teamService.createTeam(team);

        assertNotNull(created.getId());
        assertEquals("Alpha Team", created.getName());
        assertEquals(4, notificationRepo.count()); // 2 msg + 2 awards
        assertFalse(teamAwardRepo.findByTeamId(created.getId()).isEmpty());
    }

    @Test
    void testCreateTeam_Fail_StudentNotEnrolled() {
        Course c = courseRepo.findById(course.getId()).get();
        User notEnrolled = userRepo.findById(s4.getId()).get(); 

        Team team = new Team();
        team.setName("Invalid Team");
        team.setCourse(c);
        team.setScrumMaster(notEnrolled);

        Exception e = assertThrows(RuntimeException.class, () -> teamService.createTeam(team));
        assertTrue(e.getMessage().contains("não está inscrito"));
    }

    @Test
    void testCreateTeam_Fail_StudentAlreadyInTeam() {
        Course c = courseRepo.findById(course.getId()).get();
        User u1 = userRepo.findById(s1.getId()).get();

        Team t1 = new Team(); t1.setName("Team 1"); t1.setCourse(c); t1.setScrumMaster(u1);
        teamService.createTeam(t1);

        Team t2 = new Team(); t2.setName("Team 2"); t2.setCourse(c); t2.setDevelopers(List.of(u1)); 

        Exception e = assertThrows(RuntimeException.class, () -> teamService.createTeam(t2));
        assertTrue(e.getMessage().contains("já pertence a uma equipa"));
    }
    
    @Test
    void testCreateTeam_TeacherBypassesValidation() {
        // Professores não precisam de estar inscritos para serem PO/SM
        Course c = courseRepo.findById(course.getId()).get();
        User teacher = createUser("Prof", "prof@upt.pt", "TEACHER");
        
        Team team = new Team();
        team.setName("Teacher Team");
        team.setCourse(c);
        team.setProductOwner(teacher); // Professor como PO
        
        assertDoesNotThrow(() -> teamService.createTeam(team));
    }

    // ===========================================
    // TESTES: LOGICA DE NEGÓCIO AVANÇADA (AWARDS)
    // ===========================================

    @Test
    void testColaboradorEstelarAward() {
        // Cenário: Aluno participa em 3 projetos diferentes em 3 CURSOS diferentes
        // (Para contornar a regra de "1 equipa por curso")
        
        User star = userRepo.findById(s1.getId()).get();
        
        // --- Contexto 1 (Curso do Setup) ---
        Course c1 = courseRepo.findById(course.getId()).get();
        Project p1 = createProject(c1, "P1");
        
        Team t1 = new Team(); 
        t1.setName("T1"); 
        t1.setCourse(c1); 
        t1.setProject(p1); 
        t1.setScrumMaster(star);
        teamRepo.save(t1);
        
        // --- Contexto 2 (Novo Curso) ---
        Course c2 = new Course(); c2.setName("Curso Extra 2"); c2 = courseRepo.save(c2);
        enroll(star, c2); // Inscrever aluno
        Project p2 = createProject(c2, "P2");
        
        Team t2 = new Team(); 
        t2.setName("T2"); 
        t2.setCourse(c2); 
        t2.setProject(p2); 
        t2.setDevelopers(new ArrayList<>(List.of(star)));
        teamRepo.save(t2);
        
        // --- Contexto 3 (Novo Curso - Onde vamos criar a equipa via Service) ---
        Course c3 = new Course(); c3.setName("Curso Extra 3"); c3 = courseRepo.save(c3);
        enroll(star, c3); // Inscrever aluno
        Project p3 = createProject(c3, "P3");
        
        Team t3 = new Team(); 
        t3.setName("T3"); 
        t3.setCourse(c3); 
        t3.setProject(p3); 
        t3.setProductOwner(star);
        
        entityManager.flush();
        entityManager.clear();
        
        // Ação: Criar a 3ª equipa via Service
        // Como é num curso novo (c3), a validação "já tem equipa neste curso" vai passar.
        // E como o aluno já tem histórico nos projetos P1 e P2, o total será 3.
        teamService.createTeam(t3);
        
        // Verificação: Deve ter ganho "Colaborador Estelar"
        boolean hasAward = studentAwardRepo.findAllByStudentId(star.getId()).stream()
                .anyMatch(sa -> sa.getAward().getName().equals("Colaborador Estelar"));
        
        assertTrue(hasAward, "O aluno devia ter ganho o prémio Colaborador Estelar ao entrar no 3º projeto");
    }

    // ===========================================
    // TESTES: GETTERS & FINDERS
    // ===========================================

    @Test
    void testGetAllTeams() {
        Course c = courseRepo.findById(course.getId()).get();
        Team t1 = new Team(); t1.setCourse(c); t1.setName("A"); teamRepo.save(t1);
        Team t2 = new Team(); t2.setCourse(c); t2.setName("B"); teamRepo.save(t2);
        
        List<Team> all = teamService.getAllTeams();
        assertEquals(2, all.size());
    }
    
    @Test
    void testFindTeamsByUserId() {
        Course c = courseRepo.findById(course.getId()).get();
        User u = userRepo.findById(s1.getId()).get();
        
        Team t1 = new Team(); t1.setCourse(c); t1.setName("A"); t1.setScrumMaster(u); teamRepo.save(t1);
        Team t2 = new Team(); t2.setCourse(c); t2.setName("B"); t2.setDevelopers(List.of(u)); teamRepo.save(t2);
        
        List<Team> userTeams = teamService.findTeamsByUserId(u.getId());
        assertEquals(2, userTeams.size());
    }

    @Test
    void testGetTakenStudentsMap() {
        Course c = courseRepo.findById(course.getId()).get();
        User sm = userRepo.findById(s1.getId()).get();
        User dev = userRepo.findById(s2.getId()).get();
        
        Team t = new Team();
        t.setName("Map Team");
        t.setCourse(c);
        t.setScrumMaster(sm);
        t.setDevelopers(List.of(dev));
        teamRepo.save(t);
        
        entityManager.flush();
        entityManager.clear();
        
        Map<Long, Set<Long>> map = teamService.getTakenStudentsMap();
        
        assertTrue(map.containsKey(c.getId()));
        Set<Long> takenIds = map.get(c.getId());
        assertTrue(takenIds.contains(sm.getId()));
        assertTrue(takenIds.contains(dev.getId()));
        assertFalse(takenIds.contains(s3.getId()));
    }

    @Test
    void testGetStudentTeamInCourse_AllRoles() {
        Course c = courseRepo.findById(course.getId()).get();
        User sm = userRepo.findById(s1.getId()).get();
        User po = userRepo.findById(s2.getId()).get();
        User dev = userRepo.findById(s3.getId()).get();
        
        Team t = new Team();
        t.setName("Full Team");
        t.setCourse(c);
        t.setScrumMaster(sm);
        t.setProductOwner(po);
        t.setDevelopers(List.of(dev));
        teamRepo.save(t);
        
        assertEquals("Full Team", teamService.getStudentTeamInCourse(sm.getId(), c.getId()).getName());
        assertEquals("Full Team", teamService.getStudentTeamInCourse(po.getId(), c.getId()).getName());
        assertEquals("Full Team", teamService.getStudentTeamInCourse(dev.getId(), c.getId()).getName());
    }

    // ===========================================
    // TESTES: UPDATE & ADD MEMBERS
    // ===========================================

    @Test
    void testUpdateTeam() {
        Course c = courseRepo.findById(course.getId()).get();
        Team t = new Team(); t.setName("Old"); t.setCourse(c); 
        t = teamRepo.save(t);
        
        Team details = new Team();
        details.setName("New");
        details.setCourse(c); // Manter curso
        
        Team updated = teamService.updateTeam(t.getId(), details);
        assertEquals("New", updated.getName());
    }

    @Test
    void testAddStudentToTeam_DeveloperOverload() {
        Course c = courseRepo.findById(course.getId()).get();
        User u = userRepo.findById(s1.getId()).get();
        Team t = new Team(); t.setCourse(c); t.setMaxMembers(5);
        t = teamRepo.save(t);
        
        // Usar o método curto (overload)
        Team res = teamService.addStudentToTeam(t.getId(), u.getId(), u);
        
        assertEquals(1, res.getDevelopers().size());
        assertEquals(u.getId(), res.getDevelopers().get(0).getId());
    }
    
    @Test
    void testAddStudentToTeam_SpecificRoles_AndConflicts() {
        Course c = courseRepo.findById(course.getId()).get();
        User u1 = userRepo.findById(s1.getId()).get();
        User u2 = userRepo.findById(s2.getId()).get();
        
        Team t = new Team(); t.setCourse(c); t.setMaxMembers(5);
        final Team savedT = teamRepo.save(t);
        
        // Add SM
        teamService.addStudentToTeamWithRole(savedT.getId(), u1.getId(), u1, "SCRUM_MASTER");
        // Try add another SM
        assertThrows(RuntimeException.class, () -> 
            teamService.addStudentToTeamWithRole(savedT.getId(), u2.getId(), u2, "SCRUM_MASTER"));
    }
    
    @Test
    void testCloseTeam() {
        Course c = courseRepo.findById(course.getId()).get();
        Team t = new Team(); t.setCourse(c); t.setClosed(false);
        t = teamRepo.save(t);
        
        Team closed = teamService.closeTeam(t.getId());
        assertTrue(closed.isClosed());
    }

    // ===========================================
    // TESTES: DELETE & REMOVE
    // ===========================================

    @Test
    void testRemoveStudentFromTeam_AndReopen() {
        Course c = courseRepo.findById(course.getId()).get();
        User u1 = userRepo.findById(s1.getId()).get();
        User u2 = userRepo.findById(s2.getId()).get();

        Team team = new Team();
        team.setCourse(c);
        team.setScrumMaster(u1);
        team.setDevelopers(new ArrayList<>(List.of(u2)));
        team.setMaxMembers(2);
        team.setClosed(true);
        
        Team savedTeam = teamRepo.save(team);

        Team result = teamService.removeStudentFromTeam(savedTeam.getId(), u2.getId());

        assertNotNull(result);
        assertEquals(0, result.getDevelopers().size());
        assertFalse(result.isClosed());
    }
    
    @Test
    void testRemoveStudentFromTeam_Fail_NotMember() {
        Course c = courseRepo.findById(course.getId()).get();
        User u1 = userRepo.findById(s1.getId()).get();
        User u2 = userRepo.findById(s2.getId()).get(); // Não membro
        
        Team t = new Team(); t.setCourse(c); t.setScrumMaster(u1);
        final Team savedTeam = teamRepo.save(t);
        
        assertThrows(RuntimeException.class, () -> 
            teamService.removeStudentFromTeam(savedTeam.getId(), u2.getId()));
    }

    @Test
    void testRemoveLastMember_DeletesTeam() {
        Course c = courseRepo.findById(course.getId()).get();
        User u1 = userRepo.findById(s1.getId()).get();

        Team team = new Team();
        team.setCourse(c);
        team.setScrumMaster(u1);
        
        Team savedTeam = teamRepo.save(team);

        Team result = teamService.removeStudentFromTeam(savedTeam.getId(), u1.getId());

        assertNull(result);
        assertFalse(teamRepo.existsById(savedTeam.getId()));
    }

    @Test
    void testDeleteTeam_CleansDependencies() {
        Course c = courseRepo.findById(course.getId()).get();
        User u1 = userRepo.findById(s1.getId()).get();

        Team team = new Team();
        team.setName("To Delete");
        team.setCourse(c);
        team.setScrumMaster(u1);
        
        Team savedTeam = teamRepo.save(team);

        Score score = new Score();
        score.setTeam(savedTeam); 
        score.setTotalPoints(10);
        scoreRepo.save(score);

        TeamAward ta = new TeamAward();
        ta.setTeam(savedTeam); 
        ta.setPointsEarned(50);
        teamAwardRepo.save(ta);

        entityManager.flush();
        entityManager.clear();

        teamService.deleteTeam(savedTeam.getId());

        assertFalse(teamRepo.existsById(savedTeam.getId()));
        assertEquals(0, scoreRepo.findByTeamId(savedTeam.getId()).size());
        assertEquals(0, teamAwardRepo.findByTeamId(savedTeam.getId()).size());
    }

    // ===========================================
    // TESTES UTILITÁRIOS EXTRA
    // ===========================================

    @Test
    void testGetTakenStudentIdsByCourse() {
        Course c = courseRepo.findById(course.getId()).get();
        User u1 = userRepo.findById(s1.getId()).get();
        User u2 = userRepo.findById(s2.getId()).get();

        Team t1 = new Team();
        t1.setCourse(c);
        t1.setScrumMaster(u1);
        t1.setDevelopers(List.of(u2));
        teamRepo.save(t1);

        Set<Long> takenIds = teamService.getTakenStudentIdsByCourse(c.getId());

        assertTrue(takenIds.contains(u1.getId()));
        assertTrue(takenIds.contains(u2.getId()));
        assertFalse(takenIds.contains(s3.getId()));
    }

    @Test
    void testGetAvailableTeamsForStudentByCourse() {
        Course c = courseRepo.findById(course.getId()).get();
        
        Team tOpen = new Team(); tOpen.setName("Open"); tOpen.setCourse(c); tOpen.setMaxMembers(5); tOpen.setClosed(false);
        teamRepo.save(tOpen);

        Team tClosed = new Team(); tClosed.setName("Closed"); tClosed.setCourse(c); tClosed.setMaxMembers(5); tClosed.setClosed(true);
        teamRepo.save(tClosed);

        List<Team> available = teamService.getAvailableTeamsForStudentByCourse(c.getId());

        assertEquals(1, available.size());
        assertEquals("Open", available.get(0).getName());
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
    
    private Project createProject(Course c, String name) {
        Project p = new Project();
        p.setCourse(c);
        p.setName(name);
        return projectRepo.save(p);
    }
}