package nv.nadav.smart_home.service;

import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.dto.DeviceUpdateDto;

import java.util.List;

public interface DeviceService {
    DeviceDto addDevice(DeviceDto deviceDto);

    DeviceDto updateDevice(String deviceId, DeviceUpdateDto deviceDto);

    DeviceDto getDeviceById(String deviceId);

    List<DeviceDto> getAllDevices();

    void deleteDeviceById(String deviceId);

    List<String> getDeviceIds();

    boolean existsByDeviceId(String deviceId);

}
