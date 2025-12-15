package pt.up.edscrum.edscrum.Service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.model.Course;
import pt.up.edscrum.model.Enrollment;
import pt.up.edscrum.model.Project;
import pt.up.edscrum.model.Team;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.AwardRepository;
import pt.up.edscrum.repository.CourseRepository;
import pt.up.edscrum.repository.EnrollmentRepository;
import pt.up.edscrum.repository.NotificationRepository; // ADICIONADO
import pt.up.edscrum.repository.ProjectRepository;
import pt.up.edscrum.repository.ScoreRepository;
import pt.up.edscrum.repository.SprintRepository;
import pt.up.edscrum.repository.StudentAwardRepository;
import pt.up.edscrum.repository.TeamAwardRepository;
import pt.up.edscrum.repository.TeamRepository;
import pt.up.edscrum.repository.UserRepository;
import pt.up.edscrum.repository.UserStoryRepository;
import pt.up.edscrum.service.CourseService;
import pt.up.edscrum.service.ProjectService;
import pt.up.edscrum.service.TeamService;
import pt.up.edscrum.service.UserService;

@SpringBootTest
@Transactional
class TeamServiceTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

    // Repositories for cleanup
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TeamAwardRepository teamAwardRepository;
    @Autowired
    private StudentAwardRepository studentAwardRepository;
    @Autowired
    private ScoreRepository scoreRepository;
    @Autowired
    private AwardRepository awardRepository;
    @Autowired
    private SprintRepository sprintRepository;
    @Autowired
    private UserStoryRepository userStoryRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private NotificationRepository notificationRepository; // ADICIONADO

    @BeforeEach
    void setUp() {
        // Limpar tabelas na ordem correta para não violar FK
        // Notificações dependem de Users, logo devem ser apagadas antes dos Users
        notificationRepository.deleteAll(); 
        
        userStoryRepository.deleteAll();
        sprintRepository.deleteAll();
        teamAwardRepository.deleteAll();
        studentAwardRepository.deleteAll();
        scoreRepository.deleteAll();
        enrollmentRepository.deleteAll();
        
        // Teams dependem de Project e User
        teamRepository.deleteAll();
        
        // Projects dependem de Course
        projectRepository.deleteAll();
        
        awardRepository.deleteAll();
        
        // Enrollments dependem de Course e User (já apagado acima, mas garantir ordem)
        enrollmentRepository.deleteAll();
        
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    private void createAndSaveEnrollment(User student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.save(enrollment);
    }

    @Test
    void testCreateTeamWithMembers() {
        Course c = new Course();
        c.setName("Engenharia");
        c = courseService.createCourse(c); 

        Project p = new Project();
        p.setName("Projeto 1");
        p.setCourse(c);
        p = projectService.createProject(p); 

        User sm = new User();
        sm.setName("Scrum Master");
        sm.setRole("STUDENT");
        sm.setEmail("sm@test.com"); // Boa prática adicionar email único
        sm = userService.createUser(sm); 
        createAndSaveEnrollment(sm, c); 

        User po = new User();
        po.setName("Product Owner");
        po.setRole("STUDENT");
        po.setEmail("po@test.com");
        po = userService.createUser(po); 
        createAndSaveEnrollment(po, c); 

        User dev1 = new User();
        dev1.setName("Dev 1");
        dev1.setRole("STUDENT");
        dev1.setEmail("dev1@test.com");
        dev1 = userService.createUser(dev1); 
        createAndSaveEnrollment(dev1, c); 

        User dev2 = new User();
        dev2.setName("Dev 2");
        dev2.setRole("STUDENT");
        dev2.setEmail("dev2@test.com");
        dev2 = userService.createUser(dev2); 
        createAndSaveEnrollment(dev2, c); 

        Team team = new Team();
        team.setName("Team Alpha");
        team.setProject(p);
        team.setCourse(c); 
        team.setScrumMaster(sm);
        team.setProductOwner(po);
        team.setDevelopers(List.of(dev1, dev2));

        Team saved = teamService.createTeam(team); 
        assertNotNull(saved.getId());
        assertEquals("Team Alpha", saved.getName());
        assertEquals(2, saved.getDevelopers().size());
        assertNotNull(saved.getCourse());
        assertEquals(c.getId(), saved.getCourse().getId());
        assertNotNull(saved.getProject());
        assertEquals(p.getId(), saved.getProject().getId());
    }

    @Test
    void testUpdateTeam() {
        // Arrange: Create Course and Project for the team
        Course c = new Course();
        c.setName("Course for Update");
        c = courseService.createCourse(c);

        Project p = new Project();
        p.setName("Project for Update");
        p.setCourse(c);
        p = projectService.createProject(p);

        Team t = new Team();
        t.setName("Velha Equipa");
        t.setCourse(c); 
        t.setProject(p); 
        t = teamService.createTeam(t); 

        // Act: Create an update object that preserves existing associations
        Team update = new Team();
        update.setName("Nova Equipa");
        update.setCourse(t.getCourse()); 
        update.setProject(t.getProject()); 

        Team result = teamService.updateTeam(t.getId(), update);

        // Assert
        assertEquals("Nova Equipa", result.getName());
        assertNotNull(result.getCourse());
        assertEquals(c.getName(), result.getCourse().getName());
        assertNotNull(result.getProject());
        assertEquals(p.getName(), result.getProject().getName());
    }
}