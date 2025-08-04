package nv.nadav.smart_home.service.impl;

import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.exception.DeviceExistsException;
import nv.nadav.smart_home.exception.DeviceNotFoundException;
import nv.nadav.smart_home.exception.DeviceValidationException;
import nv.nadav.smart_home.model.Device;
import nv.nadav.smart_home.model.parameters.*;
import nv.nadav.smart_home.repository.DeviceRepository;
import nv.nadav.smart_home.service.*;
import nv.nadav.smart_home.validation.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository repo;
    private final DeviceMapper deviceMapper;
    private final AirConditionerParametersMapper airConditionerParametersMapper;
    private final CurtainParametersMapper curtainParametersMapper;
    private final DoorLockParametersMapper doorLockParametersMapper;
    private final LightParametersMapper lightParametersMapper;
    private final WaterHeaterParametersMapper waterHeaterParametersMapper;


    @Autowired
    public DeviceServiceImpl(
            DeviceRepository deviceRepository,
            DeviceMapper deviceMapper,
            AirConditionerParametersMapper airConditionerParametersMapper,
            CurtainParametersMapper curtainParametersMapper,
            DoorLockParametersMapper doorLockParametersMapper,
            LightParametersMapper lightParametersMapper,
            WaterHeaterParametersMapper waterHeaterParametersMapper
    ) {
        this.repo = deviceRepository;
        this.deviceMapper = deviceMapper;
        this.airConditionerParametersMapper = airConditionerParametersMapper;
        this.curtainParametersMapper = curtainParametersMapper;
        this.doorLockParametersMapper = doorLockParametersMapper;
        this.lightParametersMapper = lightParametersMapper;
        this.waterHeaterParametersMapper = waterHeaterParametersMapper;
    }

    private static DeviceDto mapToDto(Device device) {
        DeviceDto deviceDto = new DeviceDto();
        deviceDto.setId(device.getDeviceId());
        deviceDto.setName(device.getName());
        deviceDto.setParameters(device.getParameters());
        deviceDto.setRoom(device.getRoom());
        deviceDto.setType(device.getType());
        deviceDto.setStatus(device.getStatus());
        return deviceDto;
    }

    @Override
    public DeviceDto addDevice(DeviceDto deviceDto) {
        if (repo.existsByDeviceId(deviceDto.getId())) {
            throw new DeviceExistsException();
        }
        Validators.ValidationResult validationResult = Validators.validateNewDeviceData(deviceDto);
        if (validationResult.isValid()) {
            Device newDevice = Device.fromDto(deviceDto);
            newDevice = repo.insert(newDevice);
            return mapToDto(newDevice);
        } else {
            throw new DeviceValidationException(validationResult.errorMessages());
        }
    }

    @Override
    public DeviceDto updateDevice(String deviceId, DeviceUpdateDto deviceDto) {
        Device device = repo.findByDeviceId(deviceId).orElseThrow(() ->
                new DeviceNotFoundException(String.format("Device ID %s not found", deviceId)));
        Validators.ValidationResult validationResult = Validators.validateDeviceData(deviceDto, device.getType());
        if (validationResult.isValid()) {
            deviceMapper.updateDeviceFromDto(deviceDto, device);
            DeviceParameters parametersUpdate = deviceDto.getParameters();
            DeviceParameters currentParameters = device.getParameters();
            if (parametersUpdate != null) {
                try {
                    switch (device.getType()) {
                        case LIGHT -> lightParametersMapper.updateFromOther(
                                (LightParameters) parametersUpdate,
                                (LightParameters) currentParameters
                        );
                        case WATER_HEATER -> waterHeaterParametersMapper.updateFromOther(
                                (WaterHeaterParameters) parametersUpdate,
                                (WaterHeaterParameters) currentParameters
                        );
                        case CURTAIN -> curtainParametersMapper.updateFromOther(
                                (CurtainParameters) parametersUpdate,
                                (CurtainParameters) currentParameters
                        );
                        case DOOR_LOCK -> doorLockParametersMapper.updateFromOther(
                                (DoorLockParameters) parametersUpdate,
                                (DoorLockParameters) currentParameters
                        );
                        case AIR_CONDITIONER -> airConditionerParametersMapper.updateFromOther(
                                (AirConditionerParameters) parametersUpdate,
                                (AirConditionerParameters) currentParameters
                        );
                    }
                } catch (ClassCastException e) {
                    throw new DeviceValidationException(String.format("Incorrect parameters for device type %s", device.getType()));
                }
            }
            device = repo.save(device);
            return mapToDto(device);
        } else {
            throw new DeviceValidationException(validationResult.errorMessages());
        }
    }

    @Override
    public DeviceDto getDeviceById(String deviceId) {
        Device device = repo.findByDeviceId(deviceId).orElseThrow(() ->
                new DeviceNotFoundException(String.format("Device ID %s not found", deviceId)));
        return mapToDto(device);
    }

    @Override
    public List<DeviceDto> getAllDevices() {
        List<Device> devices = repo.findAll();
        return devices.stream().map(DeviceServiceImpl::mapToDto).toList();
    }

    @Override
    public void deleteDeviceById(String deviceId) {
        Device device = repo.findByDeviceId(deviceId).orElseThrow(() ->
                new DeviceNotFoundException(String.format("Device ID %s not found", deviceId)));
        repo.delete(device);
    }

    @Override
    public List<String> getDeviceIds() {
        return repo.getDeviceIds();
    }

    @Override
    public boolean existsByDeviceId(String deviceId) {
        return repo.existsByDeviceId(deviceId);
    }
}
