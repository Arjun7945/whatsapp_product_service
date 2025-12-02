package com.fishseller.whatsappservice.mapper;

import com.fishseller.whatsappservice.model.FishProduct;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    void updateFishFromDto(FishProduct dto, @MappingTarget FishProduct entity);
}
