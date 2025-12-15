package pt.up.edscrum.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.up.edscrum.enums.NotificationType;
import pt.up.edscrum.model.Notification;
import pt.up.edscrum.model.User;
import pt.up.edscrum.repository.NotificationRepository;
import java.util.List;

@Service
/**
 * Serviço para criação e gestão de notificações.
 * Gera notificações automáticas de acordo com as preferências do utilizador.
 */
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Cria uma notificação automática verificando as preferências do utilizador.
     */
    public void createNotification(User user, NotificationType type, String title, String message) {
        if (user == null) return;

        
        boolean shouldNotify = checkUserPreference(user, type);

        if (shouldNotify) {
            Notification notification = new Notification(user, type, title, message);
            notificationRepository.save(notification);
        }
    }

    private boolean checkUserPreference(User user, NotificationType type) {
        switch (type) {
            case AWARD:
                return user.isNotificationAwards();
            case RANKING:
            case SYSTEM:
                return user.isNotificationRankings();
            case TEAM:
            case SPRINT:
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