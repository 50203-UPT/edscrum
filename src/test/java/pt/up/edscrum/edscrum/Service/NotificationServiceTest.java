package pt.up.edscrum.edscrum.Service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pt.up.edscrum.enums.NotificationType;
import pt.up.edscrum.model.Notification;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.*;
import pt.up.edscrum.service.NotificationService;

@SpringBootTest
@Transactional
class NotificationServiceTest {

    @Autowired private NotificationService notificationService;
    @Autowired private EntityManager entityManager;

    @Autowired private NotificationRepository notificationRepo;
    @Autowired private UserRepository userRepo;
    
    // Repositórios extra para limpeza profunda (evitar FK constraints)
    @Autowired private TeamRepository teamRepo;
    @Autowired private ProjectRepository projectRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private AwardRepository awardRepo;
    @Autowired private StudentAwardRepository studentAwardRepo;
    @Autowired private TeamAwardRepository teamAwardRepo;
    @Autowired private ScoreRepository scoreRepo;
    @Autowired private SprintRepository sprintRepo;
    @Autowired private UserStoryRepository userStoryRepo;

    private User user;

    @BeforeEach
    void setUp() {
        // 1. Limpeza Segura
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

        // 2. Criar utilizador base
        user = new User();
        user.setName("Test User");
        user.setEmail("notify@test.com");
        user.setPassword("123");
        user.setRole("STUDENT");
        // Definições por omissão (true) para facilitar testes básicos
        user.setNotificationAwards(true);
        user.setNotificationRankings(true);
        user = userRepo.save(user);

        entityManager.flush();
        entityManager.clear();
    }

    // ===========================================
    // TESTES: CREATE & PREFERENCES (Lógica de Switch)
    // ===========================================

    @Test
    void testCreateNotification_UserNull() {
        // Garante que não rebenta se o user for null
        notificationService.createNotification(null, NotificationType.SYSTEM, "Titulo", "Msg");
        assertEquals(0, notificationRepo.count());
    }

    @Test
    void testCreateNotification_Award_Enabled() {
        // Configurar user para aceitar notificações de Award
        updateUserPreferences(true, false); // Awards=True

        notificationService.createNotification(user, NotificationType.AWARD, "Award", "You won!");

        assertEquals(1, notificationRepo.count());
    }

    @Test
    void testCreateNotification_Award_Disabled() {
        // Configurar user para RECUSAR notificações de Award
        updateUserPreferences(false, true); // Awards=False

        notificationService.createNotification(user, NotificationType.AWARD, "Award", "Ignored");

        assertEquals(0, notificationRepo.count());
    }

    @Test
    void testCreateNotification_Ranking_Enabled() {
        updateUserPreferences(false, true); // Rankings=True

        notificationService.createNotification(user, NotificationType.RANKING, "Rank", "Top 5");

        assertEquals(1, notificationRepo.count());
    }

    @Test
    void testCreateNotification_Ranking_Disabled() {
        updateUserPreferences(true, false); // Rankings=False

        notificationService.createNotification(user, NotificationType.RANKING, "Rank", "Ignored");

        assertEquals(0, notificationRepo.count());
    }

    @Test
    void testCreateNotification_System_UsesRankingPref_Enabled() {
        // O código usa a flag 'isNotificationRankings' para SYSTEM e RANKING
        updateUserPreferences(false, true); 

        notificationService.createNotification(user, NotificationType.SYSTEM, "Sys", "Update");

        assertEquals(1, notificationRepo.count());
    }
    
    @Test
    void testCreateNotification_System_UsesRankingPref_Disabled() {
        updateUserPreferences(false, false); 

        notificationService.createNotification(user, NotificationType.SYSTEM, "Sys", "Ignored");

        assertEquals(0, notificationRepo.count());
    }

    @Test
    void testCreateNotification_AlwaysTrueTypes() {
        // TEAM e SPRINT devem ser criados independentemente das preferências (conforme a lógica do switch)
        updateUserPreferences(false, false); // Tudo desligado

        notificationService.createNotification(user, NotificationType.TEAM, "Team", "Joined");
        notificationService.createNotification(user, NotificationType.SPRINT, "Sprint", "Started");

        assertEquals(2, notificationRepo.count());
    }

    // ===========================================
    // TESTES: RETRIEVAL (Getters)
    // ===========================================

    @Test
    void testGetUserNotifications() {
        createNotif(user, "N1", false);
        createNotif(user, "N2", false);

        List<Notification> list = notificationService.getUserNotifications(user.getId());
        assertEquals(2, list.size());
        // Verifica ordenação (OrderByCreatedAtDesc - N2 deve vir primeiro se foi criado depois)
        // Como o save é muito rápido, podem ter o mesmo timestamp, mas a lista existe.
    }

    @Test
    void testGetNotificationById() {
        Notification n = createNotif(user, "FindMe", false);
        
        Optional<Notification> found = notificationService.getNotificationById(n.getId());
        assertTrue(found.isPresent());
        assertEquals("FindMe", found.get().getTitle());
    }

    // ===========================================
    // TESTES: LÓGICA DE LEITURA (Mark As Read)
    // ===========================================

    @Test
    void testMarkAsRead_Success() {
        Notification n = createNotif(user, "Unread", false);
        
        notificationService.markAsRead(n.getId());
        
        Notification updated = notificationRepo.findById(n.getId()).get();
        assertTrue(updated.isRead());
    }

    @Test
    void testMarkAsRead_NotFound_Safe() {
        // Garante que não lança exceção se o ID não existir
        assertDoesNotThrow(() -> notificationService.markAsRead(9999L));
    }

    @Test
    void testMarkAllAsRead() {
        // Criar 2 não lidas e 1 já lida
        createNotif(user, "U1", false);
        createNotif(user, "U2", false);
        createNotif(user, "R1", true);

        // Ação
        notificationService.markAllAsRead(user.getId());

        // Verificação
        List<Notification> all = notificationRepo.findAll();
        assertEquals(3, all.size());
        assertTrue(all.stream().allMatch(Notification::isRead), "Todas as notificações devem estar como lidas");
    }

    // --- Helpers ---

    private Notification createNotif(User u, String title, boolean read) {
        Notification n = new Notification(u, NotificationType.SYSTEM, title, "Body");
        n.setRead(read);
        return notificationRepo.save(n);
    }

    private void updateUserPreferences(boolean awards, boolean rankings) {
        // Reload user to ensure clean state
        User u = userRepo.findById(user.getId()).get();
        u.setNotificationAwards(awards);
        u.setNotificationRankings(rankings);
        user = userRepo.save(u);
        entityManager.flush(); // Commit DB
        entityManager.clear(); // Clear cache
        // Atualizar referência local
        user = u;
    }
}