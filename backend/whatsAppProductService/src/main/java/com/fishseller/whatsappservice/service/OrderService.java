package com.fishseller.whatsappservice.service;

import com.fishseller.whatsappservice.dto.CartItemDto;
import com.fishseller.whatsappservice.model.*;
import com.fishseller.whatsappservice.model.enums.OrderStatus;
import com.fishseller.whatsappservice.repository.CustomerOrderRepository;
import com.fishseller.whatsappservice.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShoppingCartService shoppingCartService;
    private final DeliveryPersonService deliveryPersonService;

    public List<CustomerOrder> getAllOrders() {
        return customerOrderRepository.findAll();
    }

    public Optional<CustomerOrder> getOrderById(Long id) {
        return customerOrderRepository.findById(id);
    }

    @Transactional
    public CustomerOrder createOrder(Customer customer) {
        List<CartItemDto> items = shoppingCartService.getCartItems(customer.getId());
        Double total = shoppingCartService.calculateCartTotal(customer.getId());

        if (items.isEmpty()) {
            throw new RuntimeException("Cannot create order with empty cart");
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

        log.info("Created order #{} for customer {}", order.getId(), customer.getId());
        return order;
    }

    @Transactional
    public CustomerOrder assignDeliveryPerson(Long orderId, String deliveryPersonWaId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order already assigned to " + order.getDeliveryPersonName());
        }

        DeliveryPerson deliveryPerson = deliveryPersonService.getOrCreateDeliveryPerson(
                deliveryPersonWaId,
                "Delivery Person " + deliveryPersonWaId.substring(0, Math.min(10, deliveryPersonWaId.length())));

        order.setStatus(OrderStatus.CONFIRMED);
        order.setDeliveryPersonId(deliveryPerson.getId());
        order.setDeliveryPersonWaId(deliveryPersonWaId);
        order.setDeliveryPersonName(deliveryPerson.getName());
        order.setConfirmedAt(LocalDateTime.now());

        log.info("Assigned order #{} to delivery person {}", orderId, deliveryPerson.getName());
        return customerOrderRepository.save(order);
    }
}
