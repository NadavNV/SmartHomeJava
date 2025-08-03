package nv.nadav.smart_home.service;

import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.model.Device;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DeviceMapper {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "id", target = "deviceId")
    @Mapping(target = "parameters", ignore = true)
    void updateDeviceFromDto(DeviceUpdateDto dto, @MappingTarget Device device);
}
