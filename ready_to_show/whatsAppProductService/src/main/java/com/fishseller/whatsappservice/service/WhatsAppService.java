package com.fishseller.whatsappservice.service;

import com.fishseller.whatsappservice.config.WhatsAppConfig;
import com.fishseller.whatsappservice.dto.WhatsAppMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

        private final WhatsAppConfig whatsAppConfig;
        private final RestClient.Builder restClientBuilder;
        private final CustomerMessageService messageService;

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
                                                                                                                .title("✅ Accept Order")
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
                String message = messageService.getOrderConfirmation(String.valueOf(orderId), total);
                sendSimpleText(toWaId, message);
        }

        /**
         * Send delivery assignment notification to customer with delivery person
         * details
         */
        public void sendDeliveryAssignmentNotification(String customerWaId, String deliveryPersonName,
                        String deliveryPersonWaPhone) {
                String message = messageService.getDeliveryAssignmentNotification(deliveryPersonName,
                                deliveryPersonWaPhone);
                sendSimpleText(customerWaId, message);
        }

        /**
         * Send welcome message to newly added customer
         */
        public void sendCustomerWelcomeMessage(String customerWaId, String customerName, String customerPhone,
                        String executiveName, String executiveWaPhone) {
                String message = messageService.getCustomerWelcomeByExecutive(customerName, customerPhone,
                                executiveName, executiveWaPhone);
                sendSimpleText(customerWaId, message);
        }

        /**
         * Send welcome message to newly added team member (Admin, Executive, etc.)
         */
        public void sendTeamMemberWelcomeMessage(String teamMemberWaId, String teamMemberName, String teamMemberPhone,
                        String roleName, String addedByName, String addedByWaPhone) {
                String message = String.format(
                                "🎉 *Welcome to the Team!* 🙌\n\n" +
                                                "Hello *%s*! 👋\n" +
                                                "Phone: %s\n\n" +
                                                "You have been added as a *%s* by *%s* (📞 %s).\n\n" +
                                                "Get ready to start managing the system! 🚀\n\n" +
                                                "Send 'hi' to access your control panel and get started.",
                                teamMemberName, teamMemberPhone, roleName, addedByName, addedByWaPhone);

                sendSimpleText(teamMemberWaId, message);
        }

        /**
         * Send unauthorized delivery person message
         */
        public void sendUnauthorizedDeliveryMessage(String toWaId) {
                String message = "❌ *Unauthorized Access*\n\n" +
                                "You are not registered as a delivery person. 🚫\n" +
                                "Only authorized delivery personnel can confirm orders.\n\n" +
                                "Please contact the admin if you believe this is an error. 📞";

                sendSimpleText(toWaId, message);
        }

        /**
         * Send enhanced delivery confirmation to group
         */
        public void sendDeliveryConfirmationToGroup(String groupId, Long orderId,
                        String deliveryPersonName, String deliveryPersonPhone, LocalDateTime confirmedAt) {

                String formattedTime = confirmedAt.format(DateTimeFormatter.ofPattern("hh:mm a"));

                String message = String.format(
                                "✅ *ORDER ASSIGNED* 🚀\n\n" +
                                                "📦 *Order #%d* has been taken!\n\n" +
                                                "🚴 *Delivery Person:*\n" +
                                                "   👤 Name: *%s*\n" +
                                                "   📞 Phone: %s\n\n" +
                                                "⏰ Confirmed at: %s\n\n" +
                                                "The customer will be notified shortly. Great job! 👏",
                                orderId, deliveryPersonName, deliveryPersonPhone, formattedTime);

                sendSimpleText(groupId, message);
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
