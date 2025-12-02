package com.fishseller.whatsappservice.service;

import com.fishseller.whatsappservice.model.DeliveryPerson;
import com.fishseller.whatsappservice.repository.DeliveryPersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryPersonService {

    private final DeliveryPersonRepository deliveryPersonRepository;

    public List<DeliveryPerson> getAllDeliveryPersons() {
        return deliveryPersonRepository.findAll();
    }

    public Optional<DeliveryPerson> getDeliveryPersonById(Long id) {
        return deliveryPersonRepository.findById(id);
    }

    public Optional<DeliveryPerson> getDeliveryPersonByWaId(String waId) {
        return deliveryPersonRepository.findByWaPhoneNumber(waId);
    }

    @Transactional
    public DeliveryPerson createDeliveryPerson(DeliveryPerson deliveryPerson) {
        log.info("Creating delivery person: name={}, phone={}", deliveryPerson.getName(),
                deliveryPerson.getPhoneNumber());
        return deliveryPersonRepository.save(deliveryPerson);
    }

    @Transactional
    public DeliveryPerson getOrCreateDeliveryPerson(String waId, String defaultName) {
        return deliveryPersonRepository.findByWaPhoneNumber(waId)
                .orElseGet(() -> {
                    DeliveryPerson dp = DeliveryPerson.builder()
                            .waPhoneNumber(waId)
                            .name(defaultName)
                            .phoneNumber(waId) // Defaulting phone number to WA ID if not provided separately
                            .build();
                    log.info("Created new delivery person for WA ID: {}", waId);
                    return deliveryPersonRepository.save(dp);
                });
    }

    @Transactional
    public DeliveryPerson updateDeliveryPerson(Long id, DeliveryPerson details) {
        return deliveryPersonRepository.findById(id)
                .map(person -> {
                    person.setName(details.getName());
                    person.setPhoneNumber(details.getPhoneNumber());
                    person.setActive(details.isActive());
                    log.info("Updated delivery person id={}", id);
                    return deliveryPersonRepository.save(person);
                })
                .orElseThrow(() -> new RuntimeException("Delivery person not found with id: " + id));
    }

    @Transactional
    public void deleteDeliveryPerson(Long id) {
        if (deliveryPersonRepository.existsById(id)) {
            deliveryPersonRepository.deleteById(id);
            log.info("Deleted delivery person with id={}", id);
        } else {
            throw new RuntimeException("Delivery person not found with id: " + id);
        }
    }
}
