package com.formai.api.notifications.application.internal.outboundservices.email;

public interface EmailDeliveryService {

    boolean send(String destination, String subject, String body);
}
