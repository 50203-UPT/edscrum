package pt.up.edscrum.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.up.edscrum.model.Notification;
import pt.up.edscrum.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
/**
 * Endpoints para consulta e gestão de notificações de utilizadores.
 */
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * Obtém as notificações de um utilizador específico. URL: GET
     * /api/notifications/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable Long userId, jakarta.servlet.http.HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        if (!currentUserId.equals(userId) && !"TEACHER".equals(currentUserRole)) return ResponseEntity.status(403).build();
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Marca uma notificação específica como lida. URL: POST
     * /api/notifications/{id}/read
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, jakarta.servlet.http.HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        java.util.Optional<Notification> opt = notificationService.getNotificationById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Notification n = opt.get();
        if (!n.getUser().getId().equals(currentUserId) && !"TEACHER".equals(currentUserRole)) return ResponseEntity.status(403).build();
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Marca todas as notificações de um utilizador como lidas. URL: POST
     * /api/notifications/user/{userId}/read-all
     */
    @PostMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Long userId, jakarta.servlet.http.HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("currentUserId");
        String currentUserRole = (String) session.getAttribute("currentUserRole");
        if (currentUserId == null) return ResponseEntity.status(401).build();
        if (!currentUserId.equals(userId) && !"TEACHER".equals(currentUserRole)) return ResponseEntity.status(403).build();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
}
