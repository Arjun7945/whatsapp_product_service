package com.fishseller.whatsappservice.controller;

import com.fishseller.whatsappservice.model.DeliveryPerson;
import com.fishseller.whatsappservice.model.FishProduct;
import com.fishseller.whatsappservice.repository.DeliveryPersonRepository;
import com.fishseller.whatsappservice.repository.FishProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final FishProductRepository fishProductRepository;
    private final DeliveryPersonRepository deliveryPersonRepository;
    private final com.fishseller.whatsappservice.repository.CustomerOrderRepository customerOrderRepository;
    private final com.fishseller.whatsappservice.repository.CustomerRepository customerRepository;

    // --- Fish Product Management ---

    @GetMapping("/fish")
    public List<FishProduct> getAllFish() {
        return fishProductRepository.findAll();
    }

    @PostMapping("/fish")
    public FishProduct createFish(@RequestBody FishProduct fishProduct) {
        log.info("Creating fish product: name={}, pricePerKg={}, isAvailable={}",
                fishProduct.getName(), fishProduct.getPricePerKg(), fishProduct.isAvailable());
        FishProduct saved = fishProductRepository.save(fishProduct);
        log.info("Saved fish product with id={}, isAvailable={}", saved.getId(), saved.isAvailable());
        return saved;
    }

    @PutMapping("/fish/{id}")
    public ResponseEntity<FishProduct> updateFish(@PathVariable Long id, @RequestBody FishProduct fishDetails) {
        log.info("Updating fish product id={}: name={}, pricePerKg={}, isAvailable={}",
                id, fishDetails.getName(), fishDetails.getPricePerKg(), fishDetails.isAvailable());
        return fishProductRepository.findById(id)
                .map(fish -> {
                    fish.setName(fishDetails.getName());
                    fish.setPricePerKg(fishDetails.getPricePerKg());
                    fish.setImageURL(fishDetails.getImageURL());
                    fish.setDescription(fishDetails.getDescription());
                    fish.setAvailable(fishDetails.isAvailable());
                    FishProduct updated = fishProductRepository.save(fish);
                    log.info("Updated fish product id={}, isAvailable={}", updated.getId(), updated.isAvailable());
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/fish/{id}")
    public ResponseEntity<Void> deleteFish(@PathVariable Long id) {
        if (fishProductRepository.existsById(id)) {
            fishProductRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // --- Delivery Person Management ---

    @GetMapping("/delivery")
    public List<DeliveryPerson> getAllDeliveryPersons() {
        return deliveryPersonRepository.findAll();
    }

    @PostMapping("/delivery")
    public DeliveryPerson createDeliveryPerson(@RequestBody DeliveryPerson deliveryPerson) {
        return deliveryPersonRepository.save(deliveryPerson);
    }

    @PutMapping("/delivery/{id}")
    public ResponseEntity<DeliveryPerson> updateDeliveryPerson(@PathVariable Long id,
            @RequestBody DeliveryPerson details) {
        return deliveryPersonRepository.findById(id)
                .map(person -> {
                    person.setName(details.getName());
                    person.setPhoneNumber(details.getPhoneNumber());
                    person.setActive(details.isActive());
                    return ResponseEntity.ok(deliveryPersonRepository.save(person));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delivery/{id}")
    public ResponseEntity<Void> deleteDeliveryPerson(@PathVariable Long id) {
        if (deliveryPersonRepository.existsById(id)) {
            deliveryPersonRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // --- Order Management ---

    @GetMapping("/orders")
    public List<com.fishseller.whatsappservice.dto.CustomerOrderDto> getAllOrders() {
        return customerOrderRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<com.fishseller.whatsappservice.dto.CustomerOrderDto> getOrderById(@PathVariable Long id) {
        return customerOrderRepository.findById(id)
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private com.fishseller.whatsappservice.dto.CustomerOrderDto mapToDto(
            com.fishseller.whatsappservice.model.CustomerOrder order) {
        String customerName = "Unknown Customer";
        try {
            customerName = customerRepository.findById(order.getCustomerId())
                    .map(com.fishseller.whatsappservice.model.Customer::getName)
                    .orElse("Unknown Customer");
        } catch (Exception e) {
            log.error("Error fetching customer name for order {}", order.getId(), e);
        }

        List<com.fishseller.whatsappservice.dto.OrderItemDto> items = order.getItems().stream()
                .map(item -> {
                    String fishName = "Unknown Fish";
                    try {
                        fishName = fishProductRepository.findById(item.getFishProductId())
                                .map(FishProduct::getName)
                                .orElse("Unknown Fish");
                    } catch (Exception e) {
                        log.error("Error fetching fish name for item {}", item.getId(), e);
                    }

                    return com.fishseller.whatsappservice.dto.OrderItemDto.builder()
                            .id(item.getId())
                            .fishProductId(item.getFishProductId())
                            .fishProductName(fishName)
                            .quantity(item.getQuantityKg())
                            .pricePerKg(item.getPriceAtOrder())
                            .totalPrice(item.getQuantityKg() * item.getPriceAtOrder())
                            .build();
                })
                .collect(java.util.stream.Collectors.toList());

        return com.fishseller.whatsappservice.dto.CustomerOrderDto.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .customerName(customerName)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .deliveryPersonId(order.getDeliveryPersonId())
                .items(items)
                .build();
    }
}
