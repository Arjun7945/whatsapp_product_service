package com.fishseller.whatsappservice.service;

import com.fishseller.whatsappservice.config.WhatsAppConfig;
import com.fishseller.whatsappservice.dto.CartItemDto;
import com.fishseller.whatsappservice.model.Customer;
import com.fishseller.whatsappservice.model.CustomerOrder;
import com.fishseller.whatsappservice.model.TeamMember;
import com.fishseller.whatsappservice.model.enums.OrderStatus;
import com.fishseller.whatsappservice.model.enums.UserRole;
import com.fishseller.whatsappservice.repository.CustomerOrderRepository;
import com.fishseller.whatsappservice.repository.CustomerRepository;
import com.fishseller.whatsappservice.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service to handle delivery person operations and order assignment flow
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryFlowService {

        private final TeamMemberRepository teamMemberRepository;
        private final CustomerOrderRepository customerOrderRepository;
        private final CustomerRepository customerRepository;
        private final WhatsAppService whatsAppService;
        private final WhatsAppConfig whatsAppConfig;

        /**
         * Send order notification to delivery group with confirm button
         */
        public void sendOrderToDeliveryGroup(CustomerOrder order, Customer customer, List<CartItemDto> items) {
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
                                .append(String.format("%.5f, %.5f", customer.getLocationLat(),
                                                customer.getLocationLon()))
                                .append("\n");
                orderDetails.append("📏 Distance: ")
                                .append(String.format("%.2f km", customer.getDistanceFromBusinessKm()))
                                .append("\n\n");

                orderDetails.append("🐟 Items:\n");
                for (CartItemDto item : items) {
                        orderDetails.append(String.format("• %s - %.2f kg × ₹%.2f = ₹%.2f\n",
                                        item.getFishName(), item.getQuantityKg(), item.getPricePerKg(),
                                        item.getSubtotal()));
                }

                orderDetails.append(String.format("\n💰 Total: ₹%.2f\n", order.getTotalAmount()));
                orderDetails.append("💵 Payment: COD\n");
                orderDetails.append("⏰ Time: ").append(order.getOrderTime().toString()).append("\n\n");
                orderDetails.append("Who is willing to take this order?");

                whatsAppService.sendInteractiveOrderAlert(groupId, orderDetails.toString(), order.getId());
                log.info("Order {} sent to delivery group {}", order.getId(), groupId);
        }

        /**
         * Handle delivery person confirmation with role validation
         */
        @Transactional
        public void handleDeliveryConfirmation(String deliveryPersonWaId, Long orderId) {
                // 1. Fetch order
                CustomerOrder order = customerOrderRepository.findById(orderId)
                                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

                // 2. Check if order already confirmed
                if (order.getStatus() == OrderStatus.CONFIRMED) {
                        whatsAppService.sendSimpleText(deliveryPersonWaId,
                                        "This order has already been taken by " + order.getTeamMemberName());
                        return;
                }

                // 3. Validate delivery person role
                Optional<TeamMember> teamMemberOpt = teamMemberRepository.findByWaPhoneNumber(deliveryPersonWaId);

                if (teamMemberOpt.isEmpty()) {
                        // User not registered in system
                        whatsAppService.sendUnauthorizedDeliveryMessage(deliveryPersonWaId);
                        log.warn("Unauthorized delivery confirmation attempt by unregistered user: {}",
                                        deliveryPersonWaId);
                        return;
                }

                TeamMember teamMember = teamMemberOpt.get();

                // Check if user has DELIVERY_PERSON role
                if (teamMember.getRole() != UserRole.DELIVERY_PERSON) {
                        whatsAppService.sendUnauthorizedDeliveryMessage(deliveryPersonWaId);
                        log.warn("Unauthorized delivery confirmation attempt by user {} with role: {}",
                                        deliveryPersonWaId, teamMember.getRole());
                        return;
                }

                // 4. Update order with delivery person details
                order.setStatus(OrderStatus.CONFIRMED);
                order.setTeamMemberId(teamMember.getId());
                order.setTeamMemberWaId(deliveryPersonWaId);
                order.setTeamMemberName(teamMember.getName());
                order.setConfirmedAt(LocalDateTime.now());
                customerOrderRepository.save(order);

                // 5. Notify customer
                Customer customer = customerRepository.findById(order.getCustomerId())
                                .orElseThrow(() -> new RuntimeException("Customer not found"));

                whatsAppService.sendDeliveryAssignmentNotification(
                                customer.getWaPhoneNumber(),
                                teamMember.getName());

                // 6. Notify delivery group with enhanced message
                String groupId = whatsAppConfig.getDeliveryGroupId();
                whatsAppService.sendDeliveryConfirmationToGroup(
                                groupId,
                                order.getId(),
                                teamMember.getName(),
                                teamMember.getPhoneNumber(),
                                order.getConfirmedAt());

                log.info("Order {} assigned to delivery person {} ({})",
                                orderId, teamMember.getName(), teamMember.getPhoneNumber());
        }
}
