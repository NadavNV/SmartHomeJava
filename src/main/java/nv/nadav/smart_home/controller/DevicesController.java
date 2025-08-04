package nv.nadav.smart_home.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.exception.DeviceExistsException;
import nv.nadav.smart_home.exception.DeviceNotFoundException;
import nv.nadav.smart_home.exception.DeviceValidationException;
import nv.nadav.smart_home.serialization.DelegatingParametersDeserializer;
import nv.nadav.smart_home.serialization.DeviceParametersDeserializer;
import nv.nadav.smart_home.service.DeviceService;
import nv.nadav.smart_home.service.JwtService;
import nv.nadav.smart_home.service.MqttService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/")
public class DevicesController {

    private final DeviceService deviceService;
    private final MqttService mqttService;
    private final JwtService jwtService;

    @Autowired
    public DevicesController(DeviceService deviceService, MqttService mqttService, JwtService jwtService) {
        this.deviceService = deviceService;
        this.mqttService = mqttService;
        this.jwtService = jwtService;
    }

    @GetMapping("ids")
    public ResponseEntity<List<String>> getDeviceIds() {
        return ResponseEntity.ok(deviceService.getDeviceIds());
    }

    @GetMapping("devices")
    public ResponseEntity<List<DeviceDto>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @GetMapping("devices/{deviceId}")
    public ResponseEntity<?> getDeviceById(@PathVariable("deviceId") String deviceId) {
        try {
            return ResponseEntity.ok(deviceService.getDeviceById(deviceId));
        } catch (DeviceNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", String.format("Device ID %s not found", deviceId)));
        }
    }

    @PostMapping("devices")
    public ResponseEntity<?> addDevice(@RequestBody DeviceDto newDevice) {
        try {
            DeviceDto createdDevice = deviceService.addDevice(newDevice);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = mapper.convertValue(createdDevice, new TypeReference<>() {
            });
            mqttService.publishMqtt(payload, MqttService.TOPIC, createdDevice.getId(), MqttService.Method.POST);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(createdDevice);
        } catch (DeviceExistsException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", String.format("Device ID %s already exists", newDevice.getId())));
        } catch (DeviceValidationException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("devices/{deviceId}")
    public ResponseEntity<?> deleteDevice(@PathVariable String deviceId,
                                          @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String role = jwtService.getRoleFromToken(token);

        if (!"admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admins only"));
        }

        if (!deviceService.existsByDeviceId(deviceId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Device ID " + deviceId + " not found"));
        }

        deviceService.deleteDeviceById(deviceId);
        mqttService.publishMqtt(Map.of(), MqttService.TOPIC, deviceId, MqttService.Method.DELETE);
        return ResponseEntity.ok(Map.of("output", "Device was deleted from the database"));
    }

    @PutMapping("devices/{deviceId}")
    public ResponseEntity<?> updateDevice(@PathVariable String deviceId, @RequestBody String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            DeviceDto device = deviceService.getDeviceById(deviceId);
            DelegatingParametersDeserializer.delegate.set(
                    new DeviceParametersDeserializer(device.getType()));
            DeviceUpdateDto update = mapper.readValue(json, DeviceUpdateDto.class);
            deviceService.updateDevice(deviceId, update);
            Map<String, Object> payload = mapper.convertValue(update, new TypeReference<>() {
            });
            mqttService.publishMqtt(payload, MqttService.TOPIC, deviceId, MqttService.Method.UPDATE);
            return ResponseEntity.ok(Map.of("success", "Device updated successfully"));
        } catch (DeviceNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", String.format("Device ID %s not found", deviceId)));
        } catch (DeviceValidationException | JsonProcessingException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", e.getMessage()));
        } finally {
            DelegatingParametersDeserializer.delegate.remove();
        }
    }
}
