package nv.nadav.smart_home.service.impl;


import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.exception.DeviceExistsException;
import nv.nadav.smart_home.exception.DeviceNotFoundException;
import nv.nadav.smart_home.exception.DeviceValidationException;
import nv.nadav.smart_home.model.Device;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.model.parameters.CurtainParameters;
import nv.nadav.smart_home.model.parameters.LightParameters;
import nv.nadav.smart_home.repository.DeviceRepository;
import nv.nadav.smart_home.service.*;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static nv.nadav.smart_home.constants.Constants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DeviceServiceImplTest {
    @Mock
    private DeviceRepository repo;

    private final DeviceMapper deviceMapper = Mappers.getMapper(DeviceMapper.class);
    private final AirConditionerParametersMapper airConditionerParametersMapper =
            Mappers.getMapper(AirConditionerParametersMapper.class);
    private final CurtainParametersMapper curtainParametersMapper =
            Mappers.getMapper(CurtainParametersMapper.class);
    private final DoorLockParametersMapper doorLockParametersMapper =
            Mappers.getMapper(DoorLockParametersMapper.class);
    private final LightParametersMapper lightParametersMapper =
            Mappers.getMapper(LightParametersMapper.class);
    private final WaterHeaterParametersMapper waterHeaterParametersMapper =
            Mappers.getMapper(WaterHeaterParametersMapper.class);

    private DeviceServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new DeviceServiceImpl(
                repo,
                deviceMapper,
                airConditionerParametersMapper,
                curtainParametersMapper,
                doorLockParametersMapper,
                lightParametersMapper,
                waterHeaterParametersMapper
        );
    }

    @AfterEach
    void tearDown() throws Exception{
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void testAddDevice_exists() {
        DeviceDto device = new DeviceDto();
        device.setId("test");
        when(repo.existsByDeviceId(anyString())).thenReturn(true);
        assertThatExceptionOfType(DeviceExistsException.class).isThrownBy(() -> service.addDevice(device));
        verify(repo, never()).insert((Device) any());
    }

    @Test
    void testAddDevice_invalidParameters() {
        DeviceDto device = new DeviceDto();
        LightParameters parameters = new LightParameters();
        parameters.setBrightness(MAX_BRIGHTNESS + 1);
        device.setId("test");
        device.setParameters(parameters);
        device.setType(DeviceType.LIGHT);
        device.setStatus("on");
        when(repo.existsByDeviceId(anyString())).thenReturn(false);
        assertThatExceptionOfType(DeviceValidationException.class)
                .isThrownBy(() -> service.addDevice(device))
                .withMessage(String.format("'%s' must be between %d and %d, got %d instead.",
                        "brightness", MIN_BRIGHTNESS, MAX_BRIGHTNESS, MAX_BRIGHTNESS + 1));
        verify(repo, never()).insert((Device) any());
    }

    @Test
    void testAddDevice_valid() {
        DeviceDto deviceDto = new DeviceDto();
        LightParameters parameters = new LightParameters();
        parameters.setBrightness(MAX_BRIGHTNESS - 1);
        parameters.setColor("#123456");
        parameters.setDimmable(true);
        parameters.setDynamicColor(false);
        deviceDto.setId("test");
        deviceDto.setParameters(parameters);
        deviceDto.setType(DeviceType.LIGHT);
        deviceDto.setStatus("on");
        deviceDto.setName("test_light");
        deviceDto.setRoom("test");
        Device device = new Device();
        device.setDeviceId("test");
        device.setName("test_light");
        device.setStatus("on");
        device.setRoom("test");
        device.setType(DeviceType.LIGHT);
        device.setParameters(parameters);
        when(repo.existsByDeviceId(anyString())).thenReturn(false);
        when(repo.insert(device)).thenReturn(device);
        DeviceDto returned = service.addDevice(deviceDto);
        verify(repo).insert(eq(device));
        assertThat(returned).isEqualTo(deviceDto);
    }

    @Test
    void testUpdateDevice_notFound() {
        DeviceUpdateDto device = new DeviceUpdateDto();
        when(repo.findByDeviceId(anyString())).thenReturn(Optional.empty());
        assertThatExceptionOfType(DeviceNotFoundException.class)
                .isThrownBy(() -> service.updateDevice("test", device))
                .withMessage("Device ID test not found");
        verify(repo, never()).save(any());
    }

    @Test
    void testUpdateDevice_wrongParametersType() {
        Device device = getValidLightDevice();
        DeviceUpdateDto updateDto = new DeviceUpdateDto();
        CurtainParameters updateParameters = new CurtainParameters();
        updateParameters.setPosition(MIN_POSITION);
        updateDto.setParameters(updateParameters);
        when(repo.findByDeviceId("test")).thenReturn(Optional.of(device));
        assertThatExceptionOfType(DeviceValidationException.class)
                .isThrownBy(() -> service.updateDevice("test", updateDto))
                .withMessage(String.format("Incorrect parameters for device type %s", device.getType()));
        verify(repo, never()).save(any());
    }
    
    @Test
    void testUpdateDevice_validLightParameters() {
        Device original = getValidLightDevice();
        Device updated = getValidLightDevice();
        LightParameters updatedParameters = (LightParameters) updated.getParameters();
        updatedParameters.setBrightness(MIN_BRIGHTNESS);
        LightParameters updateParameters = new LightParameters();
        updateParameters.setBrightness(MIN_BRIGHTNESS);
        DeviceUpdateDto updateDto = new DeviceUpdateDto();
        updateDto.setParameters(updateParameters);
        when(repo.findByDeviceId("test")).thenReturn(Optional.of(original));
        when(repo.save(original)).thenReturn(updated);
        DeviceDto result = getValidLightDeviceDto();
        assertThat(service.updateDevice("test", updateDto)).isEqualTo(result);
    }

    @Test
    void testUpdateDevice_validLightStatus() {
        Device original = getValidLightDevice();
        Device updated = getValidLightDevice();
        LightParameters updatedParameters = (LightParameters) updated.getParameters();
        updated.setStatus("off");
        updatedParameters.setBrightness(MIN_BRIGHTNESS);
        LightParameters updateParameters = new LightParameters();
        updateParameters.setBrightness(MIN_BRIGHTNESS);
        DeviceUpdateDto updateDto = new DeviceUpdateDto();
        updateDto.setParameters(updateParameters);
        updateDto.setStatus("off");
        when(repo.findByDeviceId("test")).thenReturn(Optional.of(original));
        when(repo.save(original)).thenReturn(updated);
        DeviceDto result = getValidLightDeviceDto();
        result.setStatus("off");
        assertThat(service.updateDevice("test", updateDto)).isEqualTo(result);
    }

    private static DeviceDto getValidLightDeviceDto() {
        DeviceDto result = new DeviceDto();
        result.setId("test");
        result.setType(DeviceType.LIGHT);
        result.setName("test");
        result.setRoom("test");
        result.setStatus("on");
        LightParameters lightParameters = new LightParameters();
        lightParameters.setDynamicColor(true);
        lightParameters.setDimmable(true);
        lightParameters.setColor("#123456");
        lightParameters.setBrightness(MIN_BRIGHTNESS);
        result.setParameters(lightParameters);
        return result;
    }

    private static Device getValidLightDevice() {
        Device device = new Device();
        device.set_id(new ObjectId());
        device.setDeviceId("test");
        device.setType(DeviceType.LIGHT);
        device.setName("test");
        device.setRoom("test");
        device.setStatus("on");
        LightParameters lightParameters = new LightParameters();
        lightParameters.setDynamicColor(true);
        lightParameters.setDimmable(true);
        lightParameters.setColor("#123456");
        lightParameters.setBrightness((MIN_BRIGHTNESS + MAX_BRIGHTNESS) / 2);
        device.setParameters(lightParameters);
        return device;
    }
}
