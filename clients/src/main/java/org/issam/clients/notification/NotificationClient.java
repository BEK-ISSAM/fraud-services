package org.issam.clients.notification;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "notification")
public interface NotificationClient {
    @PostMapping(path = "api/v1/notifications")
    void send(@RequestBody NotificationRequest req);
}
