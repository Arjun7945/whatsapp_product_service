package com.fishseller.whatsappservice.repository;

import com.fishseller.whatsappservice.model.FishProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FishProductRepository extends JpaRepository<FishProduct, Long> {
    List<FishProduct> findByIsAvailableTrue();
}
