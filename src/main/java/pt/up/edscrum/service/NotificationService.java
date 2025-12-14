package pt.up.edscrum.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.up.edscrum.enums.NotificationType;
import pt.up.edscrum.model.Notification;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.NotificationRepository;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Cria uma notificação automática verificando as preferências do utilizador.
     */
    public void createNotification(User user, NotificationType type, String title, String message) {
        if (user == null) return;

        // 1. Verificar Preferências (Assumindo campos existentes na classe User baseados na UI)
        boolean shouldNotify = checkUserPreference(user, type);

        if (shouldNotify) {
            Notification notification = new Notification(user, type, title, message);
            notificationRepository.save(notification);
        }
    }

    private boolean checkUserPreference(User user, NotificationType type) {
        // Lógica de mapeamento entre Tipo de Notificação e Preferência do User
        switch (type) {
            case AWARD:
                // Verifica se o user quer notificações de prémios
                return user.isNotificationAwards(); 
            case RANKING:
            case SYSTEM:
                // Verifica se o user quer notificações de rankings/sistema
                return user.isNotificationRankings(); 
            case TEAM:
            case SPRINT:
                // Assumimos que notificações de Equipa/Sprint são críticas ou usam uma flag geral
                // Se não houver flag específica, retornamos true ou usamos uma flag geral "notificationsEnabled"
                return true; 
            default:
                return true;
        }
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public java.util.Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
    
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}