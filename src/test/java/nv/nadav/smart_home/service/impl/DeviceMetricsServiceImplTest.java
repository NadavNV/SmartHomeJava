package nv.nadav.smart_home.service.impl;

import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.model.parameters.*;
import nv.nadav.smart_home.service.CounterManager;
import nv.nadav.smart_home.service.DeviceTrackingService;
import nv.nadav.smart_home.service.GaugeManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static nv.nadav.smart_home.constants.Constants.*;

public class DeviceMetricsServiceImplTest {

    @Mock
    private GaugeManager mockGauges;

    @Mock
    private CounterManager mockCounters;

    @Mock
    private DeviceTrackingService mockDeviceTracking;

    private DeviceMetricsServiceImpl service;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = spy(new DeviceMetricsServiceImpl(mockDeviceTracking, mockGauges, mockCounters));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void testAddDevice_new() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.LIGHT;
        DeviceDto device = new DeviceDto();
        device.setId(deviceId);
        device.setType(deviceType);
        when(mockDeviceTracking.isDeviceNew(deviceId)).thenReturn(true);

        service.addDevice(device);

        verify(mockCounters, times(1)).incrementBy(
                eq("device_on_events_total"),
                any(String.class),
                eq(Map.of("device_id", deviceId, "device_type", deviceType.getValue())),
                eq(0.0)
        );
        verify(mockCounters, times(1)).incrementBy(
                eq("device_usage_seconds_total"),
                any(String.class),
                eq(Map.of("device_id", deviceId, "device_type", deviceType.getValue())),
                eq(0.0)
        );
        ArgumentCaptor<DeviceUpdateDto> dtoCaptor1 = ArgumentCaptor.forClass(DeviceUpdateDto.class);
        ArgumentCaptor<DeviceUpdateDto> dtoCaptor2 = ArgumentCaptor.forClass(DeviceUpdateDto.class);
        ArgumentCaptor<DeviceType> typeCaptor = ArgumentCaptor.forClass(DeviceType.class);
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(service).updateDevice(
                dtoCaptor1.capture(),
                dtoCaptor2.capture(),
                typeCaptor.capture(),
                idCaptor.capture()
        );
        assertEquals(DeviceUpdateDto.fromDto(device), dtoCaptor1.getValue());
        assertEquals(DeviceUpdateDto.fromDto(device), dtoCaptor2.getValue());
        assertEquals(DeviceType.LIGHT, typeCaptor.getValue());
        assertEquals(deviceId, idCaptor.getValue());
        verify(mockDeviceTracking).markDeviceSeen(deviceId);
    }

    @Test
    void testAddDevice_notNew() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.LIGHT;
        DeviceDto device = new DeviceDto();
        device.setId(deviceId);
        device.setType(deviceType);
        when(mockDeviceTracking.isDeviceNew(deviceId)).thenReturn(false);

        service.addDevice(device);

        verify(mockCounters, never()).incrementBy(anyString(), anyString(), anyMap(), anyDouble());
        verify(service, never()).updateDevice(any(), any(), any(), any());
        verify(mockDeviceTracking, never()).markDeviceSeen(anyString());
    }

    @Test
    void testUpdateDevice_updateName() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.LIGHT;
        DeviceUpdateDto old = new DeviceUpdateDto();
        DeviceUpdateDto update = new DeviceUpdateDto();
        old.setName("old");
        update.setName("new");
        service.updateDevice(old, update, deviceType, deviceId);
        verify(mockGauges, times(1)).setBooleanGauge(
                eq("device_metadata"),
                anyString(),
                eq(false),
                eq(Map.of("device_id", deviceId, "key", "name", "value", "old"))
        );
        verify(mockGauges, times(1)).setBooleanGauge(
                eq("device_metadata"),
                anyString(),
                eq(true),
                eq(Map.of("device_id", deviceId, "key", "name", "value", "new"))
        );
    }

    @Test
    void testUpdateDevice_updateRoom() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.LIGHT;
        DeviceUpdateDto old = new DeviceUpdateDto();
        DeviceUpdateDto update = new DeviceUpdateDto();
        old.setRoom("old");
        update.setRoom("new");
        service.updateDevice(old, update, deviceType, deviceId);
        verify(mockGauges, times(1)).setBooleanGauge(
                eq("device_metadata"),
                anyString(),
                eq(false),
                eq(Map.of("device_id", deviceId, "key", "room", "value", "old"))
        );
        verify(mockGauges, times(1)).setBooleanGauge(
                eq("device_metadata"),
                anyString(),
                eq(true),
                eq(Map.of("device_id", deviceId, "key", "room", "value", "new"))
        );
    }

    @Test
    void testUpdateDevice_newOnDevice() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.LIGHT;
        DeviceUpdateDto update = new DeviceUpdateDto();
        update.setStatus("on");
        when(mockDeviceTracking.isDeviceNew(deviceId)).thenReturn(true);
        service.updateDevice(update, update, deviceType, deviceId);
        verify(mockDeviceTracking, times(1)).startNewInterval(eq(deviceId), any());
        verify(mockGauges, times(1)).setNumericGauge(
                eq("device_status"),
                anyString(),
                eq(1.0),
                eq(Map.of("device_id", deviceId, "device_type", deviceType.getValue()))
        );
    }

    @Test
    void testUpdateDevice_newOffDevice() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.LIGHT;
        DeviceUpdateDto update = new DeviceUpdateDto();
        update.setStatus("off");
        when(mockDeviceTracking.isDeviceNew(deviceId)).thenReturn(true);
        service.updateDevice(update, update, deviceType, deviceId);
        verify(mockDeviceTracking, never()).closeLastInterval(eq(deviceId), any());
        verify(mockGauges, times(1)).setNumericGauge(
                eq("device_status"),
                anyString(),
                eq(0.0),
                eq(Map.of("device_id", deviceId, "device_type", deviceType.getValue()))
        );
    }

    @Test
    void testUpdateDevice_oldOnDevice() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.LIGHT;
        DeviceUpdateDto update = new DeviceUpdateDto();
        DeviceUpdateDto old = new DeviceUpdateDto();
        old.setStatus("off");
        update.setStatus("on");
        when(mockDeviceTracking.isDeviceNew(deviceId)).thenReturn(false);
        service.updateDevice(old, update, deviceType, deviceId);
        verify(mockDeviceTracking, times(1)).startNewInterval(eq(deviceId), any());
        verify(mockGauges, times(1)).setNumericGauge(
                eq("device_status"),
                anyString(),
                eq(1.0),
                eq(Map.of("device_id", deviceId, "device_type", deviceType.getValue()))
        );
    }

    @Test
    void testUpdateDevice_oldOffDevice() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.LIGHT;
        DeviceUpdateDto update = new DeviceUpdateDto();
        DeviceUpdateDto old = new DeviceUpdateDto();
        old.setStatus("on");
        update.setStatus("off");
        when(mockDeviceTracking.isDeviceNew(deviceId)).thenReturn(false);
        service.updateDevice(old, update, deviceType, deviceId);
        verify(mockDeviceTracking, times(1)).closeLastInterval(eq(deviceId), any());
        verify(mockGauges, times(1)).setNumericGauge(
                eq("device_status"),
                anyString(),
                eq(0.0),
                eq(Map.of("device_id", deviceId, "device_type", deviceType.getValue()))
        );
    }

    @Test
    void testUpdateDevice_doorLockLocked() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.DOOR_LOCK;
        DeviceUpdateDto update = new DeviceUpdateDto();
        update.setStatus("locked");
        service.updateDevice(update, update, deviceType, deviceId);
        verify(mockGauges, times(1)).setNumericGauge(
                eq("lock_status"),
                anyString(),
                eq(1.0),
                eq(Map.of("device_id", deviceId))
        );
        verify(mockDeviceTracking, never()).startNewInterval(anyString(), any());
        verify(mockDeviceTracking, never()).closeLastInterval(anyString(), any());
    }

    @Test
    void testUpdateDevice_doorLockUnlocked() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.DOOR_LOCK;
        DeviceUpdateDto update = new DeviceUpdateDto();
        update.setStatus("unlocked");
        service.updateDevice(update, update, deviceType, deviceId);
        verify(mockGauges, times(1)).setNumericGauge(
                eq("lock_status"),
                anyString(),
                eq(0.0),
                eq(Map.of("device_id", deviceId))
        );
        verify(mockDeviceTracking, never()).startNewInterval(anyString(), any());
        verify(mockDeviceTracking, never()).closeLastInterval(anyString(), any());
    }

    @Test
    void testUpdateDevice_curtainClosed() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.DOOR_LOCK;
        DeviceUpdateDto update = new DeviceUpdateDto();
        update.setStatus("closed");
        service.updateDevice(update, update, deviceType, deviceId);
        verify(mockGauges, times(1)).setNumericGauge(
                eq("lock_status"),
                anyString(),
                eq(1.0),
                eq(Map.of("device_id", deviceId))
        );
        verify(mockDeviceTracking, never()).startNewInterval(anyString(), any());
        verify(mockDeviceTracking, never()).closeLastInterval(anyString(), any());
    }

    @Test
    void testUpdateDevice_curtainOpen() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.DOOR_LOCK;
        DeviceUpdateDto update = new DeviceUpdateDto();
        update.setStatus("open");
        service.updateDevice(update, update, deviceType, deviceId);
        verify(mockGauges, times(1)).setNumericGauge(
                eq("lock_status"),
                anyString(),
                eq(0.0),
                eq(Map.of("device_id", deviceId))
        );
        verify(mockDeviceTracking, never()).startNewInterval(anyString(), any());
        verify(mockDeviceTracking, never()).closeLastInterval(anyString(), any());
    }

    @Test
    void testUpdateDevice_lightParametersFull() {
        LightParameters parameters = new LightParameters();
        parameters.setBrightness((MIN_BRIGHTNESS + MAX_BRIGHTNESS) / 2);
        parameters.setColor("#123456");
        parameters.setDimmable(true);
        parameters.setDynamicColor(false);
        DeviceUpdateDto update = new DeviceUpdateDto();
        update.setParameters(parameters);
        String deviceId = "test";
        DeviceType deviceType = DeviceType.LIGHT;
        service.updateDevice(update, update, deviceType, deviceId);
        InOrder inOrder = inOrder(mockGauges);
        // Brightness
        inOrder.verify(mockGauges, times(1)).setNumericGauge(
                eq("light_brightness"),
                anyString(),
                eq(parameters.getBrightness().doubleValue()),
                eq(Map.of("device_id", deviceId, "is_dimmable", Boolean.toString(parameters.isDimmable())))
        );
        // Color
        inOrder.verify(mockGauges, times(1)).setNumericGauge(
                eq("light_color"),
                anyString(),
                eq((double) Integer.parseInt(parameters.getColor().substring(1), 16)),
                eq(Map.of("device_id", deviceId, "dynamic_color", Boolean.toString(parameters.isDynamicColor())))
        );
        inOrder.verify(mockGauges, times(1)).setBooleanGauge(
                eq("light_color_info"),
                anyString(),
                eq(false),
                eq(Map.of(
                        "device_id", deviceId,
                        "dynamic_color", Boolean.toString(parameters.isDynamicColor()),
                        "color", parameters.getColor()
                ))
        );
        inOrder.verify(mockGauges, times(1)).setBooleanGauge(
                eq("light_color_info"),
                anyString(),
                eq(true),
                eq(Map.of(
                        "device_id", deviceId,
                        "dynamic_color", Boolean.toString(parameters.isDynamicColor()),
                        "color", parameters.getColor()
                ))
        );
        // isDimmable
        inOrder.verify(mockGauges, times(1)).setBooleanGauge(
                eq("light_is_dimmable"),
                anyString(),
                eq(true),
                eq(Map.of("device_id", deviceId, "state", "True"))
        );
        inOrder.verify(mockGauges, times(1)).setBooleanGauge(
                eq("light_is_dimmable"),
                anyString(),
                eq(false),
                eq(Map.of("device_id", deviceId, "state", "False"))
        );
        // dynamicColor
        inOrder.verify(mockGauges, times(1)).setBooleanGauge(
                eq("light_dynamic_color"),
                anyString(),
                eq(false),
                eq(Map.of("device_id", deviceId, "state", "True"))
        );
        inOrder.verify(mockGauges, times(1)).setBooleanGauge(
                eq("light_dynamic_color"),
                anyString(),
                eq(true),
                eq(Map.of("device_id", deviceId, "state", "False"))
        );
    }

    @Test
    void testUpdateDevice_lightParametersNoNewBooleans() {
        LightParameters oldParameters = new LightParameters();
        oldParameters.setBrightness((MIN_BRIGHTNESS + MAX_BRIGHTNESS) / 2);
        oldParameters.setColor("#654321");
        oldParameters.setDimmable(true);
        oldParameters.setDynamicColor(false);
        DeviceUpdateDto original = new DeviceUpdateDto();
        DeviceUpdateDto update = new DeviceUpdateDto();
        original.setParameters(oldParameters);
        LightParameters newParameters = new LightParameters();
        newParameters.setBrightness((MIN_BRIGHTNESS + MAX_BRIGHTNESS) / 2);
        newParameters.setColor("#123456");
        update.setParameters(newParameters);
        String deviceId = "test";
        DeviceType deviceType = DeviceType.LIGHT;
        service.updateDevice(original, update, deviceType, deviceId);
        InOrder inOrder = inOrder(mockGauges);
        // Brightness
        inOrder.verify(mockGauges, times(1)).setNumericGauge(
                eq("light_brightness"),
                anyString(),
                eq(newParameters.getBrightness().doubleValue()),
                eq(Map.of("device_id", deviceId, "is_dimmable", Boolean.toString(oldParameters.isDimmable())))
        );
        // Color
        inOrder.verify(mockGauges, times(1)).setNumericGauge(
                eq("light_color"),
                anyString(),
                eq((double) Integer.parseInt(newParameters.getColor().substring(1), 16)),
                eq(Map.of("device_id", deviceId, "dynamic_color", Boolean.toString(oldParameters.isDynamicColor())))
        );
        inOrder.verify(mockGauges, times(1)).setBooleanGauge(
                eq("light_color_info"),
                anyString(),
                eq(false),
                eq(Map.of(
                        "device_id", deviceId,
                        "dynamic_color", Boolean.toString(oldParameters.isDynamicColor()),
                        "color", oldParameters.getColor()
                ))
        );
        inOrder.verify(mockGauges, times(1)).setBooleanGauge(
                eq("light_color_info"),
                anyString(),
                eq(true),
                eq(Map.of(
                        "device_id", deviceId,
                        "dynamic_color", Boolean.toString(oldParameters.isDynamicColor()),
                        "color", newParameters.getColor()
                ))
        );
    }

    @Test
    void testUpdateDevice_waterHeaterFull() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.WATER_HEATER;
        WaterHeaterParameters oldParameters = new WaterHeaterParameters();
        WaterHeaterParameters newParameters = new WaterHeaterParameters();
        oldParameters.setTargetTemperature((MIN_WATER_TEMP + MAX_WATER_TEMP) / 2);
        oldParameters.setTemperature(24);
        oldParameters.setHeating(false);
        oldParameters.setTimerEnabled(true);
        oldParameters.setScheduledOn("06:30");
        oldParameters.setScheduledOff("08:00");
        newParameters.setTargetTemperature(MAX_WATER_TEMP);
        newParameters.setTemperature(23);
        newParameters.setHeating(true);
        newParameters.setTimerEnabled(false);
        newParameters.setScheduledOn("10:00");
        newParameters.setScheduledOff("12:30");
        DeviceUpdateDto original = new DeviceUpdateDto();
        DeviceUpdateDto update = new DeviceUpdateDto();
        original.setParameters(oldParameters);
        update.setParameters(newParameters);
        service.updateDevice(original, update, deviceType, deviceId);
        InOrder inOrder = inOrder(mockGauges);
        // Temperature
        inOrder.verify(mockGauges, times(1)).setNumericGauge(
                eq("water_heater_temperature"),
                anyString(),
                eq(newParameters.getTemperature().doubleValue()),
                eq(Map.of("device_id", deviceId))
        );
        // Target temperature
        inOrder.verify(mockGauges, times(1)).setNumericGauge(
                eq("water_heater_target_temperature"),
                anyString(),
                eq(newParameters.getTargetTemperature().doubleValue()),
                eq(Map.of("device_id", deviceId))
        );
        // isHeating
        inOrder.verify(mockGauges, times(1)).setBooleanGauge(
                eq("water_heater_is_heating_status"),
                anyString(),
                eq(true),
                eq(Map.of("device_id", deviceId, "state", "True"))
        );
        inOrder.verify(mockGauges, times(1)).setBooleanGauge(
                eq("water_heater_is_heating_status"),
                anyString(),
                eq(false),
                eq(Map.of("device_id", deviceId, "state", "False"))
        );
        // timerEnabled
        inOrder.verify(mockGauges, times(1)).setBooleanGauge(
                eq("water_heater_timer_enabled_status"),
                anyString(),
                eq(false),
                eq(Map.of("device_id", deviceId, "state", "True"))
        );
        inOrder.verify(mockGauges, times(1)).setBooleanGauge(
                eq("water_heater_timer_enabled_status"),
                anyString(),
                eq(true),
                eq(Map.of("device_id", deviceId, "state", "False"))
        );
        // Schedule
        inOrder.verify(mockGauges, times(1)).setNumericGauge(
                eq("water_heater_schedule_info"),
                anyString(),
                eq(0.0),
                eq(Map.of(
                        "device_id", deviceId,
                        "scheduled_on", oldParameters.getScheduledOn(),
                        "scheduled_off", oldParameters.getScheduledOff()
                ))
        );
        inOrder.verify(mockGauges, times(1)).setNumericGauge(
                eq("water_heater_schedule_info"),
                anyString(),
                eq(1.0),
                eq(Map.of(
                        "device_id", deviceId,
                        "scheduled_on", newParameters.getScheduledOn(),
                        "scheduled_off", newParameters.getScheduledOff()
                ))
        );
    }

    @Test
    void testUpdateDevice_waterHeaterScheduleOn() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.WATER_HEATER;
        WaterHeaterParameters oldParameters = new WaterHeaterParameters();
        WaterHeaterParameters newParameters = new WaterHeaterParameters();
        oldParameters.setScheduledOn("06:30");
        oldParameters.setScheduledOff("08:00");
        newParameters.setScheduledOn("10:00");
        DeviceUpdateDto original = new DeviceUpdateDto();
        DeviceUpdateDto update = new DeviceUpdateDto();
        original.setParameters(oldParameters);
        update.setParameters(newParameters);
        service.updateDevice(original, update, deviceType, deviceId);
        InOrder inOrder = inOrder(mockGauges);

        // Schedule
        inOrder.verify(mockGauges, times(1)).setNumericGauge(
                eq("water_heater_schedule_info"),
                anyString(),
                eq(0.0),
                eq(Map.of(
                        "device_id", deviceId,
                        "scheduled_on", oldParameters.getScheduledOn(),
                        "scheduled_off", oldParameters.getScheduledOff()
                ))
        );
        inOrder.verify(mockGauges, times(1)).setNumericGauge(
                eq("water_heater_schedule_info"),
                anyString(),
                eq(1.0),
                eq(Map.of(
                        "device_id", deviceId,
                        "scheduled_on", newParameters.getScheduledOn(),
                        "scheduled_off", oldParameters.getScheduledOff()
                ))
        );
    }

    @Test
    void testUpdateDevice_waterHeaterScheduleOff() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.WATER_HEATER;
        WaterHeaterParameters oldParameters = new WaterHeaterParameters();
        WaterHeaterParameters newParameters = new WaterHeaterParameters();
        oldParameters.setScheduledOn("06:30");
        oldParameters.setScheduledOff("08:00");
        newParameters.setScheduledOff("10:00");
        DeviceUpdateDto original = new DeviceUpdateDto();
        DeviceUpdateDto update = new DeviceUpdateDto();
        original.setParameters(oldParameters);
        update.setParameters(newParameters);
        service.updateDevice(original, update, deviceType, deviceId);
        InOrder inOrder = inOrder(mockGauges);

        // Schedule
        inOrder.verify(mockGauges, times(1)).setNumericGauge(
                eq("water_heater_schedule_info"),
                anyString(),
                eq(0.0),
                eq(Map.of(
                        "device_id", deviceId,
                        "scheduled_on", oldParameters.getScheduledOn(),
                        "scheduled_off", oldParameters.getScheduledOff()
                ))
        );
        inOrder.verify(mockGauges, times(1)).setNumericGauge(
                eq("water_heater_schedule_info"),
                anyString(),
                eq(1.0),
                eq(Map.of(
                        "device_id", deviceId,
                        "scheduled_on", oldParameters.getScheduledOn(),
                        "scheduled_off", newParameters.getScheduledOff()
                ))
        );
    }

    @Test
    void testUpdateDevice_airConditionerFull() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.AIR_CONDITIONER;
        AirConditionerParameters parameters = new AirConditionerParameters();
        parameters.setTemperature((MIN_AC_TEMP + MAX_AC_TEMP) / 2);
        parameters.setMode(AirConditionerParameters.Mode.COOL);
        parameters.setSwing(AirConditionerParameters.Swing.ON);
        parameters.setFanSpeed(AirConditionerParameters.FanSpeed.MEDIUM);
        DeviceUpdateDto update = new DeviceUpdateDto();
        update.setParameters(parameters);
        service.updateDevice(update, update, deviceType, deviceId);
        // Temperature
        verify(mockGauges, times(1)).setNumericGauge(
                eq("ac_temperature"),
                anyString(),
                eq(parameters.getTemperature().doubleValue()),
                eq(Map.of("device_id", deviceId))
        );
        // Mode
        verify(mockGauges, times(1)).setEnumGauge(
                eq("ac_mode_status"),
                anyString(),
                eq(parameters.getMode()),
                eq(Map.of("device_id", deviceId))
        );
        // Fan Speed
        verify(mockGauges, times(1)).setEnumGauge(
                eq("ac_fan_status"),
                anyString(),
                eq(parameters.getFanSpeed()),
                eq(Map.of("device_id", deviceId))
        );
        // Swing
        verify(mockGauges, times(1)).setEnumGauge(
                eq("ac_swing_status"),
                anyString(),
                eq(parameters.getSwing()),
                eq(Map.of("device_id", deviceId))
        );
    }

    @Test
    void testUpdateDevice_doorLockFull() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.DOOR_LOCK;
        DoorLockParameters parameters = new DoorLockParameters();
        parameters.setBatteryLevel((MIN_BATTERY + MAX_BATTERY) / 2);
        parameters.setAutoLockEnabled(true);
        DeviceUpdateDto update = new DeviceUpdateDto();
        update.setParameters(parameters);
        service.updateDevice(update, update, deviceType, deviceId);
        // Battery Leve
        verify(mockGauges, times(1)).setNumericGauge(
                eq("lock_battery_level"),
                anyString(),
                eq(parameters.getBatteryLevel().doubleValue()),
                eq(Map.of("device_id", deviceId))
        );
        // Auto-Lock
        InOrder inOrder = inOrder(mockGauges);
        inOrder.verify(mockGauges, times(1)).setBooleanGauge(
                eq("auto_lock_enabled"),
                anyString(),
                eq(true),
                eq(Map.of("device_id", deviceId, "state", "True"))
        );
        inOrder.verify(mockGauges, times(1)).setBooleanGauge(
                eq("auto_lock_enabled"),
                anyString(),
                eq(false),
                eq(Map.of("device_id", deviceId, "state", "False"))
        );
    }

    @Test
    void testUpdateDevice_curtainFull() {
        String deviceId = "test";
        DeviceType deviceType = DeviceType.CURTAIN;
        CurtainParameters parameters = new CurtainParameters();
        parameters.setPosition((MIN_POSITION + MAX_POSITION) / 2);
        DeviceUpdateDto update = new DeviceUpdateDto();
        update.setParameters(parameters);
        service.updateDevice(update, update, deviceType, deviceId);
        // Battery Leve
        verify(mockGauges, times(1)).setNumericGauge(
                eq("curtain_position"),
                anyString(),
                eq(parameters.getPosition().doubleValue()),
                eq(Map.of("device_id", deviceId))
        );
    }

    @Test
    void testDeleteDevice_doesNotThrow() {
        assertDoesNotThrow(() -> service.deleteDevice("test"));
    }
}
