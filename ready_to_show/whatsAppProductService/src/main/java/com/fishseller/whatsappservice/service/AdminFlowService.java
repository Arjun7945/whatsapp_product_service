package com.fishseller.whatsappservice.service;

import com.fishseller.whatsappservice.dto.WhatsAppMessageDto;
import com.fishseller.whatsappservice.dto.WhatsAppWebhookDto;
import com.fishseller.whatsappservice.model.Customer;
import com.fishseller.whatsappservice.model.TeamMember;
import com.fishseller.whatsappservice.model.enums.AdminFlowStage;
import com.fishseller.whatsappservice.model.enums.CustomerFlowStage;
import com.fishseller.whatsappservice.model.enums.UserRole;
import com.fishseller.whatsappservice.repository.CustomerRepository;
import com.fishseller.whatsappservice.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service to handle admin conversation flow via WhatsApp
 * Admins and Developers can perform CRUD operations on:
 * - Customers
 * - Delivery Persons
 * - Executives
 * - Assistant Admins
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminFlowService {

    private final TeamMemberRepository teamMemberRepository;
    private final CustomerRepository customerRepository;
    private final WhatsAppService whatsAppService;
    private final LocationValidationService locationValidationService;

    /**
     * Process incoming message from an admin or developer
     */
    @Transactional
    public void handleAdminMessage(TeamMember admin, WhatsAppWebhookDto.Message message) {
        log.info("Processing admin message from: {} ({})", admin.getName(), admin.getRole());

        // Check for ABORT command at any stage
        if (message.getType().equals("text") && message.getText() != null) {
            String text = message.getText().getBody().trim();
            if (isAbortCommand(text)) {
                handleAbortCommand(admin);
                teamMemberRepository.save(admin);
                return;
            }
        }

        // Handle different message types
        if (message.getType().equals("text") && message.getText() != null) {
            handleTextMessage(admin, message.getText().getBody());
        } else if (message.getType().equals("location") && message.getLocation() != null) {
            handleLocationMessage(admin, message.getLocation());
        } else if (message.getType().equals("interactive")) {
            if (message.getInteractive().getType().equals("button_reply")) {
                handleButtonReply(admin, message.getInteractive().getButtonReply());
            }
        }

        teamMemberRepository.save(admin);
    }

    /**
     * Handle text messages based on admin flow stage
     */
    private void handleTextMessage(TeamMember admin, String text) {
        AdminFlowStage stage = admin.getCurrentAdminFlowStage();

        if (stage == null) {
            stage = AdminFlowStage.IDLE;
        }

        log.info("Admin {} in stage {} sent: {}", admin.getName(), stage, text);

        switch (stage) {
            case IDLE:
                handleIdleState(admin, text);
                break;

            // Customer Management
            case AWAITING_CUST_NAME:
                handleCustomerNameInput(admin, text);
                break;
            case AWAITING_CUST_PHONE:
                handleCustomerPhoneInput(admin, text);
                break;
            case AWAITING_CUST_WAPHONE:
                handleCustomerWaPhoneInput(admin, text);
                break;

            // Delivery Person Management
            case AWAITING_DELIVERY_NAME:
                handleDeliveryPersonNameInput(admin, text);
                break;
            case AWAITING_DELIVERY_PHONE:
                handleDeliveryPersonPhoneInput(admin, text);
                break;
            case AWAITING_DELIVERY_WAPHONE:
                handleDeliveryPersonWaPhoneInput(admin, text);
                break;
            case AWAITING_DELIVERY_STATUS:
                handleDeliveryPersonStatusInput(admin, text);
                break;

            // Executive Management
            case AWAITING_EXEC_NAME:
                handleExecutiveNameInput(admin, text);
                break;
            case AWAITING_EXEC_PHONE:
                handleExecutivePhoneInput(admin, text);
                break;
            case AWAITING_EXEC_WAPHONE:
                handleExecutiveWaPhoneInput(admin, text);
                break;
            case AWAITING_EXEC_STATUS:
                handleExecutiveStatusInput(admin, text);
                break;

            // Assistant Admin Management
            case AWAITING_ASSISTANT_NAME:
                handleAssistantAdminNameInput(admin, text);
                break;
            case AWAITING_ASSISTANT_PHONE:
                handleAssistantAdminPhoneInput(admin, text);
                break;
            case AWAITING_ASSISTANT_WAPHONE:
                handleAssistantAdminWaPhoneInput(admin, text);
                break;
            case AWAITING_ASSISTANT_STATUS:
                handleAssistantAdminStatusInput(admin, text);
                break;

            default:
                showMainMenu(admin);
        }
    }

    /**
     * Handle idle state - show main menu on "hi" or "hello"
     */
    private void handleIdleState(TeamMember admin, String text) {
        if (text.trim().equalsIgnoreCase("hi") || text.trim().equalsIgnoreCase("hello")) {
            showMainMenu(admin);
        } else {
            whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                    "Send 'hi' to see the admin menu.");
        }
    }

    /**
     * Show main menu to admin with personalized greeting
     * Note: WhatsApp button titles have a 20-character limit
     */
    private void showMainMenu(TeamMember admin) {
        // First set of buttons (max 3)
        List<WhatsAppMessageDto.ButtonDto> buttons1 = List.of(
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("CUSTOMER_SECTION")
                                .title("👥 Customers")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("DELIVERY_SECTION")
                                .title("🚚 Delivery")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("EXECUTIVE_SECTION")
                                .title("💼 Executives")
                                .build())
                        .build());

        String greeting = String.format(
                "🎉 *Welcome, %s!* 👑\n\n" +
                        "You are logged in as: *%s*\n\n" +
                        "Please select a section to manage:\n\n" +
                        "💡 *Tip:* Send 'ABORT' anytime to return to this menu.",
                admin.getName(),
                admin.getRole());

        whatsAppService.sendCartActionButtons(admin.getWaPhoneNumber(), greeting, buttons1);

        // Send second message with remaining options
        List<WhatsAppMessageDto.ButtonDto> buttons2 = List.of(
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("ASSISTANT_SECTION")
                                .title("🛡️ Assistants")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("CONTACT_DEVELOPER")
                                .title("👨‍💻 Contact Dev")
                                .build())
                        .build());

        whatsAppService.sendCartActionButtons(admin.getWaPhoneNumber(),
                "*More Options:*", buttons2);

        admin.setCurrentAdminFlowStage(AdminFlowStage.IDLE);
    }

    /**
     * Handle button replies from admin
     */
    private void handleButtonReply(TeamMember admin, WhatsAppWebhookDto.ButtonReply buttonReply) {
        String buttonId = buttonReply.getId();
        log.info("Admin button reply: {}", buttonId);

        switch (buttonId) {
            // Main menu options
            case "CUSTOMER_SECTION":
                showCustomerMenu(admin);
                break;
            case "DELIVERY_SECTION":
                showDeliveryPersonMenu(admin);
                break;
            case "EXECUTIVE_SECTION":
                showExecutiveMenu(admin);
                break;
            case "ASSISTANT_SECTION":
                showAssistantAdminMenu(admin);
                break;
            case "CONTACT_DEVELOPER":
                whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                        "📞 *Contact Developer*\n\nFor technical support, please contact:\n[Developer Contact Info]");
                break;

            // Customer menu options
            case "ADD_CUSTOMER":
                startAddCustomer(admin);
                break;
            case "UPDATE_CUSTOMER":
                startUpdateCustomer(admin);
                break;
            case "DELETE_CUSTOMER":
                startDeleteCustomer(admin);
                break;
            case "SHOW_ALL_CUSTOMERS":
                showAllCustomers(admin);
                break;

            // Delivery Person menu options
            case "ADD_DELIVERY":
                startAddDeliveryPerson(admin);
                break;
            case "UPDATE_DELIVERY":
                startUpdateDeliveryPerson(admin);
                break;
            case "DELETE_DELIVERY":
                startDeleteDeliveryPerson(admin);
                break;
            case "SHOW_ALL_DELIVERY":
                showAllDeliveryPersons(admin);
                break;

            // Executive menu options
            case "ADD_EXECUTIVE":
                startAddExecutive(admin);
                break;
            case "UPDATE_EXECUTIVE":
                startUpdateExecutive(admin);
                break;
            case "DELETE_EXECUTIVE":
                startDeleteExecutive(admin);
                break;
            case "SHOW_ALL_EXECUTIVE":
                showAllExecutives(admin);
                break;

            // Assistant Admin menu options
            case "ADD_ASSISTANT":
                startAddAssistantAdmin(admin);
                break;
            case "UPDATE_ASSISTANT":
                startUpdateAssistantAdmin(admin);
                break;
            case "DELETE_ASSISTANT":
                startDeleteAssistantAdmin(admin);
                break;
            case "SHOW_ALL_ASSISTANT":
                showAllAssistantAdmins(admin);
                break;

            // Confirmation buttons
            case "CONFIRM_ADD":
                handleConfirmAdd(admin);
                break;
            case "EDIT_DETAILS":
                handleEditDetails(admin);
                break;
            case "CANCEL_OPERATION":
                handleAbortCommand(admin);
                break;

            // Back to main menu
            case "BACK_TO_MAIN":
                showMainMenu(admin);
                break;

            default:
                showMainMenu(admin);
        }
    }

    /**
     * Show Customer Management menu
     */
    private void showCustomerMenu(TeamMember admin) {
        List<WhatsAppMessageDto.ButtonDto> buttons = List.of(
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("ADD_CUSTOMER")
                                .title("➕ Add Customer")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("SHOW_ALL_CUSTOMERS")
                                .title("📋 Show All")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("BACK_TO_MAIN")
                                .title("⬅️ Back")
                                .build())
                        .build());

        whatsAppService.sendCartActionButtons(admin.getWaPhoneNumber(),
                "👥 *Customer Management*\n\nWhat would you like to do?", buttons);
        admin.setCurrentAdminFlowStage(AdminFlowStage.CUSTOMER_MENU);
    }

    /**
     * Show Delivery Person Management menu
     */
    private void showDeliveryPersonMenu(TeamMember admin) {
        List<WhatsAppMessageDto.ButtonDto> buttons = List.of(
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("ADD_DELIVERY")
                                .title("➕ Add")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("SHOW_ALL_DELIVERY")
                                .title("📋 Show All")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("BACK_TO_MAIN")
                                .title("⬅️ Back")
                                .build())
                        .build());

        whatsAppService.sendCartActionButtons(admin.getWaPhoneNumber(),
                "🚚 *Delivery Person Management*\n\nWhat would you like to do?", buttons);
        admin.setCurrentAdminFlowStage(AdminFlowStage.DELIVERY_MENU);
    }

    /**
     * Show Executive Management menu
     */
    private void showExecutiveMenu(TeamMember admin) {
        List<WhatsAppMessageDto.ButtonDto> buttons = List.of(
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("ADD_EXECUTIVE")
                                .title("➕ Add Executive")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("SHOW_ALL_EXECUTIVE")
                                .title("📋 Show All")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("BACK_TO_MAIN")
                                .title("⬅️ Back")
                                .build())
                        .build());

        whatsAppService.sendCartActionButtons(admin.getWaPhoneNumber(),
                "💼 *Executive Management*\n\nWhat would you like to do?", buttons);
        admin.setCurrentAdminFlowStage(AdminFlowStage.EXECUTIVE_MENU);
    }

    /**
     * Show Assistant Admin Management menu
     */
    private void showAssistantAdminMenu(TeamMember admin) {
        List<WhatsAppMessageDto.ButtonDto> buttons = List.of(
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("ADD_ASSISTANT")
                                .title("➕ Add")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("SHOW_ALL_ASSISTANT")
                                .title("📋 Show All")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("BACK_TO_MAIN")
                                .title("⬅️ Back")
                                .build())
                        .build());

        whatsAppService.sendCartActionButtons(admin.getWaPhoneNumber(),
                "🛡️ *Assistant Admin Management*\n\nWhat would you like to do?", buttons);
        admin.setCurrentAdminFlowStage(AdminFlowStage.ASSISTANT_MENU);
    }

    // ==================== CUSTOMER CRUD OPERATIONS ====================

    /**
     * Start Add Customer flow
     */
    private void startAddCustomer(TeamMember admin) {
        admin.setTempEntityType("CUSTOMER");
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "➕ *Add New Customer*\n\n📝 Please provide the customer's name:");
        admin.setCurrentAdminFlowStage(AdminFlowStage.AWAITING_CUST_NAME);
    }

    private void handleCustomerNameInput(TeamMember admin, String name) {
        admin.setTempCustomerName(name.trim());
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✅ Name: " + name.trim() + "\n\n📞 Please provide the customer's phone number:");
        admin.setCurrentAdminFlowStage(AdminFlowStage.AWAITING_CUST_PHONE);
    }

    private void handleCustomerPhoneInput(TeamMember admin, String phone) {
        admin.setTempCustomerPhone(phone.trim());
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✅ Phone: " + phone.trim()
                        + "\n\n📱 Please provide the customer's WhatsApp number (with country code, e.g., 919876543210):");
        admin.setCurrentAdminFlowStage(AdminFlowStage.AWAITING_CUST_WAPHONE);
    }

    private void handleCustomerWaPhoneInput(TeamMember admin, String waPhone) {
        admin.setTempCustomerWaPhone(waPhone.trim());
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✅ WhatsApp: " + waPhone.trim() + "\n\n📍 Please share the customer's location.\n\n" +
                        "📢 *How to share:*\n" +
                        "• Tap the attachment icon (📎)\n" +
                        "• Select 'Location'\n" +
                        "• Send location");
        admin.setCurrentAdminFlowStage(AdminFlowStage.AWAITING_CUST_LOCATION);
    }

    private void handleLocationMessage(TeamMember admin, WhatsAppWebhookDto.Location location) {
        if (admin.getCurrentAdminFlowStage() != AdminFlowStage.AWAITING_CUST_LOCATION) {
            return;
        }

        double customerLat = location.getLatitude();
        double customerLon = location.getLongitude();
        double distance = locationValidationService.getDistanceFromBusiness(customerLat, customerLon);

        // Show confirmation with all details
        String summary = String.format(
                "✅ *Customer Details Summary:*\n\n" +
                        "👤 Name: %s\n" +
                        "📞 Phone: %s\n" +
                        "📱 WhatsApp: %s\n" +
                        "📍 Location: %.6f, %.6f\n" +
                        "📏 Distance: %.2f km\n\n" +
                        "Confirm to add this customer?",
                admin.getTempCustomerName(),
                admin.getTempCustomerPhone(),
                admin.getTempCustomerWaPhone(),
                customerLat, customerLon, distance);

        // Store location temporarily in temp fields
        admin.setTempFieldName(String.valueOf(customerLat));
        admin.setTempFieldValue(String.valueOf(customerLon));

        List<WhatsAppMessageDto.ButtonDto> buttons = List.of(
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("CONFIRM_ADD")
                                .title("✅ Confirm & Add")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("CANCEL_OPERATION")
                                .title("❌ Cancel")
                                .build())
                        .build());

        whatsAppService.sendCartActionButtons(admin.getWaPhoneNumber(), summary, buttons);
        admin.setCurrentAdminFlowStage(AdminFlowStage.CONFIRMING_CUST_ADD);
    }

    private void handleConfirmAdd(TeamMember admin) {
        AdminFlowStage stage = admin.getCurrentAdminFlowStage();

        if (stage == AdminFlowStage.CONFIRMING_CUST_ADD) {
            finalizeCustomerAdd(admin);
        } else if (stage == AdminFlowStage.CONFIRMING_DELIVERY_ADD) {
            finalizeDeliveryPersonAdd(admin);
        } else if (stage == AdminFlowStage.CONFIRMING_EXEC_ADD) {
            finalizeExecutiveAdd(admin);
        } else if (stage == AdminFlowStage.CONFIRMING_ASSISTANT_ADD) {
            finalizeAssistantAdminAdd(admin);
        }
    }

    private void finalizeCustomerAdd(TeamMember admin) {
        // Parse location from temp fields
        double lat = Double.parseDouble(admin.getTempFieldName());
        double lon = Double.parseDouble(admin.getTempFieldValue());
        double distance = locationValidationService.getDistanceFromBusiness(lat, lon);

        // Create customer
        Customer newCustomer = Customer.builder()
                .name(admin.getTempCustomerName())
                .phoneNumber(admin.getTempCustomerPhone())
                .waPhoneNumber(admin.getTempCustomerWaPhone())
                .locationLat(lat)
                .locationLon(lon)
                .distanceFromBusinessKm(distance)
                .role(UserRole.CUSTOMER)
                .currentFlowStage(CustomerFlowStage.REGISTERED)
                .registeredAt(LocalDateTime.now())
                .build();

        customerRepository.save(newCustomer);

        // Clear temp fields
        clearTempFields(admin);

        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✅ *Customer Added Successfully!*\n\n" +
                        "👤 " + newCustomer.getName() + " has been added to the system.\n\n" +
                        "The customer can now start ordering by sending 'Hi' to the business number.");

        showCustomerMenu(admin);
    }

    /**
     * Show all customers
     */
    private void showAllCustomers(TeamMember admin) {
        List<Customer> customers = customerRepository.findAll();

        if (customers.isEmpty()) {
            whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                    "📋 *No customers found.*\n\nAdd your first customer to get started!");
            showCustomerMenu(admin);
            return;
        }

        StringBuilder message = new StringBuilder(
                String.format("📋 *All Customers* (Total: %d)\n\n", customers.size()));
        int count = 1;
        for (Customer c : customers) {
            message.append(String.format("%d. *%s*\n   📞 %s\n   📱 %s\n   📏 %.2f km\n\n",
                    count++, c.getName(), c.getPhoneNumber(), c.getWaPhoneNumber(),
                    c.getDistanceFromBusinessKm() != null ? c.getDistanceFromBusinessKm() : 0.0));
        }

        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(), message.toString());
        showCustomerMenu(admin);
    }

    private void startUpdateCustomer(TeamMember admin) {
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "🚧 *Update Customer*\n\nThis feature is coming soon!");
        showCustomerMenu(admin);
    }

    private void startDeleteCustomer(TeamMember admin) {
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "🚧 *Delete Customer*\n\nThis feature is coming soon!");
        showCustomerMenu(admin);
    }

    // ==================== DELIVERY PERSON CRUD OPERATIONS ====================

    private void startAddDeliveryPerson(TeamMember admin) {
        admin.setTempEntityType("DELIVERY");
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "➕ *Add New Delivery Person*\n\n📝 Please provide the delivery person's name:");
        admin.setCurrentAdminFlowStage(AdminFlowStage.AWAITING_DELIVERY_NAME);
    }

    private void handleDeliveryPersonNameInput(TeamMember admin, String name) {
        admin.setTempTeamMemberName(name.trim());
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✅ Name: " + name.trim() + "\n\n📞 Please provide the phone number:");
        admin.setCurrentAdminFlowStage(AdminFlowStage.AWAITING_DELIVERY_PHONE);
    }

    private void handleDeliveryPersonPhoneInput(TeamMember admin, String phone) {
        admin.setTempTeamMemberPhone(phone.trim());
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✅ Phone: " + phone.trim() + "\n\n📱 Please provide the WhatsApp number (with country code):");
        admin.setCurrentAdminFlowStage(AdminFlowStage.AWAITING_DELIVERY_WAPHONE);
    }

    private void handleDeliveryPersonWaPhoneInput(TeamMember admin, String waPhone) {
        admin.setTempTeamMemberWaPhone(waPhone.trim());
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✅ WhatsApp: " + waPhone.trim() + "\n\n🔄 Is this delivery person active? (yes/no):");
        admin.setCurrentAdminFlowStage(AdminFlowStage.AWAITING_DELIVERY_STATUS);
    }

    private void handleDeliveryPersonStatusInput(TeamMember admin, String status) {
        boolean isActive = status.trim().equalsIgnoreCase("yes") || status.trim().equalsIgnoreCase("y");
        admin.setTempTeamMemberIsActive(isActive);

        String summary = String.format(
                "✅ *Delivery Person Details Summary:*\n\n" +
                        "👤 Name: %s\n" +
                        "📞 Phone: %s\n" +
                        "📱 WhatsApp: %s\n" +
                        "🔄 Status: %s\n\n" +
                        "Confirm to add this delivery person?",
                admin.getTempTeamMemberName(),
                admin.getTempTeamMemberPhone(),
                admin.getTempTeamMemberWaPhone(),
                isActive ? "Active" : "Inactive");

        List<WhatsAppMessageDto.ButtonDto> buttons = List.of(
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("CONFIRM_ADD")
                                .title("✅ Confirm & Add")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("CANCEL_OPERATION")
                                .title("❌ Cancel")
                                .build())
                        .build());

        whatsAppService.sendCartActionButtons(admin.getWaPhoneNumber(), summary, buttons);
        admin.setCurrentAdminFlowStage(AdminFlowStage.CONFIRMING_DELIVERY_ADD);
    }

    private void finalizeDeliveryPersonAdd(TeamMember admin) {
        TeamMember newDeliveryPerson = TeamMember.builder()
                .name(admin.getTempTeamMemberName())
                .phoneNumber(admin.getTempTeamMemberPhone())
                .waPhoneNumber(admin.getTempTeamMemberWaPhone())
                .role(UserRole.DELIVERY_PERSON)
                .isActive(admin.getTempTeamMemberIsActive())
                .build();

        teamMemberRepository.save(newDeliveryPerson);

        // Send welcome message to new delivery person
        whatsAppService.sendTeamMemberWelcomeMessage(
                newDeliveryPerson.getWaPhoneNumber(),
                newDeliveryPerson.getName(),
                newDeliveryPerson.getPhoneNumber(),
                "Delivery Person",
                admin.getName(),
                admin.getWaPhoneNumber());

        clearTempFields(admin);

        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✅ *Delivery Person Added Successfully!*\n\n" +
                        "👤 " + newDeliveryPerson.getName() + " has been added to the system.\n\n" +
                        "A welcome message has been sent to the new delivery person. 📲");

        showDeliveryPersonMenu(admin);
    }

    private void showAllDeliveryPersons(TeamMember admin) {
        List<TeamMember> deliveryPersons = teamMemberRepository.findByRole(UserRole.DELIVERY_PERSON);

        if (deliveryPersons.isEmpty()) {
            whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                    "📋 *No delivery persons found.*");
            showDeliveryPersonMenu(admin);
            return;
        }

        StringBuilder message = new StringBuilder(
                String.format("📋 *All Delivery Persons* (Total: %d)\n\n", deliveryPersons.size()));
        int count = 1;
        for (TeamMember dp : deliveryPersons) {
            message.append(String.format("%d. *%s*\n   📞 %s\n   📱 %s\n   🔄 %s\n\n",
                    count++, dp.getName(), dp.getPhoneNumber(), dp.getWaPhoneNumber(),
                    dp.isActive() ? "Active" : "Inactive"));
        }

        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(), message.toString());
        showDeliveryPersonMenu(admin);
    }

    private void startUpdateDeliveryPerson(TeamMember admin) {
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "🚧 *Update Delivery Person*\n\nThis feature is coming soon!");
        showDeliveryPersonMenu(admin);
    }

    private void startDeleteDeliveryPerson(TeamMember admin) {
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "🚧 *Delete Delivery Person*\n\nThis feature is coming soon!");
        showDeliveryPersonMenu(admin);
    }

    // ==================== EXECUTIVE CRUD OPERATIONS ====================

    private void startAddExecutive(TeamMember admin) {
        admin.setTempEntityType("EXECUTIVE");
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "➕ *Add New Executive*\n\n📝 Please provide the executive's name:");
        admin.setCurrentAdminFlowStage(AdminFlowStage.AWAITING_EXEC_NAME);
    }

    private void handleExecutiveNameInput(TeamMember admin, String name) {
        admin.setTempTeamMemberName(name.trim());
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✅ Name: " + name.trim() + "\n\n📞 Please provide the phone number:");
        admin.setCurrentAdminFlowStage(AdminFlowStage.AWAITING_EXEC_PHONE);
    }

    private void handleExecutivePhoneInput(TeamMember admin, String phone) {
        admin.setTempTeamMemberPhone(phone.trim());
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✅ Phone: " + phone.trim() + "\n\n📱 Please provide the WhatsApp number (with country code):");
        admin.setCurrentAdminFlowStage(AdminFlowStage.AWAITING_EXEC_WAPHONE);
    }

    private void handleExecutiveWaPhoneInput(TeamMember admin, String waPhone) {
        admin.setTempTeamMemberWaPhone(waPhone.trim());
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✅ WhatsApp: " + waPhone.trim() + "\n\n🔄 Is this executive active? (yes/no):");
        admin.setCurrentAdminFlowStage(AdminFlowStage.AWAITING_EXEC_STATUS);
    }

    private void handleExecutiveStatusInput(TeamMember admin, String status) {
        boolean isActive = status.trim().equalsIgnoreCase("yes") || status.trim().equalsIgnoreCase("y");
        admin.setTempTeamMemberIsActive(isActive);

        String summary = String.format(
                "✅ *Executive Details Summary:*\n\n" +
                        "👤 Name: %s\n" +
                        "📞 Phone: %s\n" +
                        "📱 WhatsApp: %s\n" +
                        "🔄 Status: %s\n\n" +
                        "Confirm to add this executive?",
                admin.getTempTeamMemberName(),
                admin.getTempTeamMemberPhone(),
                admin.getTempTeamMemberWaPhone(),
                isActive ? "Active" : "Inactive");

        List<WhatsAppMessageDto.ButtonDto> buttons = List.of(
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("CONFIRM_ADD")
                                .title("✅ Confirm & Add")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("CANCEL_OPERATION")
                                .title("❌ Cancel")
                                .build())
                        .build());

        whatsAppService.sendCartActionButtons(admin.getWaPhoneNumber(), summary, buttons);
        admin.setCurrentAdminFlowStage(AdminFlowStage.CONFIRMING_EXEC_ADD);
    }

    private void finalizeExecutiveAdd(TeamMember admin) {
        TeamMember newExecutive = TeamMember.builder()
                .name(admin.getTempTeamMemberName())
                .phoneNumber(admin.getTempTeamMemberPhone())
                .waPhoneNumber(admin.getTempTeamMemberWaPhone())
                .role(UserRole.EXECUTIVE)
                .isActive(admin.getTempTeamMemberIsActive())
                .build();

        teamMemberRepository.save(newExecutive);

        // Send welcome message to new executive
        whatsAppService.sendTeamMemberWelcomeMessage(
                newExecutive.getWaPhoneNumber(),
                newExecutive.getName(),
                newExecutive.getPhoneNumber(),
                "Executive",
                admin.getName(),
                admin.getWaPhoneNumber());

        clearTempFields(admin);

        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✅ *Executive Added Successfully!*\n\n" +
                        "👤 " + newExecutive.getName() + " has been added to the system.\n\n" +
                        "A welcome message has been sent to the new executive. 📲");

        showExecutiveMenu(admin);
    }

    private void showAllExecutives(TeamMember admin) {
        List<TeamMember> executives = teamMemberRepository.findByRole(UserRole.EXECUTIVE);

        if (executives.isEmpty()) {
            whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                    "📋 *No executives found.*");
            showExecutiveMenu(admin);
            return;
        }

        StringBuilder message = new StringBuilder(
                String.format("📋 *All Executives* (Total: %d)\n\n", executives.size()));
        int count = 1;
        for (TeamMember exec : executives) {
            message.append(String.format("%d. *%s*\n   📞 %s\n   📱 %s\n   🔄 %s\n\n",
                    count++, exec.getName(), exec.getPhoneNumber(), exec.getWaPhoneNumber(),
                    exec.isActive() ? "Active" : "Inactive"));
        }

        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(), message.toString());
        showExecutiveMenu(admin);
    }

    private void startUpdateExecutive(TeamMember admin) {
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "🚧 *Update Executive*\n\nThis feature is coming soon!");
        showExecutiveMenu(admin);
    }

    private void startDeleteExecutive(TeamMember admin) {
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "🚧 *Delete Executive*\n\nThis feature is coming soon!");
        showExecutiveMenu(admin);
    }

    // ==================== ASSISTANT ADMIN CRUD OPERATIONS ====================

    private void startAddAssistantAdmin(TeamMember admin) {
        admin.setTempEntityType("ASSISTANT");
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "➕ *Add New Assistant Admin*\n\n📝 Please provide the assistant admin's name:");
        admin.setCurrentAdminFlowStage(AdminFlowStage.AWAITING_ASSISTANT_NAME);
    }

    private void handleAssistantAdminNameInput(TeamMember admin, String name) {
        admin.setTempTeamMemberName(name.trim());
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✅ Name: " + name.trim() + "\n\n📞 Please provide the phone number:");
        admin.setCurrentAdminFlowStage(AdminFlowStage.AWAITING_ASSISTANT_PHONE);
    }

    private void handleAssistantAdminPhoneInput(TeamMember admin, String phone) {
        admin.setTempTeamMemberPhone(phone.trim());
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✅ Phone: " + phone.trim() + "\n\n📱 Please provide the WhatsApp number (with country code):");
        admin.setCurrentAdminFlowStage(AdminFlowStage.AWAITING_ASSISTANT_WAPHONE);
    }

    private void handleAssistantAdminWaPhoneInput(TeamMember admin, String waPhone) {
        admin.setTempTeamMemberWaPhone(waPhone.trim());
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✅ WhatsApp: " + waPhone.trim() + "\n\n🔄 Is this assistant admin active? (yes/no):");
        admin.setCurrentAdminFlowStage(AdminFlowStage.AWAITING_ASSISTANT_STATUS);
    }

    private void handleAssistantAdminStatusInput(TeamMember admin, String status) {
        boolean isActive = status.trim().equalsIgnoreCase("yes") || status.trim().equalsIgnoreCase("y");
        admin.setTempTeamMemberIsActive(isActive);

        String summary = String.format(
                "✅ *Assistant Admin Details Summary:*\n\n" +
                        "👤 Name: %s\n" +
                        "📞 Phone: %s\n" +
                        "📱 WhatsApp: %s\n" +
                        "🔄 Status: %s\n\n" +
                        "Confirm to add this assistant admin?",
                admin.getTempTeamMemberName(),
                admin.getTempTeamMemberPhone(),
                admin.getTempTeamMemberWaPhone(),
                isActive ? "Active" : "Inactive");

        List<WhatsAppMessageDto.ButtonDto> buttons = List.of(
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("CONFIRM_ADD")
                                .title("✅ Confirm & Add")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("CANCEL_OPERATION")
                                .title("❌ Cancel")
                                .build())
                        .build());

        whatsAppService.sendCartActionButtons(admin.getWaPhoneNumber(), summary, buttons);
        admin.setCurrentAdminFlowStage(AdminFlowStage.CONFIRMING_ASSISTANT_ADD);
    }

    private void finalizeAssistantAdminAdd(TeamMember admin) {
        TeamMember newAssistantAdmin = TeamMember.builder()
                .name(admin.getTempTeamMemberName())
                .phoneNumber(admin.getTempTeamMemberPhone())
                .waPhoneNumber(admin.getTempTeamMemberWaPhone())
                .role(UserRole.ASSISTANT_ADMIN)
                .isActive(admin.getTempTeamMemberIsActive())
                .build();

        teamMemberRepository.save(newAssistantAdmin);

        // Send welcome message to new assistant admin
        whatsAppService.sendTeamMemberWelcomeMessage(
                newAssistantAdmin.getWaPhoneNumber(),
                newAssistantAdmin.getName(),
                newAssistantAdmin.getPhoneNumber(),
                "Assistant Admin",
                admin.getName(),
                admin.getWaPhoneNumber());

        clearTempFields(admin);

        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✅ *Assistant Admin Added Successfully!*\n\n" +
                        "👤 " + newAssistantAdmin.getName() + " has been added to the system.\n\n" +
                        "A welcome message has been sent to the new assistant admin. 📲");

        showAssistantAdminMenu(admin);
    }

    private void showAllAssistantAdmins(TeamMember admin) {
        List<TeamMember> assistantAdmins = teamMemberRepository.findByRole(UserRole.ASSISTANT_ADMIN);

        if (assistantAdmins.isEmpty()) {
            whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                    "📋 *No assistant admins found.*");
            showAssistantAdminMenu(admin);
            return;
        }

        StringBuilder message = new StringBuilder(
                String.format("📋 *All Assistant Admins* (Total: %d)\n\n", assistantAdmins.size()));
        int count = 1;
        for (TeamMember aa : assistantAdmins) {
            message.append(String.format("%d. *%s*\n   📞 %s\n   📱 %s\n   🔄 %s\n\n",
                    count++, aa.getName(), aa.getPhoneNumber(), aa.getWaPhoneNumber(),
                    aa.isActive() ? "Active" : "Inactive"));
        }

        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(), message.toString());
        showAssistantAdminMenu(admin);
    }

    private void startUpdateAssistantAdmin(TeamMember admin) {
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "🚧 *Update Assistant Admin*\n\nThis feature is coming soon!");
        showAssistantAdminMenu(admin);
    }

    private void startDeleteAssistantAdmin(TeamMember admin) {
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "🚧 *Delete Assistant Admin*\n\nThis feature is coming soon!");
        showAssistantAdminMenu(admin);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if text is an ABORT command
     */
    private boolean isAbortCommand(String text) {
        return text.trim().equalsIgnoreCase("ABORT");
    }

    /**
     * Handle ABORT command - return to main menu
     */
    private void handleAbortCommand(TeamMember admin) {
        clearTempFields(admin);
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "❌ *Operation Cancelled*\n\nReturning to main menu...");
        showMainMenu(admin);
    }

    /**
     * Handle edit details request
     */
    private void handleEditDetails(TeamMember admin) {
        whatsAppService.sendSimpleText(admin.getWaPhoneNumber(),
                "✏️ *Edit Details*\n\nThis feature is coming soon!\n\nFor now, please cancel and start over.");
        handleAbortCommand(admin);
    }

    /**
     * Clear all temporary fields
     */
    private void clearTempFields(TeamMember admin) {
        admin.setTempEntityType(null);
        admin.setTempEntityId(null);
        admin.setTempFieldName(null);
        admin.setTempFieldValue(null);
        admin.setTempCustomerName(null);
        admin.setTempCustomerPhone(null);
        admin.setTempCustomerWaPhone(null);
        admin.setTempTeamMemberName(null);
        admin.setTempTeamMemberPhone(null);
        admin.setTempTeamMemberWaPhone(null);
        admin.setTempTeamMemberIsActive(null);
    }
}
