package com.fishseller.whatsappservice.service;

import com.fishseller.whatsappservice.config.WhatsAppConfig;
import com.fishseller.whatsappservice.dto.CartItemDto;
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
    private final TeamMemberRepository teamMemberRepository;
    private final WhatsAppService whatsAppService;
    private final WhatsAppConfig whatsAppConfig;
    private final LocationValidationService locationValidationService;
    private final ShoppingCartService shoppingCartService;
    private final ExecutiveFlowService executiveFlowService;

    /**
     * Main entry point for processing incoming WhatsApp messages
     * Routes messages based on sender role: TeamMember (Executive/Delivery) or
     * Customer
     */
    @Transactional
    public void processIncomingMessage(WhatsAppWebhookDto.Value messageValue) {
        if (messageValue.getMessages() == null || messageValue.getMessages().isEmpty()) {
            return;
        }

        WhatsAppWebhookDto.Message message = messageValue.getMessages().get(0);
        String fromWaId = message.getFrom();

        log.info("Processing message from: {}", fromWaId);

        // Check if sender is a team member (Executive or Delivery Person)
        Optional<TeamMember> teamMemberOpt = teamMemberRepository.findByWaPhoneNumber(fromWaId);

        if (teamMemberOpt.isPresent()) {
            TeamMember teamMember = teamMemberOpt.get();

            if (teamMember.getRole() == UserRole.EXECUTIVE) {
                // Route to Executive Flow
                log.info("Routing to Executive Flow for: {}", teamMember.getName());
                executiveFlowService.handleExecutiveMessage(teamMember, message);
                return;
            } else if (teamMember.getRole() == UserRole.DELIVERY_PERSON) {
                // Handle delivery person button replies (order confirmation)
                if (message.getType().equals("interactive") &&
                        message.getInteractive().getType().equals("button_reply")) {
                    WhatsAppWebhookDto.ButtonReply buttonReply = message.getInteractive().getButtonReply();
                    if (buttonReply.getId().startsWith("DELIVERY_TAKE_")) {
                        Long orderId = Long.parseLong(buttonReply.getId().replace("DELIVERY_TAKE_", ""));
                        handleDeliveryConfirmation(fromWaId, orderId);
                        return;
                    }
                }
                // For other messages from delivery persons, just acknowledge
                whatsAppService.sendSimpleText(fromWaId,
                        "Thank you! Please use the order confirmation buttons in the group.");
                return;
            }
        }

        // If not a team member, treat as customer
        Customer customer = customerRepository.findByWaPhoneNumber(fromWaId)
                .orElseGet(() -> {
                    Customer newCustomer = Customer.builder()
                            .waPhoneNumber(fromWaId)
                            .currentFlowStage(CustomerFlowStage.NEW)
                            .build();
                    return customerRepository.save(newCustomer);
                });

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
                    "Welcome to our Fresh Fish Store! 🐟\n\nWhat's your name?");
            customer.setCurrentFlowStage(CustomerFlowStage.AWAITING_NAME);
        } else {
            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    "Hi! Welcome to our Fresh Fish Store! 🐟\n\nSend 'Hi' to get started.");
        }
    }

    /**
     * 2. Collect customer name
     */
    private void handleAwaitingName(Customer customer, String text) {
        customer.setName(text.trim());
        whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                "Nice to meet you, " + text.trim() + "! 👋\n\nPlease share your phone number.");
        customer.setCurrentFlowStage(CustomerFlowStage.AWAITING_PHONE);
    }

    /**
     * 3. Collect phone number and request location
     */
    private void handleAwaitingPhone(Customer customer, String text) {
        customer.setPhoneNumber(text.trim());
        whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                "Great! Now please share your current location so we can check if we deliver to your area.\n\n" +
                        "📍 Tap the attachment icon (📎) → Location → Send your current location");
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
                    String.format("✅ Great! You're in our delivery area!\n\n" +
                            "Distance from our store: %.2f km\n\n" +
                            "You've been added to our customer list! 🎉\n\n" +
                            "Send 'start' to see our latest fresh fish selection.", distance));
        } else {
            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    String.format("❌ Sorry, we cannot deliver to your location.\n\n" +
                            "You're %.2f km away from us. " +
                            "We currently deliver within 50 km radius only.\n\n" +
                            "We hope to expand to your area soon! 🙏", distance));
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
                    "Send 'start' to browse our fresh fish selection! 🐟");
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
                    "Sorry, no fish available at the moment. Please check back later!");
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
                "🐟 Fresh Fish Available Today:\n\nSelect a fish to add to your cart:", rows);

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
                    "Sorry, that fish is no longer available.");
            return;
        }

        FishProduct fish = fishOpt.get();

        // Store selected product temporarily
        customer.setTempSelectedProductId(fishProductId);
        customer.setCurrentFlowStage(CustomerFlowStage.AWAITING_QUANTITY);

        whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                String.format("You selected: %s (₹%.2f/kg)\n\nHow many KG do you need?",
                        fish.getName(), fish.getPricePerKg()));
    }

    /**
     * 8. Handle quantity input and add to cart
     */
    private void handleAwaitingQuantity(Customer customer, String text) {
        try {
            Double quantity = Double.parseDouble(text.trim());

            if (quantity <= 0) {
                whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                        "Please enter a valid quantity greater than 0.");
                return;
            }

            Long fishProductId = customer.getTempSelectedProductId();

            // Add to cart
            shoppingCartService.addToCart(customer.getId(), fishProductId, quantity);

            // Show cart options
            sendCartOptions(customer);
            customer.setCurrentFlowStage(CustomerFlowStage.ADDING_TO_CART);

        } catch (NumberFormatException e) {
            whatsAppService.sendSimpleText(customer.getWaPhoneNumber(),
                    "Please enter a valid number (e.g., 2 or 1.5)");
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
                "✅ Added to cart!\n\nWhat would you like to do next?", buttons);
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
        } else if (buttonId.startsWith("DELIVERY_TAKE_")) {
            Long orderId = Long.parseLong(buttonId.replace("DELIVERY_TAKE_", ""));
            handleDeliveryConfirmation(customer.getWaPhoneNumber(), orderId);
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

        StringBuilder summary = new StringBuilder("🛒 Your Cart:\n\n");
        for (CartItemDto item : items) {
            summary.append(String.format("• %s - %.2f kg × ₹%.2f = ₹%.2f\n",
                    item.getFishName(), item.getQuantityKg(), item.getPricePerKg(), item.getSubtotal()));
        }
        summary.append(String.format("\n💰 Total: ₹%.2f\n", total));
        summary.append("💵 Payment: COD (Cash on Delivery)\n\n");
        summary.append("Confirm your order?");

        List<WhatsAppMessageDto.ButtonDto> buttons = List.of(
                WhatsAppMessageDto.ButtonDto.builder()
                        .type("reply")
                        .reply(WhatsAppMessageDto.ReplyDto.builder()
                                .id("CONFIRM_ORDER")
                                .title("Confirm Order")
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
        sendOrderToDeliveryGroup(order, customer, items);

        customer.setCurrentFlowStage(CustomerFlowStage.REGISTERED);
    }

    /**
     * 13. Send order to delivery group with confirm button
     */
    private void sendOrderToDeliveryGroup(CustomerOrder order, Customer customer, List<CartItemDto> items) {
        String groupId = whatsAppConfig.getDeliveryGroupId();

        if (groupId == null || groupId.isEmpty()) {
            log.warn("Delivery group ID not configured!");
            return;
        }

        StringBuilder orderDetails = new StringBuilder();
        orderDetails.append("🔔 NEW ORDER #").append(order.getId()).append("\n\n");
        orderDetails.append("👤 Customer: ").append(customer.getName()).append("\n");
        orderDetails.append("📞 Phone: ").append(customer.getPhoneNumber()).append("\n");
        orderDetails.append("📍 Location: ")
                .append(String.format("%.5f, %.5f", customer.getLocationLat(), customer.getLocationLon())).append("\n");
        orderDetails.append("📏 Distance: ").append(String.format("%.2f km", customer.getDistanceFromBusinessKm()))
                .append("\n\n");

        orderDetails.append("🐟 Items:\n");
        for (CartItemDto item : items) {
            orderDetails.append(String.format("• %s - %.2f kg × ₹%.2f = ₹%.2f\n",
                    item.getFishName(), item.getQuantityKg(), item.getPricePerKg(), item.getSubtotal()));
        }

        orderDetails.append(String.format("\n💰 Total: ₹%.2f\n", order.getTotalAmount()));
        orderDetails.append("💵 Payment: COD\n");
        orderDetails.append("⏰ Time: ").append(order.getOrderTime().toString()).append("\n\n");
        orderDetails.append("Who is willing to take this order?");

        whatsAppService.sendInteractiveOrderAlert(groupId, orderDetails.toString(), order.getId());
    }

    /**
     * 14. Handle delivery person confirmation
     */
    @Transactional
    public void handleDeliveryConfirmation(String teamMemberWaId, Long orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            whatsAppService.sendSimpleText(teamMemberWaId,
                    "This order has already been taken by " + order.getTeamMemberName());
            return;
        }

        // Get or create team member (delivery person)
        TeamMember teamMember = teamMemberRepository
                .findByWaPhoneNumber(teamMemberWaId)
                .orElseGet(() -> {
                    TeamMember tm = TeamMember.builder()
                            .waPhoneNumber(teamMemberWaId)
                            .name("Delivery Person "
                                    + teamMemberWaId.substring(0, Math.min(10, teamMemberWaId.length())))
                            .role(UserRole.DELIVERY_PERSON)
                            .build();
                    return teamMemberRepository.save(tm);
                });

        // Update order
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTeamMemberId(teamMember.getId());
        order.setTeamMemberWaId(teamMemberWaId);
        order.setTeamMemberName(teamMember.getName());
        order.setConfirmedAt(LocalDateTime.now());
        customerOrderRepository.save(order);

        // Notify customer
        Customer customer = customerRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        whatsAppService.sendDeliveryAssignmentNotification(
                customer.getWaPhoneNumber(),
                teamMember.getName());

        // Notify group
        String groupId = whatsAppConfig.getDeliveryGroupId();
        whatsAppService.sendSimpleText(groupId,
                "✅ Order #" + orderId + " taken by " + teamMember.getName());

        log.info("Order {} assigned to team member {}", orderId, teamMember.getName());
    }
}
