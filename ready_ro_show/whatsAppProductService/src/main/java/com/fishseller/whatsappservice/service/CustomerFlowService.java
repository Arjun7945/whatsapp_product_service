package com.fishseller.whatsappservice.service;

import com.fishseller.whatsappservice.dto.CartItemDto;
import com.fishseller.whatsappservice.dto.UserLookupResult;
import com.fishseller.whatsappservice.dto.WhatsAppMessageDto;
import com.fishseller.whatsappservice.dto.WhatsAppWebhookDto;
import com.fishseller.whatsappservice.model.*;
import com.fishseller.whatsappservice.model.enums.CustomerFlowStage;
import com.fishseller.whatsappservice.model.enums.OrderStatus;
import com.fishseller.whatsappservice.model.enums.UserRole;
import com.fishseller.whatsappservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service to handle the complete customer conversation flow via WhatsApp
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerFlowService {

    private final CustomerRepository customerRepository;
    private final FishProductRepository fishProductRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final WhatsAppService whatsAppService;
    private final LocationValidationService locationValidationService;
    private final ShoppingCartService shoppingCartService;
    private final ExecutiveFlowService executiveFlowService;
    private final DeliveryFlowService deliveryFlowService;
    private final UserRoleLookupService userRoleLookupService;
    private final AdminFlowService adminFlowService;

    /**
     * Main entry point for processing incoming WhatsApp messages
     * Routes messages based on sender role: TeamMember (Executive/Delivery) or
     * Customer
     * 
     * Flow:
     * 1. Lookup user role using UserRoleLookupService
     * 2. Route based on role:
     * - ADMIN/DEVELOPER -> AdminFlowService
     * - EXECUTIVE -> ExecutiveFlowService
     * - DELIVERY_PERSON -> DeliveryFlowService
     * - CUSTOMER -> Customer flow (existing or registered)
     * - null (unknown) -> Create new customer and start registration
     */
    @Transactional
    public void processIncomingMessage(WhatsAppWebhookDto.Value messageValue) {
        if (messageValue.getMessages() == null || messageValue.getMessages().isEmpty()) {
            return;
        }

        WhatsAppWebhookDto.Message message = messageValue.getMessages().get(0);
        String fromWaId = message.getFrom();

        log.info("Processing message from: {}", fromWaId);

        // Step 1: Lookup user role
        UserLookupResult lookupResult = userRoleLookupService.lookupUserByWaPhoneNumber(fromWaId);

        // Step 2: Route based on role
        if (lookupResult != null) {
            // User exists in system - route based on role
            switch (lookupResult.getRole()) {
                case ADMIN:
                case DEVELOPER:
                    // Route to Admin Flow
                    TeamMember admin = lookupResult.asTeamMember();
                    log.info("Routing to Admin Flow for: {} (Role: {})",
                            admin.getName(), admin.getRole());
                    adminFlowService.handleAdminMessage(admin, message);
                    return;

                case EXECUTIVE:
                    // Route to Executive Flow
                    TeamMember executive = lookupResult.asTeamMember();
                    log.info("Routing to Executive Flow for: {}", executive.getName());
                    executiveFlowService.handleExecutiveMessage(executive, message);
                    return;

                case DELIVERY_PERSON:
                    // Route to Delivery Flow
                    TeamMember deliveryPerson = lookupResult.asTeamMember();
                    log.info("Routing to Delivery Flow for: {}", deliveryPerson.getName());

                    // Handle delivery person button replies (order confirmation)
                    if (message.getType().equals("interactive") &&
                            message.getInteractive().getType().equals("button_reply")) {
                        WhatsAppWebhookDto.ButtonReply buttonReply = message.getInteractive().getButtonReply();
                        if (buttonReply.getId().startsWith("DELIVERY_TAKE_")) {
                            Long orderId = Long.parseLong(buttonReply.getId().replace("DELIVERY_TAKE_", ""));
                            deliveryFlowService.handleDeliveryConfirmation(fromWaId, orderId);
                            return;
                        }
                    }
                    // For other messages from delivery persons, just acknowledge
                    whatsAppService.sendSimpleText(fromWaId,
                            "Thank you! Please use the order confirmation buttons in the group.");
                    return;

                case CUSTOMER:
                    // Route to Customer Flow
                    Customer customer = lookupResult.asCustomer();
                    log.info("Routing to Customer Flow for: {} (Stage: {})",
                            customer.getName() != null ? customer.getName() : "Unknown",
                            customer.getCurrentFlowStage());
                    handleCustomerMessage(customer, message);
                    return;

                default:
                    log.warn("Unknown role for user: {}", fromWaId);
                    return;
            }
        } else {
            // Unknown number - create new customer and start registration flow
            log.info("Unknown number {} - creating new customer", fromWaId);
            Customer newCustomer = Customer.builder()
                    .waPhoneNumber(fromWaId)
                    .currentFlowStage(CustomerFlowStage.NEW)
                    .role(UserRole.CUSTOMER)
                    .build();
            newCustomer = customerRepository.save(newCustomer);

            handleCustomerMessage(newCustomer, message);
        }
    }

    /**
     * Handle messages from customers (both new and existing)
     */
    private void handleCustomerMessage(Customer customer, WhatsAppWebhookDto.Message message) {
        // Allow customer to restart flow from any stage by sending "start" or "hi"
        if (message.getType().equals("text") && message.getText() != null) {
            String text = message.getText().getBody().trim();
            if (text.equalsIgnoreCase("start")) {
                customer.setCurrentFlowStage(CustomerFlowStage.REGISTERED);
                showProductCatalog(customer);
                customerRepository.save(customer);
                return;
            } else if (text.equalsIgnoreCase("hi") || text.equalsIgnoreCase("hello")) {
                // Reset to registered if already registered, otherwise treat as new customer
                if (customer.getCurrentFlowStage() != CustomerFlowStage.NEW &&
                        customer.getCurrentFlowStage() != CustomerFlowStage.AWAITING_NAME) {
                    customer.setCurrentFlowStage(CustomerFlowStage.REGISTERED);
                    whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                            "👋 *Hello " + customer.getName() + "!* 😊\n\n" +
                                    "🐟 Ready to explore our fresh fish selection?\n\n" +
                                    "Simply send *'start'* to browse our premium catch of the day! ✨");
                    customerRepository.save(customer);
                    return;
                }
            }
        }

        // Handle different message types for customers
        if (message.getType().equals("text") && message.getText() != null) {
            handleTextMessage(customer, message.getText().getBody());
        } else if (message.getType().equals("location") && message.getLocation() != null) {
            handleLocationMessage(customer, message.getLocation());
        } else if (message.getType().equals("interactive")) {
            if (message.getInteractive().getType().equals("button_reply")) {
                handleButtonReply(customer, message.getInteractive().getButtonReply());
            } else if (message.getInteractive().getType().equals("list_reply")) {
                handleListReply(customer, message.getInteractive().getListReply());
            }
        }

        customerRepository.save(customer);
    }

    /**
     * Handle text messages based on customer flow stage
     */
    private void handleTextMessage(Customer customer, String text) {
        log.info("Customer {} in stage {} sent: {}", customer.getWaPhoneNumber(), customer.getCurrentFlowStage(), text);

        switch (customer.getCurrentFlowStage()) {
            case NEW:
                handleNewCustomer(customer, text);
                break;
            case AWAITING_NAME:
                handleAwaitingName(customer, text);
                break;
            case AWAITING_PHONE:
                handleAwaitingPhone(customer, text);
                break;
            case AWAITING_QUANTITY:
                handleAwaitingQuantity(customer, text);
                break;
            case REGISTERED:
            case BROWSING:
            case ADDING_TO_CART:
                handleRegisteredCustomer(customer, text);
                break;
            default:
                whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                        "I didn't understand that. Send 'start' to browse our fish selection.");
        }
    }

    /**
     * 1. Handle new customer - greet and ask for name
     */
    private void handleNewCustomer(Customer customer, String text) {
        if (text.trim().equalsIgnoreCase("hi") || text.trim().equalsIgnoreCase("hello")) {
            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    "🙏 *Welcome to Our Fresh Fish Store!* 🐟\n\n" +
                            "We're delighted to have you here! ✨\n\n" +
                            "To serve you better, may I know your good name?");
            customer.setCurrentFlowStage(CustomerFlowStage.AWAITING_NAME);
        } else {
            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    "👋 *Hello! Welcome to Our Fresh Fish Store!* 🐟\n\n" +
                            "We offer the freshest catch of the day, delivered right to your doorstep! 🚚\n\n" +
                            "Please send *'Hi'* to get started with us. 😊");
        }
    }

    /**
     * 2. Collect customer name
     */
    private void handleAwaitingName(Customer customer, String text) {
        customer.setName(text.trim());
        whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                "🙏 *Nice to meet you, " + text.trim() + "!* 👋\n\n" +
                        "Thank you for choosing us! We're excited to serve you the freshest fish. 🐟\n\n" +
                        "Could you please share your contact number?");
        customer.setCurrentFlowStage(CustomerFlowStage.AWAITING_PHONE);
    }

    /**
     * 3. Collect phone number and request location
     */
    private void handleAwaitingPhone(Customer customer, String text) {
        customer.setPhoneNumber(text.trim());
        whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                "✅ *Perfect! Thank you!* 🙏\n\n" +
                        "Now, to ensure we can deliver fresh fish to your doorstep, please share your location. 📍\n\n"
                        +
                        "📢 *How to share:*\n" +
                        "• Tap the attachment icon (📎)\n" +
                        "• Select 'Location'\n" +
                        "• Send your current location\n\n" +
                        "This helps us serve you better! 😊");
        customer.setCurrentFlowStage(CustomerFlowStage.AWAITING_LOCATION);
    }

    /**
     * 4. Handle location message and validate delivery area
     */
    private void handleLocationMessage(Customer customer, WhatsAppWebhookDto.Location location) {
        double customerLat = location.getLatitude();
        double customerLon = location.getLongitude();

        log.info("Received location from customer {}: lat={}, lon={}", customer.getWaPhoneNumber(), customerLat,
                customerLon);

        // Save location
        customer.setLocationLat(customerLat);
        customer.setLocationLon(customerLon);

        // Calculate distance
        double distance = locationValidationService.getDistanceFromBusiness(customerLat, customerLon);
        customer.setDistanceFromBusinessKm(distance);

        // Validate delivery area
        if (locationValidationService.isWithinDeliveryRadius(customerLat, customerLon)) {
            customer.setCurrentFlowStage(CustomerFlowStage.REGISTERED);
            customer.setRegisteredAt(LocalDateTime.now());

            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    String.format("✨ *Wonderful News, %s!* 🎉\n\n" +
                            "✅ You're in our delivery zone! We're thrilled to serve you. 🙏\n" +
                            "📍 Distance from our store: *%.2f km*\n\n" +
                            "🐟 *Ready to explore our fresh catch?*\n" +
                            "Simply send *'start'* to browse our premium selection of fresh fish!\n\n" +
                            "💚 We promise the freshest quality, delivered with care!",
                            customer.getName(), distance));
        } else {
            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    String.format("😔 *We're Sorry, %s!*\n\n" +
                            "Unfortunately, your location is outside our current delivery area. 📍\n\n" +
                            "📍 Distance from our store: *%.2f km*\n" +
                            "🚚 Our delivery radius: *50 km*\n\n" +
                            "💔 We'd love to serve you in the future!\n" +
                            "We're constantly expanding our delivery zones. Please check back with us soon! 🙏\n\n" +
                            "Thank you for your interest! ❤️",
                            customer.getName(), distance));
            customer.setCurrentFlowStage(CustomerFlowStage.NEW);
        }
    }

    /**
     * 5. Handle registered customer commands
     */
    private void handleRegisteredCustomer(Customer customer, String text) {
        if (text.trim().equalsIgnoreCase("start")) {
            showProductCatalog(customer);
        } else {
            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    "👋 *Hello " + customer.getName() + "!* 😊\n\n" +
                            "🐟 Ready to explore our fresh fish selection?\n\n" +
                            "Simply send *'start'* to browse our premium catch of the day! ✨");
        }
    }

    /**
     * 6. Show product catalog
     */
    private void showProductCatalog(Customer customer) {
        log.info("Fetching available fish products for customer: {}", customer.getWaPhoneNumber());
        List<FishProduct> products = fishProductRepository.findByIsAvailableTrue();
        log.info("Found {} available fish products", products.size());

        if (products.isEmpty()) {
            // Also check all products to see if any exist
            List<FishProduct> allProducts = fishProductRepository.findAll();
            log.warn("No available fish found, but {} total fish products exist in database", allProducts.size());
            if (!allProducts.isEmpty()) {
                log.warn("Sample product: name={}, isAvailable={}",
                        allProducts.get(0).getName(), allProducts.get(0).isAvailable());
            }

            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    "😔 *We're Sorry, " + customer.getName() + "!*\n\n" +
                            "We don't have any fresh fish available at this moment. 🐟\n\n" +
                            "🕒 Our fresh stock arrives daily!\n" +
                            "Please check back with us in a little while. We'll have the freshest catch ready for you! ✨\n\n"
                            +
                            "Thank you for your patience! 🙏");
            return;
        }

        List<WhatsAppMessageDto.RowDto> rows = products.stream()
                .map(p -> WhatsAppMessageDto.RowDto.builder()
                        .id("FISH_" + p.getId())
                        .title(p.getName())
                        .description(String.format("₹%.2f/kg", p.getPricePerKg()))
                        .build())
                .collect(Collectors.toList());

        whatsAppService.sendInteractiveList(customer.getWaPhoneNumber(),
                "🐟 *Fresh Fish Available Today, " + customer.getName() + "!* ✨\n\n" +
                        "🌊 Premium quality, freshly caught!\n" +
                        "🚚 Delivered right to your doorstep!\n\n" +
                        "Select a fish to add to your cart:",
                rows);

        customer.setCurrentFlowStage(CustomerFlowStage.BROWSING);
    }

    /**
     * 7. Handle product selection from list
     */
    private void handleListReply(Customer customer, WhatsAppWebhookDto.ListReply listReply) {
        String selectedId = listReply.getId(); // e.g., "FISH_123"

        if (!selectedId.startsWith("FISH_")) {
            return;
        }

        Long fishProductId = Long.parseLong(selectedId.replace("FISH_", ""));

        Optional<FishProduct> fishOpt = fishProductRepository.findById(fishProductId);
        if (fishOpt.isEmpty()) {
            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    "😔 *Oops!*\n\n" +
                            "Sorry, that fish is no longer available. 🐟\n\n" +
                            "Please send *'start'* to see our current fresh selection! ✨");
            return;
        }

        FishProduct fish = fishOpt.get();

        // Store selected product temporarily
        customer.setTempSelectedProductId(fishProductId);
        customer.setCurrentFlowStage(CustomerFlowStage.AWAITING_QUANTITY);

        whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                String.format("✅ *Great Choice, %s!* 🎉\n\n" +
                        "🐟 You selected: *%s*\n" +
                        "💰 Price: *₹%.2f per kg*\n\n" +
                        "⚖️ How many kilograms would you like?\n" +
                        "(Example: 2 or 2.5)",
                        customer.getName(), fish.getName(), fish.getPricePerKg()));
    }

    /**
     * 8. Handle quantity input and add to cart or update existing item
     */
    private void handleAwaitingQuantity(Customer customer, String text) {
        try {
            Double quantity = Double.parseDouble(text.trim());

            if (quantity <= 0) {
                whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                        "⚠️ *Invalid Quantity!*\n\n" +
                                "Please enter a quantity greater than 0.\n" +
                                "Example: 2 or 2.5 😊");
                return;
            }

            Long fishProductId = customer.getTempSelectedProductId();

            // Check if this is an edit or new add
            List<CartItemDto> items = shoppingCartService.getCartItems(customer.getId());
            boolean isEdit = items.stream()
                    .anyMatch(i -> i.getFishProductId().equals(fishProductId));

            if (isEdit) {
                // Update existing item
                shoppingCartService.updateQuantity(customer.getId(), fishProductId, quantity);
                whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                        "✅ *Perfect!* 🎉\n\n" +
                                "Quantity updated successfully! ✨");
                showCartSummary(customer);
            } else {
                // Add new item
                shoppingCartService.addToCart(customer.getId(), fishProductId, quantity);
                sendCartOptions(customer);
                customer.setCurrentFlowStage(CustomerFlowStage.ADDING_TO_CART);
            }

        } catch (NumberFormatException e) {
            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    "⚠️ *Oops! Invalid Input*\n\n" +
                            "Please enter a valid number for quantity.\n" +
                            "Examples: *2* or *1.5* or *3.5* 😊");
        }
    }

    /**
     * 9. Send cart action options
     */
    private void sendCartOptions(Customer customer) {
        List<WhatsAppMessageDto.ButtonDto> buttons = List.of(
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("CONTINUE_SHOPPING")
                                .title("Add More Fish")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("CHECKOUT")
                                .title("Checkout")
                                .build())
                        .build());

        whatsAppService.sendCartActionButtons(customer.getWaPhoneNumber(),
                "✅ *Added to Cart Successfully!* 🎉\n\n" +
                        "👍 Great choice, " + customer.getName() + "!\n\n" +
                        "What would you like to do next?",
                buttons);
    }

    /**
     * 10. Handle button replies (cart actions, order confirmation, delivery
     * confirmation)
     */
    private void handleButtonReply(Customer customer, WhatsAppWebhookDto.ButtonReply buttonReply) {
        String buttonId = buttonReply.getId();

        log.info("Button reply from {}: {}", customer.getWaPhoneNumber(), buttonId);

        if ("CONTINUE_SHOPPING".equals(buttonId)) {
            showProductCatalog(customer);
        } else if ("CHECKOUT".equals(buttonId)) {
            showCartSummary(customer);
        } else if ("CONFIRM_ORDER".equals(buttonId)) {
            placeOrder(customer);
        } else if ("CANCEL_ORDER".equals(buttonId)) {
            handleCancelOrder(customer);
        } else if ("EDIT_ORDER".equals(buttonId)) {
            showEditOrderOptions(customer);
        } else if ("EDIT_PRODUCT".equals(buttonId)) {
            showProductEditOptions(customer);
        } else if ("EDIT_QUANTITY".equals(buttonId)) {
            showQuantityEditOptions(customer);
        } else if ("BACK_TO_CHECKOUT".equals(buttonId)) {
            showCartSummary(customer);
        } else if ("REMOVE_ALL_ITEMS".equals(buttonId)) {
            handleRemoveAllItems(customer);
        } else if (buttonId.startsWith("REMOVE_ITEM_")) {
            handleRemoveItem(customer, buttonId);
        } else if (buttonId.startsWith("EDIT_QTY_")) {
            handleEditQuantitySelection(customer, buttonId);
        }
    }

    /**
     * 11. Show cart summary before checkout
     */
    private void showCartSummary(Customer customer) {
        List<CartItemDto> items = shoppingCartService.getCartItems(customer.getId());
        Double total = shoppingCartService.calculateCartTotal(customer.getId());

        if (items.isEmpty()) {
            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    "Your cart is empty! Send 'start' to browse fish.");
            customer.setCurrentFlowStage(CustomerFlowStage.REGISTERED);
            return;
        }

        StringBuilder summary = new StringBuilder("🛒 *Your Cart Summary, " + customer.getName() + ":*\n\n");
        for (CartItemDto item : items) {
            summary.append(String.format("• %s - %.2f kg × ₹%.2f = ₹%.2f\n",
                    item.getFishName(), item.getQuantityKg(), item.getPricePerKg(), item.getSubtotal()));
        }
        summary.append(String.format("\n💰 *Total Amount: ₹%.2f*\n", total));
        summary.append("💵 *Payment: COD (Cash on Delivery)*\n\n");
        summary.append("✅ Ready to confirm your order?");

        List<WhatsAppMessageDto.ButtonDto> buttons = List.of(
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("CONFIRM_ORDER")
                                .title("Confirm Order")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("EDIT_ORDER")
                                .title("Edit Order")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("CANCEL_ORDER")
                                .title("Cancel Order")
                                .build())
                        .build());

        whatsAppService.sendCartActionButtons(customer.getWaPhoneNumber(), summary.toString(), buttons);
        customer.setCurrentFlowStage(CustomerFlowStage.CHECKOUT);
    }

    /**
     * 12. Place order and send to delivery group
     */
    @Transactional
    private void placeOrder(Customer customer) {
        List<CartItemDto> items = shoppingCartService.getCartItems(customer.getId());
        Double total = shoppingCartService.calculateCartTotal(customer.getId());

        if (items.isEmpty()) {
            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    "Your cart is empty!");
            return;
        }

        // Create order
        CustomerOrder order = CustomerOrder.builder()
                .customerId(customer.getId())
                .orderTime(LocalDateTime.now())
                .totalAmount(total)
                .status(OrderStatus.PENDING)
                .paymentMethod("COD")
                .build();

        order = customerOrderRepository.save(order);

        // Create order items
        for (CartItemDto item : items) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .fishProductId(item.getFishProductId())
                    .quantityKg(item.getQuantityKg())
                    .priceAtOrder(item.getPricePerKg())
                    .build();
            orderItemRepository.save(orderItem);
        }

        // Clear cart
        shoppingCartService.clearCart(customer.getId());

        // Send confirmation to customer
        whatsAppService.sendOrderConfirmation(customer.getWaPhoneNumber(), order.getId(), total);

        // Send to delivery group
        deliveryFlowService.sendOrderToDeliveryGroup(order, customer, items);

        customer.setCurrentFlowStage(CustomerFlowStage.REGISTERED);
    }

    /**
     * Handle Cancel Order - Clear cart and reset to registered state
     */
    private void handleCancelOrder(Customer customer) {
        // Clear shopping cart
        shoppingCartService.clearCart(customer.getId());

        // Reset flow stage
        customer.setCurrentFlowStage(CustomerFlowStage.REGISTERED);

        // Send confirmation
        whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                "✅ *Order Cancelled, " + customer.getName() + "* 🙏\n\n" +
                        "🗑️ Your cart has been cleared.\n\n" +
                        "🐟 Whenever you're ready, send *'start'* to browse our fresh fish selection again! ✨");
    }

    /**
     * Show Edit Order options menu
     */
    private void showEditOrderOptions(Customer customer) {
        List<WhatsAppMessageDto.ButtonDto> buttons = List.of(
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("EDIT_PRODUCT")
                                .title("Edit Product")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("EDIT_QUANTITY")
                                .title("Edit Quantity")
                                .build())
                        .build(),
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("BACK_TO_CHECKOUT")
                                .title("Back to Checkout")
                                .build())
                        .build());

        whatsAppService.sendCartActionButtons(customer.getWaPhoneNumber(),
                "📝 *Edit Your Order, " + customer.getName() + "!*\n\n" +
                        "What would you like to modify?",
                buttons);

        customer.setCurrentFlowStage(CustomerFlowStage.EDITING_ORDER);
    }

    /**
     * Show product edit options - allow removing products from cart
     */
    private void showProductEditOptions(Customer customer) {
        List<CartItemDto> items = shoppingCartService.getCartItems(customer.getId());

        if (items.isEmpty()) {
            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    "Your cart is empty! Send 'start' to browse fish.");
            customer.setCurrentFlowStage(CustomerFlowStage.REGISTERED);
            return;
        }

        if (items.size() == 1) {
            // Single item - use buttons
            CartItemDto item = items.get(0);
            List<WhatsAppMessageDto.ButtonDto> buttons = List.of(
                    WhatsAppMessageDto.ButtonDto.builder()
                            .type("reply")
                            .reply(WhatsAppMessageDto.ReplyDto.builder()
                                    .id("REMOVE_ITEM_" + item.getFishProductId())
                                    .title("Remove " + item.getFishName())
                                    .build())
                            .build(),
                    WhatsAppMessageDto.ButtonDto.builder()
                            .type("reply")
                            .reply(WhatsAppMessageDto.ReplyDto.builder()
                                    .id("BACK_TO_CHECKOUT")
                                    .title("Back to Checkout")
                                    .build())
                            .build());

            whatsAppService.sendCartActionButtons(customer.getWaPhoneNumber(),
                    String.format("🗑️ Remove Product\n\nCurrent cart:\n• %s - %.2f kg\n\nRemove this item?",
                            item.getFishName(), item.getQuantityKg()),
                    buttons);
        } else {
            // Multiple items - use interactive list with Remove All option
            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    "🗑️ Remove Products\n\nSelect an option:");

            // Send list message (to be implemented in WhatsAppService)
            sendProductRemovalList(customer, items);
        }

        customer.setCurrentFlowStage(CustomerFlowStage.EDITING_PRODUCT);
    }

    /**
     * Send interactive list for product removal
     */
    private void sendProductRemovalList(Customer customer, List<CartItemDto> items) {
        StringBuilder message = new StringBuilder("Current cart:\n");
        for (CartItemDto item : items) {
            message.append(String.format("• %s - %.2f kg × ₹%.2f = ₹%.2f\n",
                    item.getFishName(), item.getQuantityKg(), item.getPricePerKg(), item.getSubtotal()));
        }
        message.append("\nSelect a product to remove or remove all:");

        // For now, use buttons (we'll implement interactive list in WhatsAppService
        // later)
        List<WhatsAppMessageDto.ButtonDto> buttons = new java.util.ArrayList<>();

        // Add Remove All as first button
        buttons.add(WhatsAppMessageDto.ButtonDto.builder()
                .type("reply")
                .reply(WhatsAppMessageDto.ReplyDto.builder()
                        .id("REMOVE_ALL_ITEMS")
                        .title("🗑️ Remove All")
                        .build())
                .build());

        // Add individual items (max 2 more buttons = 3 total)
        for (int i = 0; i < Math.min(items.size(), 2); i++) {
            CartItemDto item = items.get(i);
            buttons.add(WhatsAppMessageDto.ButtonDto.builder()
                    .type("reply")
                    .reply(WhatsAppMessageDto.ReplyDto.builder()
                            .id("REMOVE_ITEM_" + item.getFishProductId())
                            .title("Remove " + item.getFishName())
                            .build())
                    .build());
        }

        whatsAppService.sendCartActionButtons(customer.getWaPhoneNumber(), message.toString(), buttons);
    }

    /**
     * Show quantity edit options - allow editing quantities
     */
    private void showQuantityEditOptions(Customer customer) {
        List<CartItemDto> items = shoppingCartService.getCartItems(customer.getId());

        if (items.isEmpty()) {
            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    "Your cart is empty! Send 'start' to browse fish.");
            customer.setCurrentFlowStage(CustomerFlowStage.REGISTERED);
            return;
        }

        if (items.size() == 1) {
            // Single item - go directly to quantity input
            CartItemDto item = items.get(0);
            customer.setTempSelectedProductId(item.getFishProductId());

            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    String.format("Current quantity for %s: %.2f kg\n\n" +
                            "Enter new quantity (in kg):",
                            item.getFishName(), item.getQuantityKg()));

            customer.setCurrentFlowStage(CustomerFlowStage.AWAITING_QUANTITY);
        } else {
            // Multiple items - show buttons to select which product
            List<WhatsAppMessageDto.ButtonDto> buttons = new java.util.ArrayList<>();
            for (int i = 0; i < Math.min(items.size(), 3); i++) {
                CartItemDto item = items.get(i);
                buttons.add(WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("EDIT_QTY_" + item.getFishProductId())
                                .title("Edit " + item.getFishName())
                                .build())
                        .build());
            }

            StringBuilder message = new StringBuilder("✏️ Edit Quantities\n\n");
            message.append("Current cart:\n");
            for (CartItemDto item : items) {
                message.append(String.format("• %s - %.2f kg\n",
                        item.getFishName(), item.getQuantityKg()));
            }
            message.append("\nSelect a product to edit quantity:");

            whatsAppService.sendCartActionButtons(customer.getWaPhoneNumber(),
                    message.toString(), buttons);

            customer.setCurrentFlowStage(CustomerFlowStage.EDITING_QUANTITY);
        }
    }

    /**
     * Handle removing all items from cart
     */
    private void handleRemoveAllItems(Customer customer) {
        // Clear entire cart
        shoppingCartService.clearCart(customer.getId());

        whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                "✅ *All Products Removed!* 🙏\n\n" +
                        "🛝️ Your cart is now empty, " + customer.getName() + ".\n\n" +
                        "🐟 Here are our fresh fish available today:");
        showProductCatalog(customer);
    }

    /**
     * Handle removing an item from cart
     */
    private void handleRemoveItem(Customer customer, String buttonId) {
        Long fishProductId = Long.parseLong(buttonId.replace("REMOVE_ITEM_", ""));

        // Remove item from cart
        shoppingCartService.removeFromCart(customer.getId(), fishProductId);

        whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                "✅ *Product Removed Successfully!* 👍\n\n" +
                        "Item has been removed from your cart. ✨");

        // Check if cart is now empty
        List<CartItemDto> remainingItems = shoppingCartService.getCartItems(customer.getId());

        if (remainingItems.isEmpty()) {
            // Cart is empty - show product catalog directly
            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    "Your cart is now empty. Here are our available products:");
            showProductCatalog(customer);
        } else {
            // Show updated cart summary
            showCartSummary(customer);
        }
    }

    /**
     * Handle selecting a product to edit quantity
     */
    private void handleEditQuantitySelection(Customer customer, String buttonId) {
        Long fishProductId = Long.parseLong(buttonId.replace("EDIT_QTY_", ""));

        // Store the product ID for quantity update
        customer.setTempSelectedProductId(fishProductId);

        // Get current quantity
        List<CartItemDto> items = shoppingCartService.getCartItems(customer.getId());
        CartItemDto item = items.stream()
                .filter(i -> i.getFishProductId().equals(fishProductId))
                .findFirst()
                .orElse(null);

        if (item != null) {
            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    String.format("Current quantity for %s: %.2f kg\n\n" +
                            "Enter new quantity (in kg):",
                            item.getFishName(), item.getQuantityKg()));

            customer.setCurrentFlowStage(CustomerFlowStage.AWAITING_QUANTITY);
        }
    }

}
