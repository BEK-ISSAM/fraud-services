package org.issam.clients.notification;



public record NotificationRequest(
        Integer toCustomerId,
        String toCustomerEmail,
        String message
) { }