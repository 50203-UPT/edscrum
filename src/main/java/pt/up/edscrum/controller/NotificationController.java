package pt.up.edscrum.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    public static class NotificationDTO {

        public String id;
        public String title;
        public String message;
        public String type;
        public Date timestamp;
        public boolean read;
        public String link;

        public NotificationDTO(String id, String title, String message, String type, Date timestamp, boolean read, String link) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.type = type;
            this.timestamp = timestamp;
            this.read = read;
            this.link = link;
        }
    }

    /**
     * Fornece notificações simuladas para um professor (mock para frontend).
     *
     * @return ResponseEntity com lista de NotificationDTO
     */
    @GetMapping("/teacher")
    public ResponseEntity<List<NotificationDTO>> getTeacherNotifications() {
        List<NotificationDTO> notifications = Arrays.asList(
                new NotificationDTO("notif1", "Novo Prémio Atribuído!", "O estudante João Silva recebeu o prémio 'Inovador do Mês'.", "AWARD", new Date(System.currentTimeMillis() - 5 * 60 * 1000), false, "#"),
                new NotificationDTO("notif2", "Sprint Concluída", "A Sprint 2 do projeto 'Plataforma de Gestão' foi concluída.", "PROJECT_COMPLETED", new Date(System.currentTimeMillis() - 3600 * 1000), false, "#")
        );
        return ResponseEntity.ok(notifications);
    }

    /**
     * Marca uma notificação como lida (simulado).
     *
     * @param notificationId ID da notificação
     * @return ResponseEntity OK
     */
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable String notificationId) {
        System.out.println("Notification " + notificationId + " marked as read.");
        return ResponseEntity.ok().build();
    }
}
