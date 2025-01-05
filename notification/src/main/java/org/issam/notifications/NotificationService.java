package org.issam.notifications;

import lombok.AllArgsConstructor;
import org.issam.clients.notification.NotificationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service @AllArgsConstructor
public class NotificationService {
    NotificationRepository notificationRepository;

    public void send(NotificationRequest req){
        notificationRepository.save(Notification.builder()
                .sender("IssamCode")
                .sentAt(LocalDateTime.now())
                .targetCustomerId(req.toCustomerId())
                .targetCustomerEmail(req.toCustomerEmail())
                .message(req.message())
                .build()
        );
    }

    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }
}
