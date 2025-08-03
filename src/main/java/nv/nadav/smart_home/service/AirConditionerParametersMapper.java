package nv.nadav.smart_home.service;

import nv.nadav.smart_home.model.parameters.AirConditionerParameters;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AirConditionerParametersMapper {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromOther(AirConditionerParameters source, @MappingTarget AirConditionerParameters target);
}
