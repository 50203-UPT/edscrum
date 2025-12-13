package pt.up.edscrum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.up.edscrum.model.Notification;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Buscar todas as notificações de um user, ordenadas por data (mais recente primeiro)
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Opcional: Buscar apenas não lidas
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);
}