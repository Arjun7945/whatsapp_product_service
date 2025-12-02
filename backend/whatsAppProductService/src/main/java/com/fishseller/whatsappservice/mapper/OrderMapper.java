package com.fishseller.whatsappservice.mapper;

import com.fishseller.whatsappservice.dto.CustomerOrderDto;
import com.fishseller.whatsappservice.dto.OrderItemDto;
import com.fishseller.whatsappservice.model.CustomerOrder;
import com.fishseller.whatsappservice.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "customerName", ignore = true) // Populated manually in Controller/Service
    CustomerOrderDto toDto(CustomerOrder order);

    @Mapping(target = "fishProductName", ignore = true) // Populated manually
    @Mapping(target = "quantity", source = "quantityKg")
    @Mapping(target = "pricePerKg", source = "priceAtOrder")
    @Mapping(target = "totalPrice", expression = "java(item.getQuantityKg() * item.getPriceAtOrder())")
    OrderItemDto toDto(OrderItem item);
}
