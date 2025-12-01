package com.fishseller.whatsappservice.repository;

import com.fishseller.whatsappservice.model.DeliveryPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryPersonRepository extends JpaRepository<DeliveryPerson, Long> {

    /**
     * Find delivery person by WhatsApp phone number
     */
    Optional<DeliveryPerson> findByWaPhoneNumber(String waPhoneNumber);

    Optional<DeliveryPerson> findByPhoneNumber(String phoneNumber);

    List<DeliveryPerson> findByIsActiveTrue();
}
