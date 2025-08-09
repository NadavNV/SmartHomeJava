package nv.nadav.smart_home.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.exception.DeviceExistsException;
import nv.nadav.smart_home.exception.DeviceNotFoundException;
import nv.nadav.smart_home.exception.DeviceValidationException;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.model.parameters.LightParameters;
import nv.nadav.smart_home.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static nv.nadav.smart_home.constants.Constants.MIN_BRIGHTNESS;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DevicesController.class)
class DevicesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceService deviceService;

    @MockitoBean
    private DeviceMetricsService deviceMetricsService;

    @MockitoBean
    private MqttService mqttService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private HttpMetricsService httpMetricsService;

    @Autowired
    private ObjectMapper objectMapper;

    private DeviceDto sampleDevice;

    @BeforeEach
    void setup() {
        sampleDevice = new DeviceDto();
        sampleDevice.setId("device123");
        sampleDevice.setType(DeviceType.LIGHT);
        sampleDevice.setName("test");
        sampleDevice.setRoom("test");
        sampleDevice.setStatus("on");
        LightParameters lightParameters = new LightParameters();
        lightParameters.setDynamicColor(true);
        lightParameters.setDimmable(true);
        lightParameters.setColor("#123456");
        lightParameters.setBrightness(MIN_BRIGHTNESS);
        sampleDevice.setParameters(lightParameters);
    }

    @Test
    void getDeviceIds_ReturnsList() throws Exception {
        given(deviceService.getDeviceIds()).willReturn(List.of("device1", "device2"));

        mockMvc.perform(get("/api/ids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]", is("device1")))
                .andExpect(jsonPath("$[1]", is("device2")));
    }

    @Test
    void getAllDevices_ReturnsDevicesAndCallsMetrics() throws Exception {
        given(deviceService.getAllDevices()).willReturn(List.of(sampleDevice));

        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("device123")));

        then(deviceMetricsService).should().addDevice(sampleDevice);
    }

    @Test
    void getDeviceById_Found() throws Exception {
        given(deviceService.getDeviceById("device123")).willReturn(sampleDevice);

        mockMvc.perform(get("/api/devices/device123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("device123")));

        then(deviceMetricsService).should().addDevice(sampleDevice);
    }

    @Test
    void getDeviceById_NotFound() throws Exception {
        given(deviceService.getDeviceById("missing")).willThrow(new DeviceNotFoundException());

        mockMvc.perform(get("/api/devices/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("missing")));
    }

    @Test
    void addDevice_Success() throws Exception {
        given(deviceService.addDevice(ArgumentMatchers.any(DeviceDto.class))).willReturn(sampleDevice);

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDevice)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("device123")));

        then(deviceMetricsService).should().addDevice(any());
        then(mqttService).should().publishMqtt(anyMap(), eq(MqttService.TOPIC), eq("device123"), eq(MqttService.Method.POST));
    }

    @Test
    void addDevice_DeviceExists() throws Exception {
        given(deviceService.addDevice(any())).willThrow(new DeviceExistsException());

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDevice)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("already exists")));
    }

    @Test
    void addDevice_ValidationError() throws Exception {
        given(deviceService.addDevice(any())).willThrow(new DeviceValidationException("Invalid"));

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDevice)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid")));
    }

    @Test
    void deleteDevice_AdminRole_Success() throws Exception {
        given(jwtService.getRoleFromToken(anyString())).willReturn("admin");
        given(deviceService.existsByDeviceId("device123")).willReturn(true);

        mockMvc.perform(delete("/api/devices/device123")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.output", containsString("deleted")));

        then(deviceService).should().deleteDeviceById("device123");
        then(deviceMetricsService).should().deleteDevice("device123");
        then(mqttService).should().publishMqtt(anyMap(), eq(MqttService.TOPIC), eq("device123"), eq(MqttService.Method.DELETE));
    }

    @Test
    void deleteDevice_NonAdmin_Forbidden() throws Exception {
        given(jwtService.getRoleFromToken(anyString())).willReturn("user");

        mockMvc.perform(delete("/api/devices/device123")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("Admins only")));

        then(deviceService).should(never()).deleteDeviceById(anyString());
    }

    @Test
    void deleteDevice_NotFound() throws Exception {
        given(jwtService.getRoleFromToken(anyString())).willReturn("admin");
        given(deviceService.existsByDeviceId("device123")).willReturn(false);

        mockMvc.perform(delete("/api/devices/device123")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("not found")));

        then(deviceService).should(never()).deleteDeviceById(anyString());
    }

    @Test
    void updateDevice_Success() throws Exception {
        String updateJson = "{\"status\":\"off\"}";

        given(deviceService.getDeviceById("device123")).willReturn(sampleDevice);
        given(deviceService.updateDevice(anyString(), ArgumentMatchers.any(DeviceUpdateDto.class))).willReturn(null);

        DeviceUpdateDto updateDto = new DeviceUpdateDto();
        updateDto.setStatus("off");

        mockMvc.perform(put("/api/devices/device123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", containsString("updated")));

        then(deviceMetricsService).should().addDevice(sampleDevice);
        then(deviceMetricsService).should().updateDevice(eq(DeviceUpdateDto.fromDto(sampleDevice)), eq(updateDto), eq(DeviceType.LIGHT), eq("device123"));
        then(mqttService).should().publishMqtt(anyMap(), eq(MqttService.TOPIC), eq("device123"), eq(MqttService.Method.UPDATE));
    }

    @Test
    void updateDevice_NotFound() throws Exception {
        given(deviceService.getDeviceById("device123")).willThrow(new DeviceNotFoundException());

        mockMvc.perform(put("/api/devices/device123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("not found")));

        then(deviceService).should(never()).updateDevice(anyString(), ArgumentMatchers.any(DeviceUpdateDto.class));
    }

    @Test
    void updateDevice_ValidationError() throws Exception {
        given(deviceService.getDeviceById("device123")).willReturn(sampleDevice);
        willThrow(new DeviceValidationException("Invalid")).given(deviceService).updateDevice(anyString(), any());

        mockMvc.perform(put("/api/devices/device123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid")));
    }
}