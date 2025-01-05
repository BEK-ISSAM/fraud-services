package org.issam.notifications;

import lombok.extern.slf4j.Slf4j;
import org.issam.clients.notification.NotificationRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @AllArgsConstructor
@RequestMapping("api/v1/notifications")
@Slf4j
public class NotificationController {
    NotificationService notificationService;

    @PostMapping
    public void send(@RequestBody NotificationRequest req){
        log.info("New notification... {}", req);
        notificationService.send(req);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAllNotifications(){
        log.info("Getting all notifications {}...", notificationService.getAllNotifications().getBody());
        return notificationService.getAllNotifications();
    }
}
