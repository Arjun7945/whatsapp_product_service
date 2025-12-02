package com.fishseller.whatsappservice.service;

import com.fishseller.whatsappservice.model.FishProduct;
import com.fishseller.whatsappservice.repository.FishProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FishProductService {

    private final FishProductRepository fishProductRepository;

    public List<FishProduct> getAllFish() {
        return fishProductRepository.findAll();
    }

    public List<FishProduct> getAvailableFish() {
        return fishProductRepository.findByIsAvailableTrue();
    }

    public Optional<FishProduct> getFishById(Long id) {
        return fishProductRepository.findById(id);
    }

    @Transactional
    public FishProduct createFish(FishProduct fishProduct) {
        log.info("Creating fish product: name={}, pricePerKg={}, isAvailable={}",
                fishProduct.getName(), fishProduct.getPricePerKg(), fishProduct.isAvailable());
        FishProduct saved = fishProductRepository.save(fishProduct);
        log.info("Saved fish product with id={}, isAvailable={}", saved.getId(), saved.isAvailable());
        return saved;
    }

    @Transactional
    public FishProduct updateFish(Long id, FishProduct fishDetails) {
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
                    return updated;
                })
                .orElseThrow(() -> new RuntimeException("Fish product not found with id: " + id));
    }

    @Transactional
    public void deleteFish(Long id) {
        if (fishProductRepository.existsById(id)) {
            fishProductRepository.deleteById(id);
            log.info("Deleted fish product with id={}", id);
        } else {
            throw new RuntimeException("Fish product not found with id: " + id);
        }
    }
}
