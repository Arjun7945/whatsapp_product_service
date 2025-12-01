package com.fishseller.whatsappservice.model;

import com.fishseller.whatsappservice.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customer_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId; // Foreign key to Customer

    @Column(nullable = false)
    private LocalDateTime orderTime;

    @Column(nullable = false)
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private String paymentMethod = "COD"; // Default to Cash on Delivery

    private Long deliveryPersonId; // Assigned delivery person ID

    private String deliveryPersonWaId; // WhatsApp ID of delivery person

    private String deliveryPersonName; // Name of delivery person

    private LocalDateTime confirmedAt; // When delivery person confirmed

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;
}
