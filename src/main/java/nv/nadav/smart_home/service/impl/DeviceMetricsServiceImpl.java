package nv.nadav.smart_home.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.model.parameters.DeviceParameters;
import nv.nadav.smart_home.service.DeviceMetricsService;
import nv.nadav.smart_home.service.DeviceTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.util.concurrent.AtomicDouble;

@Service
public class DeviceMetricsServiceImpl implements DeviceMetricsService {
    private static final Logger logger = LoggerFactory.getLogger("smart_home.device_metrics");
    private final Map<String, AtomicDouble> flagValues = new ConcurrentHashMap<>();
    private final Map<DeviceStatusKey, AtomicDouble> deviceStatusMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicDouble> metadataFlags = new ConcurrentHashMap<>();
    private final Map<String, String> metricDescriptions = new HashMap<>();
    private final DeviceTrackingService trackingService;
    private final MeterRegistry registry;

    public DeviceMetricsServiceImpl(DeviceTrackingService trackingService, MeterRegistry registry) {
        this.trackingService = trackingService;
        this.registry = registry;
        // Device metrics
        metricDescriptions.put("device_metadata", "Key/Value device Metadata");
        metricDescriptions.put("device_status", "Device on/off state");
        metricDescriptions.put("device_on_events_total", "Number of times device turned on");
        metricDescriptions.put("device_usage_seconds_total", "Total on-time in seconds");

        // Air conditioner
        metricDescriptions.put("ac_temperature", "Current temperature (AC)");
        metricDescriptions.put("ac_mode_status", "Current active mode of air conditioners");
        metricDescriptions.put("ac_swing_status", "Current swing mode of air conditioners");
        metricDescriptions.put("ac_fan_status", "Current fan mode of air conditioners");

        // Water heater
        metricDescriptions.put("water_heater_temperature", "Current temperature (water heater)");
        metricDescriptions.put("water_heater_target_temperature", "Target temperature");
        metricDescriptions.put("water_heater_is_heating_status", "Water heater is heating");
        metricDescriptions.put("water_heater_timer_enabled_status", "Water heater timer enabled");
        metricDescriptions.put("water_heater_schedule_info", "Water heater schedule info");

        // Light
        metricDescriptions.put("light_brightness", "Current light brightness");
        metricDescriptions.put("light_color", "Current light color as decimal RGB");
        metricDescriptions.put("light_color_info", "Current light color as label");
        metricDescriptions.put("light_is_dimmable", "Is this light dimmable");
        metricDescriptions.put("light_dynamic_color", "Does this light have dynamic color");

        // Door lock
        metricDescriptions.put("lock_status", "Locked/unlocked status");
        metricDescriptions.put("auto_lock_enabled", "Auto-lock enabled");
        metricDescriptions.put("lock_battery_level", "Battery level");

        // Curtain
        metricDescriptions.put("curtain_status", "Open/closed status");
        metricDescriptions.put("curtain_position", "Current position (%)");
    }

    @Override
    public void addDevice(DeviceDto device) {
        String deviceId = device.getId();
        String deviceType = device.getType().getValue();
        if (trackingService.isDeviceNew(deviceId)) {
            logger.info("Device {} read from the database for the first time.", deviceId);
            logger.info("Adding device metrics.");
            Counter.builder("device_on_events_total")
                    .description(metricDescriptions.get("device_on_events_total"))
                    .tags("device_id", deviceId, "device_type", deviceType)
                    .register(registry)
                    .increment(0);
            Counter.builder("device_usage_seconds")
                    .description(metricDescriptions.get("device_usage_seconds"))
                    .tags("device_id", deviceId, "device_type", deviceType)
                    .register(registry)
                    .increment(0);
            DeviceUpdateDto deviceValues = DeviceUpdateDto.fromDto(device);
            updateDevice(deviceValues, deviceValues, device.getType(), deviceId);
            trackingService.markDeviceSeen(deviceId);
        }
    }

    private record DeviceStatusKey(String id, String type) {
    }

    private void updateDeviceStatus(String deviceId, DeviceType deviceType, String oldStatus, String newStatus) {
        if ("on".equals(newStatus) && (trackingService.isDeviceNew(deviceId) || "off".equals(oldStatus))) {
            trackingService.startNewInterval(deviceId, Instant.now());
            if (!trackingService.isDeviceNew(deviceId)) {
                Counter.builder("device_on_events_total")
                        .description(metricDescriptions.get("device_on_events_total"))
                        .tags("device_id", deviceId, "device_type", deviceType.getValue())
                        .register(registry)
                        .increment();
            }
        }

        if ("off".equals(newStatus) && "on".equals(oldStatus)) {
            double duration = trackingService.closeLastInterval(deviceId, Instant.now());
            Counter.builder("device_usage_seconds_total")
                    .description(metricDescriptions.get("device_usage_seconds_total"))
                    .tags("device_id", deviceId, "device_type", deviceType.getValue())
                    .register(registry)
                    .increment(duration);
        }

        Set<String> activeStates = Set.of("on", "locked", "closed");
        AtomicDouble status = deviceStatusMap.computeIfAbsent(
                new DeviceStatusKey(deviceId, deviceType.getValue()),
                key -> {
                    AtomicDouble initial = new AtomicDouble();
                    Gauge.builder("device_status", initial, AtomicDouble::get)
                            .description(metricDescriptions.get("device_status"))
                            .tag("device_id", key.id())
                            .tag("device_type", key.type())
                            .strongReference(true)
                            .register(registry);
                    return initial;
                });
        status.set(activeStates.contains(newStatus) ? 1.0 : 0.0);
    }

    private void flipDeviceBooleanFlag(String metricName, String deviceId, boolean newValue) {
        String trueKey = metricName + ":" + deviceId + ":True";
        String falseKey = metricName + ":" + deviceId + ":False";

        flagValues.computeIfAbsent(trueKey, _ -> {
            AtomicDouble ref = new AtomicDouble(0.0);
            Gauge.builder(metricName, ref, AtomicDouble::get)
                    .description(metricDescriptions.get(metricName))
                    .tags("device_id", deviceId, "state", "True")
                    .register(registry);
            return ref;
        }).set(newValue ? 1.0 : 0.0);

        flagValues.computeIfAbsent(falseKey, _ -> {
            AtomicDouble ref = new AtomicDouble(0.0);
            Gauge.builder(metricName, ref, AtomicDouble::get)
                    .description(metricDescriptions.get(metricName))
                    .tags("device_id", deviceId, "state", "False")
                    .register(registry);
            return ref;
        }).set(newValue ? 0.0 : 1.0);
    }

    @Override
    public void updateDevice(DeviceUpdateDto oldValues, DeviceUpdateDto update, DeviceType type, String deviceId) {
        String newName = update.getName();
        String newRoom = update.getRoom();
        String newStatus = update.getStatus();
        DeviceParameters newParameters = update.getParameters();

        if (newName != null) {
            String key = "name";
            String oldValue = oldValues.getName();
            String oldMetricKey = deviceId + ":" + key + ":" + oldValue;
            String newMetricKey = deviceId + ":" + key + ":" + newName;
        }
    }

    @Override
    public void deleteDevice(String deviceId) {

    }
}
