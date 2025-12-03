package com.fishseller.whatsappservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fish_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FishProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double pricePerKg;

    private String imageURL;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    @JsonProperty("isAvailable")
    private boolean isAvailable;
}
