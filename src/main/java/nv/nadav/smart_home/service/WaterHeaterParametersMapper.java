package nv.nadav.smart_home.service;

import nv.nadav.smart_home.model.parameters.WaterHeaterParameters;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WaterHeaterParametersMapper {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromOther(WaterHeaterParameters source, @MappingTarget WaterHeaterParameters target);
}
