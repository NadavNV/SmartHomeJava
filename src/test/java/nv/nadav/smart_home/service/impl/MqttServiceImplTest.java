package nv.nadav.smart_home.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.model.parameters.WaterHeaterParameters;
import nv.nadav.smart_home.service.DeviceMetricsService;
import nv.nadav.smart_home.service.DeviceService;
import nv.nadav.smart_home.service.MqttService;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MqttServiceImplTest {

    @Mock
    private MqttClient mockClient;

    @Mock
    private DeviceService mockDeviceService;

    @Mock
    private DeviceMetricsService mockMetricsService;

    @Mock
    private Validator mockValidator;

    @InjectMocks
    private MqttServiceImpl mqttService;

    @Captor
    private ArgumentCaptor<MqttCallback> callbackCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(mockClient.getClientId()).thenReturn("test-client");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void testMqttInit_setsCallbackAndSubscribes() throws MqttException {
        mqttService.mqttInit();

        verify(mockClient).setCallback(any());
        verify(mockClient).connect(any());
        verify(mockClient).subscribe(eq("$share/backend/nadavnv-smart-home/devices/#"), eq(2));
    }

    @Test
    void testPublishMqtt_successful() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "light");
        payload.put("value", 1);

        mqttService.publishMqtt(payload, "prefix", "abc123", MqttService.Method.POST);

        verify(mockClient).publish(eq("prefix/abc123/post"), any(MqttMessage.class));
    }

    @Test
    void testPublishMqtt_publishFails_addsToQueue() throws Exception {
        doThrow(new MqttException(1)).when(mockClient).publish(anyString(), any(MqttMessage.class));

        Map<String, Object> payload = new HashMap<>(Map.of("type", "sensor", "value", 42));
        mqttService.publishMqtt(payload, "prefix", "abc123", MqttService.Method.POST);

        // no exception means fallback logic worked
        verify(mockClient).publish(eq("prefix/abc123/post"), any(MqttMessage.class));
    }

    @Test
    void testMessageArrived_validPost_addsDevice() throws Exception {
        MqttCallback callback = captureCallback();

        DeviceDto dto = new DeviceDto();
        String json = objectMapper.writeValueAsString(dto);

        MqttMessage message = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
        message.setProperties(userProperties(
                "sender_id", "device-1",
                "sender_group", "frontend"
        ));

        when(mockValidator.validate(any(DeviceDto.class))).thenReturn(Set.of());

        String topic = "nadavnv-smart-home/devices/dev123/post";
        callback.messageArrived(topic, message);

        verify(mockDeviceService).addDevice(any(DeviceDto.class));
        verify(mockMetricsService).addDevice(any(DeviceDto.class));
    }

    @Test
    void testMessageArrived_invalidTopic_logsError() throws Exception {
        MqttCallback callback = captureCallback();

        MqttMessage message = new MqttMessage("{\"type\":\"test\"}".getBytes(StandardCharsets.UTF_8));
        message.setProperties(userProperties("sender_id", "abc", "sender_group", "frontend"));

        String topic = "bad/topic/format";

        callback.messageArrived(topic, message);

        verify(mockDeviceService, never()).addDevice(any());
        verify(mockMetricsService, never()).addDevice(any());
    }

    @Test
    void testMessageArrived_selfMessage_skips() throws Exception {
        MqttCallback callback = captureCallback();

        MqttMessage message = new MqttMessage("{}".getBytes());
        message.setProperties(userProperties("sender_id", "test-client", "sender_group", "frontend"));

        String topic = "nadavnv-smart-home/devices/dev123/post";
        callback.messageArrived(topic, message);

        verify(mockDeviceService, never()).addDevice(any());
        verify(mockMetricsService, never()).addDevice(any());
    }

    @Test
    void testMessageArrived_updateValid() throws Exception {
        String deviceId = "test";
        String topic = "nadavnv-smart-home/devices/" + deviceId + "/update";

        WaterHeaterParameters parameters = new WaterHeaterParameters();
        parameters.setTemperature(60);
        DeviceUpdateDto updateDto = new DeviceUpdateDto();
        updateDto.setParameters(parameters);
        DeviceType deviceType = DeviceType.WATER_HEATER;

        String jsonPayload = objectMapper.writeValueAsString(updateDto);


        MqttMessage message = new MqttMessage(jsonPayload.getBytes(StandardCharsets.UTF_8));
        message.setProperties(userProperties("sender_id", "external-device", "sender_group", "frontend"));

        DeviceDto existingDevice = new DeviceDto();
        existingDevice.setId(deviceId);
        existingDevice.setType(deviceType);
        assertEquals(DeviceType.WATER_HEATER, existingDevice.getType());
        when(mockDeviceService.getDeviceById(deviceId)).thenReturn(existingDevice);

        MqttCallback callback = captureCallback();
        callback.messageArrived(topic, message);

        ArgumentCaptor<DeviceUpdateDto> captor = ArgumentCaptor.forClass(DeviceUpdateDto.class);
        verify(mockDeviceService).updateDevice(eq(deviceId), captor.capture());
        verify(mockMetricsService).updateDevice(
                any(DeviceUpdateDto.class),
                eq(updateDto),
                eq(DeviceType.WATER_HEATER),
                eq(deviceId)
        );

        DeviceUpdateDto captured = captor.getValue();
        assertInstanceOf(WaterHeaterParameters.class, captured.getParameters());
        WaterHeaterParameters capturedParams = (WaterHeaterParameters) captured.getParameters();
        assertEquals(60, capturedParams.getTemperature());
    }

    // === Helpers ===

    private MqttCallback captureCallback() {
        mqttService.mqttInit();
        verify(mockClient).setCallback(callbackCaptor.capture());
        return callbackCaptor.getValue();
    }

    private MqttProperties userProperties(String... kvPairs) {
        MqttProperties props = new MqttProperties();
        List<UserProperty> list = new ArrayList<>();
        for (int i = 0; i < kvPairs.length; i += 2) {
            list.add(new UserProperty(kvPairs[i], kvPairs[i + 1]));
        }
        props.setUserProperties(list);
        return props;
    }
}

