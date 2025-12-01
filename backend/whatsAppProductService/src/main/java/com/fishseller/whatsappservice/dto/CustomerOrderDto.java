package com.fishseller.whatsappservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomerOrderDto {
    private Long id;
    private Long customerId;
    private String customerName;
    private Double totalAmount;
    private String status;
    private Long deliveryPersonId;
    private List<OrderItemDto> items;
}
