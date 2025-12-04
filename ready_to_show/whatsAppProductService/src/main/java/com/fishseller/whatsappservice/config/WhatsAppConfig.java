package com.fishseller.whatsappservice.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class WhatsAppConfig {

    @Value("${whatsapp.api.token}")
    private String apiToken;

    @Value("${whatsapp.api.base-url}")
    private String apiBaseUrl;

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.webhook.verify-token}")
    private String webhookVerifyToken;

    @Value("${whatsapp.delivery-group-id}")
    private String deliveryGroupId;
}
