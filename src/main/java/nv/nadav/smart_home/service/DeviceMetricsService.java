package nv.nadav.smart_home.service;

import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.model.DeviceType;

public interface DeviceMetricsService {
    void addDevice(DeviceDto device);

    void updateDevice(DeviceUpdateDto oldValues, DeviceUpdateDto update, DeviceType type, String deviceId);

    void deleteDevice(String deviceId);
}
