package com.fishseller.whatsappservice.service;

import com.fishseller.whatsappservice.config.WhatsAppConfig;
import com.fishseller.whatsappservice.dto.WhatsAppMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

        private final WhatsAppConfig whatsAppConfig;
        private final RestClient.Builder restClientBuilder;

        private RestClient getRestClient() {
                return restClientBuilder
                                .baseUrl(whatsAppConfig.getApiBaseUrl())
                                .defaultHeader("Authorization", "Bearer " + whatsAppConfig.getApiToken())
                                .defaultHeader("Content-Type", "application/json")
                                .build();
        }

        public void sendSimpleText(String toWaId, String text) {
                WhatsAppMessageDto message = WhatsAppMessageDto.builder()
                                .to(toWaId)
                                .type("text")
                                .text(WhatsAppMessageDto.TextDto.builder()
                                                .body(text)
                                                .previewUrl(false)
                                                .build())
                                .build();

                sendToMeta(message);
        }

        public void sendInteractiveOrderAlert(String toWaId, String bodyText, Long orderId) {
                // Constructing a button message for the delivery group
                WhatsAppMessageDto message = WhatsAppMessageDto.builder()
                                .to(toWaId)
                                .type("interactive")
                                .interactive(WhatsAppMessageDto.InteractiveDto.builder()
                                                .type("button")
                                                .body(WhatsAppMessageDto.BodyDto.builder()
                                                                .text(bodyText)
                                                                .build())
                                                .action(WhatsAppMessageDto.ActionDto.builder()
                                                                .buttons(List.of(
                                                                                WhatsAppMessageDto.ButtonDto.builder()
                                                                                                .type("reply")
                                                                                                .reply(WhatsAppMessageDto.ReplyDto
                                                                                                                .builder()
                                                                                                                .id("DELIVERY_TAKE_"
                                                                                                                                + orderId)
                                                                                                                .title("Confirm Delivery")
                                                                                                                .build())
                                                                                                .build()))
                                                                .build())
                                                .build())
                                .build();

                sendToMeta(message);
        }

        public void sendInteractiveList(String toWaId, String bodyText, List<WhatsAppMessageDto.RowDto> rows) {
                WhatsAppMessageDto message = WhatsAppMessageDto.builder()
                                .to(toWaId)
                                .type("interactive")
                                .interactive(WhatsAppMessageDto.InteractiveDto.builder()
                                                .type("list")
                                                .body(WhatsAppMessageDto.BodyDto.builder()
                                                                .text(bodyText)
                                                                .build())
                                                .action(WhatsAppMessageDto.ActionDto.builder()
                                                                .button("View Fish")
                                                                .sections(List.of(
                                                                                WhatsAppMessageDto.SectionDto.builder()
                                                                                                .title("Available Fish")
                                                                                                .rows(rows)
                                                                                                .build()))
                                                                .build())
                                                .build())
                                .build();

                sendToMeta(message);
        }

        /**
         * Send interactive buttons for cart actions (Continue Shopping / Checkout)
         */
        public void sendCartActionButtons(String toWaId, String bodyText, List<WhatsAppMessageDto.ButtonDto> buttons) {
                WhatsAppMessageDto message = WhatsAppMessageDto.builder()
                                .to(toWaId)
                                .type("interactive")
                                .interactive(WhatsAppMessageDto.InteractiveDto.builder()
                                                .type("button")
                                                .body(WhatsAppMessageDto.BodyDto.builder()
                                                                .text(bodyText)
                                                                .build())
                                                .action(WhatsAppMessageDto.ActionDto.builder()
                                                                .buttons(buttons)
                                                                .build())
                                                .build())
                                .build();

                sendToMeta(message);
        }

        /**
         * Send order confirmation to customer
         */
        public void sendOrderConfirmation(String toWaId, Long orderId, Double total) {
                String message = String.format(
                                "✅ Order Confirmed! 🎉\n\n" +
                                                "Order #%d\n" +
                                                "Total: ₹%.2f\n" +
                                                "Payment: COD (Cash on Delivery)\n\n" +
                                                "Your order has been sent to our delivery team. " +
                                                "You'll be notified once a delivery person is assigned.\n\n" +
                                                "Thank you for your order! 🐟",
                                orderId, total);

                sendSimpleText(toWaId, message);
        }

        /**
         * Send delivery assignment notification to customer
         */
        public void sendDeliveryAssignmentNotification(String customerWaId, String deliveryPersonName) {
                String message = String.format(
                                "🚚 Delivery Person Assigned!\n\n" +
                                                "Your order will be delivered by: %s\n\n" +
                                                "They will contact you shortly. Thank you! 🙏",
                                deliveryPersonName);

                sendSimpleText(customerWaId, message);
        }

        /**
         * Send welcome message to newly added customer
         */
        public void sendCustomerWelcomeMessage(String customerWaId) {
                String message = "🎉 Welcome to our Fresh Fish Store!\n\n" +
                                "You have been added to our customer list.\n\n" +
                                "Get ready to start ordering fresh fish daily!\n\n" +
                                "Send 'start' to see the daily fresh fish details and continue. 🐟";

                sendSimpleText(customerWaId, message);
        }

        private void sendToMeta(WhatsAppMessageDto message) {
                try {
                        getRestClient().post()
                                        .uri("/" + whatsAppConfig.getPhoneNumberId() + "/messages")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(message)
                                        .retrieve()
                                        .toBodilessEntity();
                        log.info("Message sent to {}", message.getTo());
                } catch (Exception e) {
                        log.error("Failed to send message to {}: {}", message.getTo(), e.getMessage());
                }
        }
}
