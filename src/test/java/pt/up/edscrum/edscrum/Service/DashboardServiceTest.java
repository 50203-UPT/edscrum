package pt.up.edscrum.edscrum.Service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.dto.dashboard.ProjectDetailsDTO;
import pt.up.edscrum.dto.dashboard.StudentDashboardDTO;
import pt.up.edscrum.dto.dashboard.TeacherDashboardDTO;
import pt.up.edscrum.enums.ProjectStatus;
import pt.up.edscrum.enums.SprintStatus;
import pt.up.edscrum.enums.UserStoryPriority;
import pt.up.edscrum.enums.UserStoryStatus;
import pt.up.edscrum.model.*;
import pt.up.edscrum.repository.*;
import pt.up.edscrum.service.DashboardService;

@SpringBootTest
@Transactional
class DashboardServiceTest {

    @Autowired private DashboardService dashboardService;
    @Autowired private EntityManager entityManager;

    @Autowired private CourseRepository courseRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private AwardRepository awardRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private SprintRepository sprintRepo;
    @Autowired private UserStoryRepository userStoryRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private NotificationRepository notificationRepo;

    @BeforeEach
    void setUp() {
        notificationRepo.deleteAll();
        userStoryRepo.deleteAll();
        sprintRepo.deleteAll();
        studentAwardRepo.deleteAll();
        teamAwardRepo.deleteAll();
        scoreRepo.deleteAll();
        teamRepo.deleteAll();
        projectRepo.deleteAll();
        enrollmentRepo.deleteAll();
        awardRepo.deleteAll();
        courseRepo.deleteAll();
        userRepo.deleteAll();
        
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void testGetTeacherDashboard() {
        Course c = new Course();
        c.setName("Engenharia de Software");
        // CORREÇÃO: Usar nova variável para ser "effectively final"
        final Course savedCourse = courseRepo.save(c);

        User s1 = createUser("Student1", "s1@upt.pt", "STUDENT");
        User s2 = createUser("Student2", "s2@upt.pt", "STUDENT");
        createEnrollment(s1, savedCourse);
        createEnrollment(s2, savedCourse);

        Project p = new Project();
        p.setName("Projeto Alpha");
        p.setCourse(savedCourse);
        p.setStatus(ProjectStatus.EM_CURSO);
        p = projectRepo.save(p);

        Sprint sp = new Sprint();
        sp.setProject(p);
        sp.setName("Sprint 1");
        sp.setStatus(SprintStatus.CONCLUIDO);
        sprintRepo.save(sp);

        Team t = new Team();
        t.setName("Team A");
        t.setCourse(savedCourse);
        t.setProject(p);
        teamRepo.save(t);

        entityManager.flush();
        entityManager.clear();

        TeacherDashboardDTO dto = dashboardService.getTeacherDashboard(savedCourse.getId());

        assertNotNull(dto);
        assertEquals("Engenharia de Software", dto.getCourseName());
        assertEquals(2, dto.getTotalStudents());
        assertEquals(1, dto.getTotalTeams());
        assertEquals(1, dto.getTotalProjects());
        assertEquals(1, dto.getProjects().size());
        assertEquals(100.0, dto.getProjects().get(0).getCompletionPercentage());
    }

    @Test
    void testGetStudentDashboard() {
        User student = createUser("João", "joao@upt.pt", "STUDENT");
        Course c = new Course();
        c.setName("Curso A");
        final Course savedCourse = courseRepo.save(c);
        createEnrollment(student, savedCourse);

        Award award = new Award();
        award.setName("Top Coder");
        award.setPoints(50);
        award.setType("MANUAL");
        award.setTargetType("INDIVIDUAL");
        award = awardRepo.save(award);

        StudentAward sa = new StudentAward();
        sa.setStudent(student);
        sa.setAward(award);
        sa.setPointsEarned(50);
        studentAwardRepo.save(sa);
        
        Score score = new Score();
        score.setUser(student);
        score.setTotalPoints(50);
        scoreRepo.save(score);
        
        entityManager.flush();
        entityManager.clear();

        StudentDashboardDTO dto = dashboardService.getStudentDashboard(student.getId());

        assertEquals("João", dto.getName());
        assertEquals(50, dto.getTotalPoints());
        assertEquals(1, dto.getEarnedAwards().size());
        // Aqui assumimos que o DTO interno do StudentDashboard também tem um método getName() ou o campo é público
        // Se der erro, verifica StudentDashboardDTO.java
        assertEquals("Top Coder", dto.getEarnedAwards().get(0).getName());
        assertEquals(1, dto.getEnrolledCourses().size());
    }

    @Test
    void testEnrollStudentInCourse_Success_WithCode() {
        User student = createUser("Maria", "maria@upt.pt", "STUDENT");
        Course c = new Course();
        c.setName("Segurança");
        c.setCode("SECURE123");
        final Course savedCourse = courseRepo.save(c);

        dashboardService.enrollStudentInCourse(student.getId(), savedCourse.getId(), "SECURE123");

        assertTrue(enrollmentRepo.existsByStudentIdAndCourseId(student.getId(), savedCourse.getId()));
    }

    @Test
    void testEnrollStudentInCourse_Fail_WrongCode() {
        User student = createUser("Pedro", "pedro@upt.pt", "STUDENT");
        Course c = new Course();
        c.setName("Segurança");
        c.setCode("SECURE123");
        final Course savedCourse = courseRepo.save(c);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            // Aqui usamos savedCourse, que é final, resolvendo o erro de lambda
            dashboardService.enrollStudentInCourse(student.getId(), savedCourse.getId(), "WRONG");
        });

        assertEquals("O código de acesso está incorreto.", exception.getMessage());
    }

    @Test
    void testGetProjectDetails() {
        Course c = new Course(); 
        c.setName("Curso Proj"); 
        final Course savedCourse = courseRepo.save(c);
        
        Project p = new Project();
        p.setName("Super Projeto");
        p.setCourse(savedCourse);
        p.setStatus(ProjectStatus.EM_CURSO);
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusDays(30));
        p = projectRepo.save(p);

        Sprint s = new Sprint();
        s.setProject(p);
        s.setName("S1");
        s = sprintRepo.save(s);

        UserStory us1 = new UserStory(); 
        us1.setSprint(s); 
        us1.setStatus(UserStoryStatus.DONE); 
        us1.setPriority(UserStoryPriority.HIGH); 
        // CORREÇÃO: setName em vez de setTitle
        us1.setName("T1"); 
        
        UserStory us2 = new UserStory(); 
        us2.setSprint(s); 
        // CORREÇÃO: UserStoryStatus.TODO (sem underscore ou conforme o teu Enum)
        us2.setStatus(UserStoryStatus.TODO); 
        us2.setPriority(UserStoryPriority.LOW); 
        us2.setName("T2");
        
        userStoryRepo.saveAll(List.of(us1, us2));

        User po = createUser("PO User", "po@upt.pt", "STUDENT");
        Team t = new Team();
        t.setName("Equipa X");
        t.setCourse(savedCourse);
        t.setProject(p);
        t.setProductOwner(po);
        t = teamRepo.save(t);

        entityManager.flush();
        entityManager.clear();

        ProjectDetailsDTO details = dashboardService.getProjectDetails(p.getId());

        assertNotNull(details);
        assertEquals("Super Projeto", details.getName());
        assertEquals("Equipa X", details.getTeamName());
        
        // 1 DONE, 1 TODO = 50%
        assertEquals(2, details.getTotalStories());
        assertEquals(1, details.getCompletedStories());
        assertEquals(50, details.getProgressPercentage());

        assertFalse(details.getMembers().isEmpty());
        // CORREÇÃO: Agora getName() e getRoleInTeam() existem no DTO
        assertEquals("PO User", details.getMembers().get(0).getName());
        assertEquals("Product Owner", details.getMembers().get(0).getRoleInTeam());
    }

    // --- Helpers ---
    private User createUser(String name, String email, String role) {
        if (userRepo.findByEmail(email).isPresent()) return userRepo.findByEmail(email).get();
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword("pass");
        u.setRole(role);
        return userRepo.save(u);
    }

    private void createEnrollment(User u, Course c) {
        Enrollment e = new Enrollment();
        e.setStudent(u);
        e.setCourse(c);
        enrollmentRepo.save(e);
    }
}