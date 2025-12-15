package pt.up.edscrum.edscrum.Service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.model.*;
import pt.up.edscrum.repository.*;
import pt.up.edscrum.service.AwardService;

@SpringBootTest
@Transactional
class AwardServiceTest {

    @Autowired private AwardService awardService;
    @Autowired private EntityManager entityManager;

    @Autowired private AwardRepository awardRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private SprintRepository sprintRepo;

    private User student;
    private Project project;
    private Team team;
    private Award awardIndiv;
    private Award awardTeam;

    @BeforeEach
    void setUp() {
        // Limpeza Profunda
        notificationRepo.deleteAll();
        teamAwardRepo.deleteAll();
        studentAwardRepo.deleteAll();
        scoreRepo.deleteAll();
        sprintRepo.deleteAll();
        teamRepo.deleteAll();
        projectRepo.deleteAll();
        enrollmentRepo.deleteAll();
        awardRepo.deleteAll();
        courseRepo.deleteAll();
        userRepo.deleteAll();

        // 1. Criar "Alunos Fantasma" para ocupar o Top 5 (Bloqueio de Ranking Automático para testes base)
        for (int i = 0; i < 5; i++) {
            User dummy = new User();
            dummy.setName("Dummy " + i);
            dummy.setEmail("dummy" + i + "@test.com");
            dummy.setRole("STUDENT");
            userRepo.save(dummy);

            Score dummyScore = new Score();
            dummyScore.setUser(dummy);
            dummyScore.setTotalPoints(10000 + (i * 10)); 
            scoreRepo.save(dummyScore);
        }

        // 2. Setup Base
        Course course = new Course();
        course.setName("Engenharia SW");
        course = courseRepo.save(course);

        project = new Project();
        project.setName("Projeto Teste");
        project.setCourse(course);
        project = projectRepo.save(project);

        student = new User();
        student.setName("Aluno Teste");
        student.setEmail("aluno@test.com");
        student.setRole("STUDENT");
        student = userRepo.save(student);

        team = new Team();
        team.setName("Equipa Alpha");
        team.setCourse(course);
        team.setProject(project);
        team.setDevelopers(List.of(student));
        team = teamRepo.save(team);

        awardIndiv = new Award();
        awardIndiv.setName("Prémio Individual");
        awardIndiv.setPoints(100);
        awardIndiv.setTargetType("INDIVIDUAL");
        awardIndiv.setType("MANUAL");
        awardIndiv = awardRepo.save(awardIndiv);

        awardTeam = new Award();
        awardTeam.setName("Prémio Equipa");
        awardTeam.setPoints(50);
        awardTeam.setTargetType("TEAM");
        awardTeam.setType("MANUAL");
        awardTeam = awardRepo.save(awardTeam);

        entityManager.flush();
        entityManager.clear();
    }

    // ===========================================
    // TESTES BÁSICOS (MANUAIS)
    // ===========================================

    @Test
    void testCreateAndGetAward() {
        Award newAward = new Award();
        newAward.setName("Novo Prémio");
        newAward.setPoints(10);
        newAward.setType("MANUAL");
        newAward.setTargetType("INDIVIDUAL");
        
        Award created = awardService.createAward(newAward);
        assertNotNull(created.getId());
        
        // Testar getAllAwards
        List<Award> all = awardService.getAllAwards();
        assertTrue(all.size() >= 3); // 2 do setup + 1 novo

        Award found = awardService.getAwardById(created.getId());
        assertEquals("Novo Prémio", found.getName());
    }

    @Test
    void testAssignAwardToStudent_AndUpdateScore() {
        awardService.assignAwardToStudent(awardIndiv.getId(), student.getId(), project.getId());

        int totalPoints = awardService.calculateTotalPoints(student.getId());
        assertEquals(100, totalPoints);

        Score score = scoreRepo.findByUser(student);
        assertNotNull(score);
        assertEquals(100, score.getTotalPoints());
    }
    
    @Test
    void testAssignAwardToStudent_WithoutProject() {
        awardService.assignAwardToStudent(awardIndiv.getId(), student.getId());

        int totalPoints = awardService.calculateTotalPoints(student.getId());
        assertEquals(100, totalPoints);
        
        // Verificar que não está associado a projeto
        List<StudentAward> awards = studentAwardRepo.findAllByStudentId(student.getId());
        assertEquals(1, awards.size());
        assertNull(awards.get(0).getProject());
    }

    @Test
    void testAssignAwardToStudent_AlreadyAssigned_ThrowsException() {
        awardService.assignAwardToStudent(awardIndiv.getId(), student.getId(), project.getId());

        assertThrows(RuntimeException.class, () -> {
            awardService.assignAwardToStudent(awardIndiv.getId(), student.getId(), project.getId());
        });
    }

    @Test
    void testAssignAwardToTeam_AndUpdateMemberScores() {
        awardService.assignAwardToTeam(awardTeam.getId(), team.getId(), project.getId());

        int totalPoints = awardService.calculateTotalPoints(student.getId());
        assertEquals(50, totalPoints);

        Score score = scoreRepo.findByUser(student);
        assertEquals(50, score.getTotalPoints());
    }
    
    @Test
    void testAssignAwardToTeam_WithoutProject() {
        awardService.assignAwardToTeam(awardTeam.getId(), team.getId());
        
        int totalPoints = awardService.calculateTotalPoints(student.getId());
        assertEquals(50, totalPoints);
    }

    @Test
    void testAssignAwardToTeam_AlreadyAssigned_ThrowsException() {
        awardService.assignAwardToTeam(awardTeam.getId(), team.getId(), project.getId());

        assertThrows(RuntimeException.class, () -> {
            awardService.assignAwardToTeam(awardTeam.getId(), team.getId(), project.getId());
        });
    }

    // ===========================================
    // TESTES DE RANKING AUTOMÁTICO
    // ===========================================

    @Test
    void testRankingAwards_Top5_And_Top3() {
        // 1. Limpar scores dos Dummies para permitir que o nosso aluno suba
        scoreRepo.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // 2. Dar muitos pontos ao aluno para ser Rank #1
        // Atribuir 200 pontos (Manual)
        Award bigAward = new Award(); 
        bigAward.setName("Big Win"); 
        bigAward.setPoints(200); 
        bigAward.setTargetType("INDIVIDUAL");
        bigAward.setType("MANUAL");
        bigAward = awardRepo.save(bigAward);

        awardService.assignAwardToStudent(bigAward.getId(), student.getId(), project.getId());

        // 3. Verificar se ganhou prémios automáticos
        // Esperado: 200 (Big Win) + 50 (Top 5) + 120 (Top 3) = 370 pontos
        int total = awardService.calculateTotalPoints(student.getId());
        assertEquals(370, total, "Deve incluir bónus de Top 5 e Top 3");

        // Verificar persistência dos prémios automáticos
        assertTrue(studentAwardRepo.findAllByStudentId(student.getId()).stream()
                .anyMatch(sa -> sa.getAward().getName().contains("Top 5")));
        assertTrue(studentAwardRepo.findAllByStudentId(student.getId()).stream()
                .anyMatch(sa -> sa.getAward().getName().contains("Top 3")));
    }

    // ===========================================
    // TESTES DE MILESTONES DE SPRINT
    // ===========================================

    @Test
    void testHandleSprintCreated_Milestones() {
        // 1. Criar 1º Sprint
        createSprintForUser(student);
        awardService.handleSprintCreated(student.getId(), project.getId());
        
        assertTrue(hasAward(student, "Primeiro Salto"), "Deve ganhar prémio de 1º sprint");

        // 2. Criar até 5 Sprints
        for(int i=0; i<4; i++) createSprintForUser(student);
        awardService.handleSprintCreated(student.getId(), null);
        
        assertTrue(hasAward(student, "Sprint Artisan"), "Deve ganhar prémio de 5 sprints");

        // 3. Criar até 10 Sprints
        for(int i=0; i<5; i++) createSprintForUser(student);
        awardService.handleSprintCreated(student.getId(), null);
        
        assertTrue(hasAward(student, "Sprint Veteran"), "Deve ganhar prémio de 10 sprints");
    }
    
    @Test
    void testHandleSprintCreated_NullUser() {
        assertDoesNotThrow(() -> awardService.handleSprintCreated(null, null));
    }

    // ===========================================
    // TESTES DE PROFESSOR (BLOCKING)
    // ===========================================

    @Test
    void testTeacherAsPO_DoesNotGetAwards() {
        User teacher = new User();
        teacher.setName("Prof PO");
        teacher.setRole("TEACHER");
        teacher.setEmail("p@upt.pt");
        teacher = userRepo.save(teacher);

        // Colocar professor como PO da equipa
        team.setProductOwner(teacher);
        teamRepo.save(team);
        
        entityManager.flush();
        entityManager.clear();

        // 1. Tentar atribuir prémio individual (Deve falhar com exceção)
        final Long tId = teacher.getId();
        final Long awId = awardIndiv.getId();
        
        // Teste assignAwardToStudent (sem projeto)
        assertThrows(RuntimeException.class, () -> {
            awardService.assignAwardToStudent(awId, tId);
        });
        
        // 2. Tentar atribuir prémio automático (Deve ser ignorado silenciosamente)
        awardService.assignAutomaticAwardToStudentByName("Auto Award", "Desc", 10, tId, null);
        assertEquals(0, studentAwardRepo.findAllByStudentId(tId).size());
    }

    // ===========================================
    // TESTES AUXILIARES (GET AVAILABLE, DELETE, UPDATE)
    // ===========================================

    @Test
    void testGetAvailableAwards() {
        // Atribuir 1 prémio individual
        awardService.assignAwardToStudent(awardIndiv.getId(), student.getId(), project.getId());
        
        List<Award> available = awardService.getAvailableAwardsForStudent(student.getId(), project.getId());
        assertFalse(available.stream().anyMatch(a -> a.getId().equals(awardIndiv.getId())));
        
        // Atribuir 1 prémio equipa
        awardService.assignAwardToTeam(awardTeam.getId(), team.getId(), project.getId());
        
        List<Award> teamAvail = awardService.getAvailableAwardsForTeam(team.getId(), project.getId());
        assertFalse(teamAvail.stream().anyMatch(a -> a.getId().equals(awardTeam.getId())));
    }

    @Test
    void testUpdateAward() {
        Award updateDetails = new Award();
        updateDetails.setName("Nome Atualizado");
        updateDetails.setDescription("Desc");
        updateDetails.setPoints(200);
        updateDetails.setType("MANUAL");
        updateDetails.setTargetType("INDIVIDUAL");
        
        Award updated = awardService.updateAward(awardIndiv.getId(), updateDetails);
        assertEquals("Nome Atualizado", updated.getName());
        assertEquals(200, updated.getPoints());
    }

    @Test
    void testDeleteAward() {
        Award toDelete = new Award();
        toDelete.setName("Para Apagar");
        toDelete.setPoints(10);
        toDelete.setType("MANUAL");
        toDelete.setTargetType("INDIVIDUAL");
        toDelete = awardRepo.save(toDelete);
        
        Long id = toDelete.getId();
        awardService.deleteAward(id);
        
        assertFalse(awardRepo.existsById(id));
    }
    
    @Test
    void testAssignAutomaticAward_AlreadyExists() {
        // Atribuir a primeira vez
        awardService.assignAutomaticAwardToStudentByName("Auto", "Desc", 10, student.getId(), project.getId());
        int count1 = studentAwardRepo.findAll().size();
        
        // Atribuir segunda vez (deve ignorar)
        awardService.assignAutomaticAwardToStudentByName("Auto", "Desc", 10, student.getId(), project.getId());
        int count2 = studentAwardRepo.findAll().size();
        
        assertEquals(count1, count2);
    }
    
    @Test
    void testAssignAutomaticAwardToTeam_AlreadyExists() {
        awardService.assignAutomaticAwardToTeamByName("TeamAuto", "Desc", 10, team.getId(), project.getId());
        int count1 = teamAwardRepo.findAll().size();
        
        awardService.assignAutomaticAwardToTeamByName("TeamAuto", "Desc", 10, team.getId(), project.getId());
        int count2 = teamAwardRepo.findAll().size();
        
        assertEquals(count1, count2);
    }

    // --- Helpers ---

    private void createSprintForUser(User u) {
        Sprint s = new Sprint();
        s.setCreatedBy(u);
        s.setProject(project);
        s.setName("Sprint");
        sprintRepo.save(s);
    }
    
    private boolean hasAward(User u, String namePart) {
        return studentAwardRepo.findAllByStudentId(u.getId()).stream()
                .anyMatch(sa -> sa.getAward().getName().contains(namePart));
    }
}