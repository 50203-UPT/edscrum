package pt.up.edscrum.edscrum.Service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
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
    @Autowired private AwardRepository awardRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private EnrollmentRepository enrollmentRepo; // ADICIONADO: Necessário para limpar FKs

    // Variáveis de instância para usar nos testes
    private User student;
    private Project project;
    private Team team;
    private Award awardIndiv;
    private Award awardTeam;

    /**
     * Executado ANTES de cada teste individual.
     * Garante que a BD está limpa e cria os dados frescos.
     */
    @BeforeEach
    void setUp() {
        cleanDatabase(); // Garante que não há lixo de testes anteriores
        createScenario(); // Cria os dados necessários
    }

    /**
     * Executado DEPOIS de cada teste.
     */
    @AfterEach
    void tearDown() {
        // Opcional com @Transactional, mas garante limpeza absoluta se a transação falhar
        cleanDatabase(); 
    }

    // Método auxiliar para limpar dados (Ordem inversa das dependências para evitar erros de FK)
    private void cleanDatabase() {
        notificationRepo.deleteAll();
        teamAwardRepo.deleteAll();
        studentAwardRepo.deleteAll();
        scoreRepo.deleteAll();
        
        // Teams dependem de Project e User
        teamRepo.deleteAll();
        
        // Projects dependem de Course
        projectRepo.deleteAll();
        
        awardRepo.deleteAll();
        
        // Enrollments dependem de Course e User (CRÍTICO: Apagar antes de Users)
        enrollmentRepo.deleteAll(); 
        
        userRepo.deleteAll();
        courseRepo.deleteAll();
    }

    // Método auxiliar para criar o cenário de teste
    private void createScenario() {
        // 1. Criar "Alunos Fantasma" para ocupar o Top 5
        // Isto é crucial: o nosso aluno de teste ficará em 6º lugar e NÃO receberá pontos automáticos de ranking
        for (int i = 0; i < 5; i++) {
            User dummy = new User();
            dummy.setName("Dummy " + i);
            dummy.setEmail("dummy" + i + "@test.com");
            dummy.setPassword("pass");
            dummy.setRole("STUDENT");
            userRepo.save(dummy);

            Score dummyScore = new Score();
            dummyScore.setUser(dummy);
            dummyScore.setTotalPoints(10000 + (i * 10)); // Pontuação muito alta para garantir o top
            scoreRepo.save(dummyScore);
        }

        // 2. Criar Curso
        Course course = new Course();
        course.setName("Engenharia SW");
        courseRepo.save(course);

        // 3. Criar Projeto
        project = new Project();
        project.setName("Projeto Teste");
        project.setCourse(course);
        projectRepo.save(project);

        // 4. Criar o nosso Estudante de Teste
        student = new User();
        student.setName("Aluno Teste");
        student.setEmail("aluno@test.com");
        student.setRole("STUDENT");
        userRepo.save(student);

        // 5. Criar Equipa e adicionar o aluno
        team = new Team();
        team.setName("Equipa Alpha");
        team.setCourse(course);
        team.setProject(project);
        team.setDevelopers(List.of(student));
        teamRepo.save(team);

        // 6. Criar Prémios (Manuais)
        awardIndiv = new Award();
        awardIndiv.setName("Prémio Individual");
        awardIndiv.setPoints(100);
        awardIndiv.setTargetType("INDIVIDUAL");
        awardIndiv.setType("MANUAL");
        awardRepo.save(awardIndiv);

        awardTeam = new Award();
        awardTeam.setName("Prémio Equipa");
        awardTeam.setPoints(50);
        awardTeam.setTargetType("TEAM");
        awardTeam.setType("MANUAL");
        awardRepo.save(awardTeam);
    }

    @Test
    void testCreateAndGetAward() {
        Award newAward = new Award();
        newAward.setName("Novo Prémio");
        newAward.setPoints(10);
        newAward.setType("MANUAL");
        newAward.setTargetType("INDIVIDUAL");
        
        Award created = awardService.createAward(newAward);
        assertNotNull(created.getId());
        
        Award found = awardService.getAwardById(created.getId());
        assertEquals("Novo Prémio", found.getName());
    }

    @Test
    void testAssignAwardToStudent_AndUpdateScore() {
        // Ação: Atribuir prémio de 100 pontos
        awardService.assignAwardToStudent(awardIndiv.getId(), student.getId(), project.getId());

        // Verificação
        // Como criámos 5 alunos com 10.000 pontos, o nosso aluno não ganha prémios de ranking.
        // Deve ter exatamente 100 pontos.
        int totalPoints = awardService.calculateTotalPoints(student.getId());
        assertEquals(100, totalPoints, "O aluno devia ter apenas 100 pontos (sem bónus automático)");

        Score score = scoreRepo.findByUser(student);
        assertNotNull(score);
        assertEquals(100, score.getTotalPoints());
    }

    @Test
    void testAssignAwardToStudent_AlreadyAssigned_ThrowsException() {
        // Atribuir a primeira vez
        awardService.assignAwardToStudent(awardIndiv.getId(), student.getId(), project.getId());

        // Tentar atribuir o mesmo prémio novamente deve lançar exceção
        assertThrows(RuntimeException.class, () -> {
            awardService.assignAwardToStudent(awardIndiv.getId(), student.getId(), project.getId());
        });
    }

    @Test
    void testAssignAwardToTeam_AndUpdateMemberScores() {
        // Ação: Atribuir prémio de equipa (50 pontos)
        awardService.assignAwardToTeam(awardTeam.getId(), team.getId(), project.getId());

        // Verificação: O membro da equipa deve receber os pontos
        int totalPoints = awardService.calculateTotalPoints(student.getId());
        assertEquals(50, totalPoints);

        Score score = scoreRepo.findByUser(student);
        assertNotNull(score);
        assertEquals(50, score.getTotalPoints());
    }

    @Test
    void testCombinedPoints_IndividualAndTeam() {
        // 1. Prémio Individual (100 pts)
        awardService.assignAwardToStudent(awardIndiv.getId(), student.getId(), project.getId());
        
        // 2. Prémio de Equipa (50 pts)
        awardService.assignAwardToTeam(awardTeam.getId(), team.getId(), project.getId());

        // Total deve ser 150
        int total = awardService.calculateTotalPoints(student.getId());
        assertEquals(150, total);
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
        awardRepo.save(toDelete);
        
        Long id = toDelete.getId();
        awardService.deleteAward(id);
        
        assertFalse(awardRepo.existsById(id));
    }
}