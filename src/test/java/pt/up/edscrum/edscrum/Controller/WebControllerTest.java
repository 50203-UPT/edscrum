package pt.up.edscrum.edscrum.Controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jakarta.persistence.EntityManager;
import pt.up.edscrum.controller.WebController;
import pt.up.edscrum.enums.ProjectStatus;
import pt.up.edscrum.model.*;
import pt.up.edscrum.repository.*;

@SpringBootTest
@Transactional
class WebControllerTest {

    @Autowired private WebController webController;
    @Autowired private EntityManager entityManager;

    @Autowired private UserRepository userRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private SprintRepository sprintRepo;
    @Autowired private AwardRepository awardRepo;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private UserStoryRepository userStoryRepo;

    private MockHttpSession session;
    private Model model;
    private RedirectAttributesModelMap redirectAttributes;

    private User teacher;
    private User student;
    private Course course;
    private Project project;
    private Team team;

    @BeforeEach
    void setUp() {
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

        teacher = new User();
        teacher.setName("Prof Test");
        teacher.setEmail("prof@upt.pt");
        teacher.setPassword("123");
        teacher.setRole("TEACHER");
        teacher = userRepo.save(teacher);

        student = new User();
        student.setName("Aluno Test");
        student.setEmail("aluno@upt.pt");
        student.setPassword("123");
        student.setRole("STUDENT");
        student = userRepo.save(student);

        course = new Course();
        course.setName("Engenharia SW");
        course.setTeacher(teacher);
        course = courseRepo.save(course);

        project = new Project();
        project.setName("Projeto Teste");
        project.setCourse(course);
        project.setStatus(ProjectStatus.EM_CURSO);
        project = projectRepo.save(project);

        team = new Team();
        team.setName("Equipa Alpha");
        team.setCourse(course);
        team.setProject(project);
        team.setScrumMaster(student);
        team = teamRepo.save(team);

        session = new MockHttpSession();
        model = new ConcurrentModel();
        redirectAttributes = new RedirectAttributesModelMap();

        entityManager.flush();
        entityManager.clear();
    }

    // ===========================================
    // 1. PUBLIC PAGES & AUTH
    // ===========================================

    @Test
    void testLoginPage() {
        assertEquals("index", webController.loginPage(model));
    }

    @Test
    void testRegisterPage() {
        assertEquals("register", webController.registerPage());
    }

    @Test
    void testLogout() {
        assertEquals("redirect:/", webController.logout());
    }

    @Test
    void testWebLogin_Success_Teacher() {
        String view = webController.webLogin("prof@upt.pt", "123", model, session);
        assertEquals("redirect:/view/teacher/home", view);
        assertEquals(teacher.getId(), session.getAttribute("currentUserId"));
    }

    @Test
    void testWebLogin_Success_Student() {
        String view = webController.webLogin("aluno@upt.pt", "123", model, session);
        assertEquals("redirect:/view/student/home", view);
    }

    @Test
    void testWebLogin_Failure() {
        String view = webController.webLogin("errado@upt.pt", "errado", model, session);
        assertEquals("index", view);
        assertTrue(model.containsAttribute("error"));
    }

    @Test
    void testWebRegister_Success() {
        User u = new User(); u.setName("Novo"); u.setEmail("novo@upt.pt"); u.setPassword("123"); u.setRole("STUDENT");
        String view = webController.webRegister(u, model);
        assertEquals("redirect:/?registered=true", view);
        assertTrue(userRepo.findByEmail("novo@upt.pt").isPresent());
    }

    @Test
    void testWebRegister_Fail_EmailExists() {
        User u = new User(); u.setName("Duplicado"); u.setEmail("aluno@upt.pt");
        String view = webController.webRegister(u, model);
        assertEquals("register", view);
        assertTrue(model.containsAttribute("error"));
    }

    // ===========================================
    // 2. PASSWORD RECOVERY
    // ===========================================

    @Test
    void testForgotPasswordPage() {
        String view = webController.forgotPasswordPage(null, null, model);
        assertEquals("forgotPassword", view);
        view = webController.forgotPasswordPage("true", "email@test.com", model);
        assertTrue(model.containsAttribute("success"));
    }

    @Test
    void testHandleForgotPassword() {
        String view = webController.handleForgotPassword("aluno@upt.pt", model);
        assertTrue(view.contains("success=true"));
        view = webController.handleForgotPassword("naoexiste@upt.pt", model);
        assertEquals("forgotPassword", view);
        assertTrue(model.containsAttribute("error"));
    }
    
    @Test
    void testShowForgotPasswordPage() {
        assertEquals("forgotPassword", webController.showForgotPasswordPage());
    }

    @Test
    void testProcessSendCode() {
        String view = webController.processSendCode("aluno@upt.pt", redirectAttributes);
        assertEquals("redirect:/verify-code", view);
        
        view = webController.processSendCode("nada@nada.com", redirectAttributes);
        assertEquals("redirect:/forgot-password", view);
    }

    @Test
    void testVerifyCodePage() {
        assertEquals("redirect:/forgot-password", webController.showVerifyCodePage(model));
        model.addAttribute("email", "test@test.com");
        assertEquals("verifyCode", webController.showVerifyCodePage(model));
    }

    @Test
    void testProcessVerifyCode() {
        // CORREÇÃO DO ERRO 2: Verificar FlashAttributes e não o objeto raiz
        String view = webController.processVerifyCode("aluno@upt.pt", "CODIGO_ERRADO", redirectAttributes);
        assertEquals("redirect:/verify-code", view);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("errorMessage"));
    }

    @Test
    void testShowResetPasswordPage() {
        assertEquals("redirect:/forgot-password", webController.showResetPasswordPage(model));
    }

    @Test
    void testProcessChangePassword() {
        String view = webController.processChangePassword("aluno@upt.pt", "novapass", redirectAttributes);
        assertEquals("redirect:/", view);
        User u = userRepo.findByEmail("aluno@upt.pt").get();
        assertEquals("novapass", u.getPassword());
    }

    // ===========================================
    // 3. TEACHER ACTIONS
    // ===========================================

    @Test
    void testTeacherHome() {
        session.setAttribute("currentUserId", teacher.getId());
        session.setAttribute("currentUserRole", "TEACHER");
        String view = webController.teacherHome(model, session);
        assertEquals("teacherHome", view);
        assertTrue(model.containsAttribute("courses"));
    }

    @Test
    void testCreateCourseWeb() {
        session.setAttribute("currentUserId", teacher.getId());
        session.setAttribute("currentUserRole", "TEACHER");
        Course c = new Course(); c.setName("New Course Web");
        
        String view = webController.createCourseWeb(c, teacher.getId(), redirectAttributes, session);
        assertEquals("redirect:/view/teacher/home", view);
        assertTrue(courseRepo.findAll().stream().anyMatch(co -> co.getName().equals("New Course Web")));
    }

    @Test
    void testCreateProjectWeb() {
        session.setAttribute("currentUserId", teacher.getId());
        session.setAttribute("currentUserRole", "TEACHER");
        Project p = new Project(); p.setName("New Project Web");

        String view = webController.createProjectWeb(p, course.getId(), teacher.getId(), team.getId(), redirectAttributes, session);
        assertEquals("redirect:/view/teacher/home", view);
        
        List<Project> projects = projectRepo.findAll();
        assertTrue(projects.stream().anyMatch(pr -> pr.getName().equals("New Project Web")));
    }

    @Test
    void testDeleteProjectWeb() {
        Project p = new Project(); p.setName("Del"); p.setCourse(course); p = projectRepo.save(p);
        String view = webController.deleteProjectWeb(p.getId(), teacher.getId(), redirectAttributes);
        assertEquals("redirect:/view/teacher/home", view);
        assertFalse(projectRepo.existsById(p.getId()));
    }

    @Test
    void testCreateTeamWeb() {
        // Setup Dados
        User fresh = new User(); fresh.setName("Fresh"); fresh.setRole("STUDENT"); fresh.setEmail("fr@pt"); fresh.setPassword("1");
        fresh = userRepo.save(fresh);
        
        Enrollment en = new Enrollment(); en.setStudent(fresh); en.setCourse(course);
        enrollmentRepo.save(en);

        // CORREÇÃO DO ERRO 1: Flush antes de chamar o controller para garantir que a DB está consistente
        entityManager.flush();
        entityManager.clear();

        List<Long> devs = List.of(fresh.getId());

        String view = webController.createTeamWeb(
            "Team Web", 
            course.getId(), 
            teacher.getId(), 
            null, 
            project.getId(), 
            fresh.getId(), 
            fresh.getId(), 
            devs,          
            redirectAttributes
        );

        assertEquals("redirect:/view/teacher/home?tab=teams", view);
        
        // CORREÇÃO DO ERRO 1 (Continuação): Flush e Clear para garantir que lemos os novos prémios da BD
        entityManager.flush();
        entityManager.clear();

        boolean hasAward = studentAwardRepo.findAllByStudentId(fresh.getId()).stream()
                .anyMatch(sa -> sa.getAward().getName().contains("Arquiteto"));
        assertTrue(hasAward, "Prémio não foi atribuído");
    }

    @Test
    void testTeacherProfileUpdate() {
        session.setAttribute("currentUserId", teacher.getId());
        session.setAttribute("currentUserRole", "TEACHER");
        
        MockMultipartFile file = new MockMultipartFile("imageFile", "test.jpg", "image/jpeg", "content".getBytes());
        String view = webController.updateTeacherProfile(teacher.getId(), "Prof Update", "prof@upt.pt", "123", "456", "456", file, null, redirectAttributes, session);
        
        assertEquals("redirect:/view/teacher/home", view);
        User u = userRepo.findById(teacher.getId()).get();
        assertEquals("Prof Update", u.getName());
    }

    // ===========================================
    // 4. STUDENT ACTIONS & API
    // ===========================================

    @Test
    void testStudentHome() {
        session.setAttribute("currentUserId", student.getId());
        session.setAttribute("currentUserRole", "STUDENT");
        String view = webController.studentHome(model, session);
        assertEquals("studentHome", view);
    }

    @Test
    void testEnrollStudent_Ajax() {
        session.setAttribute("currentUserId", student.getId());
        session.setAttribute("currentUserRole", "STUDENT");
        Course c2 = new Course(); c2.setName("Ajax Course"); c2 = courseRepo.save(c2);

        Object response = webController.enrollStudent(student.getId(), c2.getId(), null, "XMLHttpRequest", redirectAttributes, session);
        assertTrue(response instanceof org.springframework.http.ResponseEntity);
        assertEquals(200, ((org.springframework.http.ResponseEntity<?>) response).getStatusCode().value());
        assertTrue(enrollmentRepo.existsByStudentIdAndCourseId(student.getId(), c2.getId()));
    }

    @Test
    void testEnrollStudent_Standard() {
        session.setAttribute("currentUserId", student.getId());
        session.setAttribute("currentUserRole", "STUDENT");
        Course c3 = new Course(); c3.setName("Std Course"); c3 = courseRepo.save(c3);

        Object response = webController.enrollStudent(student.getId(), c3.getId(), null, null, redirectAttributes, session);
        assertEquals("redirect:/view/student/home?tab=all-courses", response);
    }

    // ===========================================
    // 5. SPRINT & MISC
    // ===========================================

    @Test
    void testCreateSprintWeb() {
        session.setAttribute("currentUserId", student.getId());
        session.setAttribute("currentUserRole", "STUDENT");
        Sprint s = new Sprint(); s.setName("Sprint Web Test");

        String view = webController.createSprintWeb(project.getId(), student.getId(), s, redirectAttributes, session);
        assertTrue(view.startsWith("redirect:/view/sprint/"));
        
        List<Sprint> sprints = sprintRepo.findByProjectId(project.getId());
        assertTrue(sprints.stream().anyMatch(sp -> sp.getName().equals("Sprint Web Test")));
    }

    @Test
    void testSprintDashboard() {
        session.setAttribute("currentUserId", student.getId());
        session.setAttribute("currentUserRole", "STUDENT");

        Sprint s = new Sprint(); 
        s.setProject(project); 
        s.setName("S1"); 
        // CORREÇÃO DO ERRO 3: Inicializar a lista de user stories para evitar NullPointerException
        s.setUserStories(new ArrayList<>()); 
        s = sprintRepo.save(s);

        String view = webController.sprintDashboard(s.getId(), model, session);
        assertEquals("sprintDashboard", view);
        assertTrue(model.containsAttribute("sprintProgress"));
    }

    @Test
    void testAssignAwardAction() {
        session.setAttribute("currentUserId", teacher.getId());
        session.setAttribute("currentUserRole", "TEACHER");

        Award a = new Award(); a.setName("M"); a.setPoints(10); a.setTargetType("INDIVIDUAL"); a.setType("MANUAL");
        final Award savedAward = awardRepo.save(a);

        String view = webController.assignAwardAction(course.getId(), savedAward.getId(), student.getId(), session);
        assertTrue(view.contains("/view/teacher/course/" + course.getId()));

        boolean has = studentAwardRepo.findAllByStudentId(student.getId()).stream()
                .anyMatch(sa -> sa.getAward().getId().equals(savedAward.getId()));
        assertTrue(has);
    }
    
    @Test
    void testDeleteTeamWeb() {
        String view = webController.deleteTeamWeb(team.getId(), teacher.getId(), redirectAttributes);
        assertEquals("redirect:/view/teacher/home?tab=teams", view);
        assertFalse(teamRepo.existsById(team.getId()));
    }
    
    @Test
    void testApiRemoveTeamMember() {
        // CORREÇÃO DO ERRO 4: O controller remove o único membro (SM) -> Equipa fica vazia -> TeamService apaga equipa.
        // Logo, testamos se a equipa foi apagada com sucesso.
        
        session.setAttribute("currentUserId", teacher.getId());
        session.setAttribute("currentUserRole", "TEACHER");
        
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("studentId", student.getId());
        
        var response = webController.removeTeamMemberApi(team.getId(), payload, session);
        assertEquals(200, response.getStatusCode().value());
        
        // Se a equipa ficou vazia, deve ter sido apagada automaticamente
        assertFalse(teamRepo.existsById(team.getId()), "A equipa deveria ter sido eliminada automaticamente por ficar vazia.");
    }
    
    @Test
    void testUpdateStudentSettings() {
        session.setAttribute("currentUserId", student.getId());
        session.setAttribute("currentUserRole", "STUDENT");
        
        String view = webController.updateStudentSettings(student.getId(), "New Name", true, true, session);
        assertEquals("redirect:/view/student/home", view);
        
        User u = userRepo.findById(student.getId()).get();
        assertEquals("New Name", u.getName());
    }
}