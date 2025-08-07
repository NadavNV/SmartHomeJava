package nv.nadav.smart_home.controller;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.exception.DeviceExistsException;
import nv.nadav.smart_home.exception.DeviceNotFoundException;
import nv.nadav.smart_home.exception.DeviceValidationException;
import nv.nadav.smart_home.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/")
public class DevicesController {

    private final DeviceService deviceService;
    private final DeviceMetricsService deviceMetricsService;
    private final MqttService mqttService;
    private final JwtService jwtService;

    @Autowired
    public DevicesController(
            DeviceService deviceService,
            DeviceMetricsService deviceMetricsService,
            MqttService mqttService,
            JwtService jwtService
    ) {
        this.deviceService = deviceService;
        this.deviceMetricsService = deviceMetricsService;
        this.mqttService = mqttService;
        this.jwtService = jwtService;
    }

    @GetMapping("ids")
    public ResponseEntity<List<String>> getDeviceIds() {
        return ResponseEntity.ok(deviceService.getDeviceIds());
    }

    @GetMapping("devices")
    public ResponseEntity<List<DeviceDto>> getAllDevices() {
        List<DeviceDto> devices = deviceService.getAllDevices();
        for (DeviceDto device : devices) {
            deviceMetricsService.addDevice(device);
        }
        return ResponseEntity.ok(devices);
    }

    @GetMapping("devices/{deviceId}")
    public ResponseEntity<?> getDeviceById(@PathVariable("deviceId") String deviceId) {
        try {
            DeviceDto device = deviceService.getDeviceById(deviceId);
            deviceMetricsService.addDevice(device);
            return ResponseEntity.ok(device);
        } catch (DeviceNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", String.format("Device ID %s not found", deviceId)));
        }
    }

    @PostMapping("devices")
    public ResponseEntity<?> addDevice(@Valid @RequestBody DeviceDto newDevice) {
        try {
            DeviceDto createdDevice = deviceService.addDevice(newDevice);
            deviceMetricsService.addDevice(createdDevice);
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
        deviceMetricsService.deleteDevice(deviceId);  // Allows adding a new device with old id
        mqttService.publishMqtt(Map.of(), MqttService.TOPIC, deviceId, MqttService.Method.DELETE);
        return ResponseEntity.ok(Map.of("output", "Device was deleted from the database"));
    }

    @PutMapping("devices/{deviceId}")
    public ResponseEntity<?> updateDevice(@PathVariable String deviceId, @RequestBody String json) {
        try {
            DeviceDto device = deviceService.getDeviceById(deviceId);
            deviceMetricsService.addDevice(device);
            DeviceUpdateDto update = DeviceUpdateDto.deserialize(json, device.getType());
            deviceService.updateDevice(deviceId, update);
            deviceMetricsService.updateDevice(DeviceUpdateDto.fromDto(device), update, device.getType(), deviceId);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = mapper.convertValue(update, new TypeReference<>() {
            });
            mqttService.publishMqtt(payload, MqttService.TOPIC, deviceId, MqttService.Method.UPDATE);
            return ResponseEntity.ok(Map.of("success", "Device updated successfully"));
        } catch (DeviceNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", String.format("Device ID %s not found", deviceId)));
        } catch (DeviceValidationException | IOException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
