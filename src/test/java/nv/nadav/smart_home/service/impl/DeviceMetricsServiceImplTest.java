package nv.nadav.smart_home.service.impl;

import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.service.CounterManager;
import nv.nadav.smart_home.service.DeviceTrackingService;
import nv.nadav.smart_home.service.GaugeManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
}
