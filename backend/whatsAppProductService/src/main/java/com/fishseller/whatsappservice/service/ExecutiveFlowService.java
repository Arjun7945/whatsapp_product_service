package com.fishseller.whatsappservice.service;

import com.fishseller.whatsappservice.dto.WhatsAppMessageDto;
import com.fishseller.whatsappservice.dto.WhatsAppWebhookDto;
import com.fishseller.whatsappservice.model.Customer;
import com.fishseller.whatsappservice.model.TeamMember;
import com.fishseller.whatsappservice.model.enums.CustomerFlowStage;
import com.fishseller.whatsappservice.model.enums.ExecutiveFlowStage;
import com.fishseller.whatsappservice.model.enums.UserRole;
import com.fishseller.whatsappservice.repository.CustomerRepository;
import com.fishseller.whatsappservice.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service to handle executive conversation flow via WhatsApp
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutiveFlowService {

    private final TeamMemberRepository teamMemberRepository;
    private final CustomerRepository customerRepository;
    private final WhatsAppService whatsAppService;
    private final LocationValidationService locationValidationService;

    /**
     * Process incoming message from an executive
     */
    @Transactional
    public void handleExecutiveMessage(TeamMember executive, WhatsAppWebhookDto.Message message) {
        log.info("Processing executive message from: {} ({})", executive.getName(), executive.getWaPhoneNumber());

        // Handle different message types
        if (message.getType().equals("text") && message.getText() != null) {
            handleTextMessage(executive, message.getText().getBody());
        } else if (message.getType().equals("location") && message.getLocation() != null) {
            handleLocationMessage(executive, message.getLocation());
        } else if (message.getType().equals("interactive")) {
            if (message.getInteractive().getType().equals("button_reply")) {
                handleButtonReply(executive, message.getInteractive().getButtonReply());
            }
        }

        teamMemberRepository.save(executive);
    }

    /**
     * Handle text messages based on executive flow stage
     */
    private void handleTextMessage(TeamMember executive, String text) {
        ExecutiveFlowStage stage = executive.getCurrentFlowStage();

        if (stage == null) {
            stage = ExecutiveFlowStage.IDLE;
        }

        log.info("Executive {} in stage {} sent: {}", executive.getName(), stage, text);

        switch (stage) {
            case IDLE:
                handleIdleState(executive, text);
                break;
            case AWAITING_CUST_NAME:
                handleAwaitingCustomerName(executive, text);
                break;
            case AWAITING_CUST_PHONE:
                handleAwaitingCustomerPhone(executive, text);
                break;
            case AWAITING_CUST_WAPHONE:
                handleAwaitingCustomerWaPhone(executive, text);
                break;
            default:
                showMainMenu(executive);
        }
    }

    /**
     * Handle idle state - show main menu on "hi" or "hello"
     */
    private void handleIdleState(TeamMember executive, String text) {
        if (text.trim().equalsIgnoreCase("hi") || text.trim().equalsIgnoreCase("hello")) {
            showMainMenu(executive);
        } else {
            whatsAppService.sendSimpleText(executive.getWaPhoneNumber(),
                    "Send 'hi' to see the menu.");
        }
    }

    /**
     * Show main menu to executive
     */
    private void showMainMenu(TeamMember executive) {
        List<WhatsAppMessageDto.ButtonDto> buttons = List.of(
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("ADD_CUSTOMER")
                                .title("Add Customer")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("SHOW_CUSTOMERS")
                                .title("Show Customers")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("CONTACT_ADMIN")
                                .title("Contact Admin")
                                .build())
                        .build());

        String message = String.format("Hello %s. How may I help you!", executive.getName());
        whatsAppService.sendCartActionButtons(executive.getWaPhoneNumber(), message, buttons);
        executive.setCurrentFlowStage(ExecutiveFlowStage.IDLE);
    }

    /**
     * Handle button replies from executive
     */
    private void handleButtonReply(TeamMember executive, WhatsAppWebhookDto.ButtonReply buttonReply) {
        String buttonId = buttonReply.getId();
        log.info("Executive button reply: {}", buttonId);

        switch (buttonId) {
            case "ADD_CUSTOMER":
                startAddCustomerFlow(executive);
                break;
            case "SHOW_CUSTOMERS":
                showCustomersAddedToday(executive);
                break;
            case "CONTACT_ADMIN":
                whatsAppService.sendSimpleText(executive.getWaPhoneNumber(),
                        "Please contact admin at: [Admin Contact Info]");
                break;
            default:
                showMainMenu(executive);
        }
    }

    /**
     * Start the Add Customer flow
     */
    private void startAddCustomerFlow(TeamMember executive) {
        whatsAppService.sendSimpleText(executive.getWaPhoneNumber(),
                "Let's add a new customer! 📝\n\nPlease provide the customer's name:");
        executive.setCurrentFlowStage(ExecutiveFlowStage.AWAITING_CUST_NAME);
    }

    /**
     * Handle customer name input
     */
    private void handleAwaitingCustomerName(TeamMember executive, String text) {
        executive.setTempCustomerName(text.trim());
        whatsAppService.sendSimpleText(executive.getWaPhoneNumber(),
                "Great! Now please provide the customer's phone number:");
        executive.setCurrentFlowStage(ExecutiveFlowStage.AWAITING_CUST_PHONE);
    }

    /**
     * Handle customer phone number input
     */
    private void handleAwaitingCustomerPhone(TeamMember executive, String text) {
        executive.setTempCustomerPhone(text.trim());
        whatsAppService.sendSimpleText(executive.getWaPhoneNumber(),
                "Perfect! Now please provide the customer's WhatsApp number (with country code, e.g., 919876543210):");
        executive.setCurrentFlowStage(ExecutiveFlowStage.AWAITING_CUST_WAPHONE);
    }

    /**
     * Handle customer WhatsApp number input
     */
    private void handleAwaitingCustomerWaPhone(TeamMember executive, String text) {
        executive.setTempCustomerWaPhone(text.trim());
        whatsAppService.sendSimpleText(executive.getWaPhoneNumber(),
                "Excellent! Finally, please share the customer's location.\n\n" +
                        "📍 Tap the attachment icon (📎) → Location → Send location");
        executive.setCurrentFlowStage(ExecutiveFlowStage.AWAITING_CUST_LOCATION);
    }

    /**
     * Handle location message for new customer
     */
    private void handleLocationMessage(TeamMember executive, WhatsAppWebhookDto.Location location) {
        if (executive.getCurrentFlowStage() != ExecutiveFlowStage.AWAITING_CUST_LOCATION) {
            return;
        }

        double customerLat = location.getLatitude();
        double customerLon = location.getLongitude();

        // Validate location
        double distance = locationValidationService.getDistanceFromBusiness(customerLat, customerLon);

        if (!locationValidationService.isWithinDeliveryRadius(customerLat, customerLon)) {
            whatsAppService.sendSimpleText(executive.getWaPhoneNumber(),
                    String.format("❌ Sorry, this location is outside our delivery area (%.2f km away).\n\n" +
                            "Customer not added. Please try again with a location within 70 km.", distance));
            executive.setCurrentFlowStage(ExecutiveFlowStage.IDLE);
            showMainMenu(executive);
            return;
        }

        // Create customer with REGISTERED flow stage
        Customer newCustomer = Customer.builder()
                .name(executive.getTempCustomerName())
                .phoneNumber(executive.getTempCustomerPhone())
                .waPhoneNumber(executive.getTempCustomerWaPhone())
                .locationLat(customerLat)
                .locationLon(customerLon)
                .distanceFromBusinessKm(distance)
                .role(UserRole.CUSTOMER)
                .currentFlowStage(CustomerFlowStage.REGISTERED) // Set to REGISTERED so they can start browsing
                .addedByExecutiveId(executive.getId())
                .registeredAt(LocalDateTime.now())
                .build();

        customerRepository.save(newCustomer);

        // Clear temp fields
        executive.setTempCustomerName(null);
        executive.setTempCustomerPhone(null);
        executive.setTempCustomerWaPhone(null);
        executive.setCurrentFlowStage(ExecutiveFlowStage.IDLE);

        // Notify executive
        whatsAppService.sendSimpleText(executive.getWaPhoneNumber(),
                "✅ Customer added successfully!\n\n" +
                        "Ask the customer to check if they got a confirmation message from the business number.");

        // Notify new customer
        whatsAppService.sendCustomerWelcomeMessage(newCustomer.getWaPhoneNumber());

        log.info("Executive {} added new customer: {}", executive.getName(), newCustomer.getName());
    }

    /**
     * Show customers added by this executive today
     */
    private void showCustomersAddedToday(TeamMember executive) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        List<Customer> customers = customerRepository.findAll().stream()
                .filter(c -> c.getAddedByExecutiveId() != null &&
                        c.getAddedByExecutiveId().equals(executive.getId()) &&
                        c.getRegisteredAt() != null &&
                        c.getRegisteredAt().isAfter(startOfDay))
                .toList();

        if (customers.isEmpty()) {
            whatsAppService.sendSimpleText(executive.getWaPhoneNumber(),
                    "You haven't added any customers today.");
            return;
        }

        StringBuilder message = new StringBuilder("👥 Customers you added today:\n\n");
        for (int i = 0; i < customers.size(); i++) {
            Customer c = customers.get(i);
            message.append(String.format("%d. %s - %s\n", i + 1, c.getName(), c.getPhoneNumber()));
        }

        whatsAppService.sendSimpleText(executive.getWaPhoneNumber(), message.toString());
    }
}
