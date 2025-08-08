package nv.nadav.smart_home.service.impl;

import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.model.parameters.*;
import nv.nadav.smart_home.model.parameters.AirConditionerParameters.*;
import nv.nadav.smart_home.service.CounterManager;
import nv.nadav.smart_home.service.DeviceMetricsService;
import nv.nadav.smart_home.service.DeviceTrackingService;
import nv.nadav.smart_home.service.GaugeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class DeviceMetricsServiceImpl implements DeviceMetricsService {
    private static final Logger logger = LoggerFactory.getLogger("smart_home.device_metrics");
    private final GaugeManager gaugeManager;
    private final CounterManager counterManager;
    private final Map<String, String> metricDescriptions = new HashMap<>();
    private final DeviceTrackingService trackingService;

    public DeviceMetricsServiceImpl(
            DeviceTrackingService trackingService,
            GaugeManager gaugeManager,
            CounterManager counterManager
    ) {
        this.trackingService = trackingService;
        this.gaugeManager = gaugeManager;
        this.counterManager = counterManager;
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
        if (trackingService.isDeviceNew(deviceId)) {
            logger.info("Device {} read from the database for the first time.", deviceId);
            logger.info("Adding device metrics.");
            String deviceType = device.getType().getValue();
            counterManager.incrementBy(
                    "device_on_events_total",
                    metricDescriptions.get("device_on_events_total"),
                    Map.of("device_id", deviceId, "device_type", deviceType),
                    0.0
            );
            counterManager.incrementBy(
                    "device_usage_seconds_total",
                    metricDescriptions.get("device_usage_seconds_total"),
                    Map.of("device_id", deviceId, "device_type", deviceType),
                    0.0
            );
            DeviceUpdateDto deviceValues = DeviceUpdateDto.fromDto(device);
            updateDevice(deviceValues, deviceValues, device.getType(), deviceId);
            trackingService.markDeviceSeen(deviceId);
        }
    }

    private void updateDeviceStatus(String deviceId, DeviceType deviceType, String oldStatus, String newStatus) {
        if ("on".equals(newStatus) && (trackingService.isDeviceNew(deviceId) || "off".equals(oldStatus))) {
            trackingService.startNewInterval(deviceId, Instant.now());
            if (!trackingService.isDeviceNew(deviceId)) {
                counterManager.increment(
                        "device_on_events_total",
                        metricDescriptions.get("device_on_events_total"),
                        Map.of("device_id", deviceId, "device_type", deviceType.getValue())
                );
            }
        }

        if ("off".equals(newStatus) && "on".equals(oldStatus)) {
            double duration = trackingService.closeLastInterval(deviceId, Instant.now());
            counterManager.incrementBy(
                    "device_usage_seconds_total",
                    metricDescriptions.get("device_usage_seconds_total"),
                    Map.of("device_id", deviceId, "device_type", deviceType.getValue()),
                    duration
            );
        }

        Set<String> activeStates = Set.of("on", "locked", "closed");
        switch (deviceType) {
            case LIGHT, WATER_HEATER, AIR_CONDITIONER -> gaugeManager.setNumericGauge(
                    "device_status",
                    metricDescriptions.get("device_status"),
                    activeStates.contains(newStatus) ? 1.0 : 0.0,
                    Map.of("device_id", deviceId, "device_type", deviceType.getValue())
            );
            case DOOR_LOCK -> gaugeManager.setNumericGauge(
                    "lock_status",
                    metricDescriptions.get("lock_status"),
                    activeStates.contains(newStatus) ? 1.0 : 0.0,
                    Map.of("device_id", deviceId)
            );
            case CURTAIN -> gaugeManager.setNumericGauge(
                    "curtain_status",
                    metricDescriptions.get("curtain_status"),
                    activeStates.contains(newStatus) ? 1.0 : 0.0,
                    Map.of("device_id", deviceId)
            );
        }
    }

    private void flipDeviceBooleanFlag(String metricName, String deviceId, boolean newValue) {
        gaugeManager.setBooleanGauge(
                metricName,
                metricDescriptions.get(metricName),
                newValue,
                Map.of("device_id", deviceId, "state", "True")
        );
        gaugeManager.setBooleanGauge(
                metricName,
                metricDescriptions.get(metricName),
                !newValue,
                Map.of("device_id", deviceId, "state", "False")
        );
    }

    private void updateDeviceMetadata(String key, String oldValue, String newValue, String deviceId) {
        gaugeManager.setBooleanGauge(
                "device_metadata",
                metricDescriptions.get("device_metadata"),
                false,
                Map.of("device_id", deviceId, "key", key, "value", oldValue)
        );
        gaugeManager.setBooleanGauge(
                "device_metadata",
                metricDescriptions.get("device_metadata"),
                true,
                Map.of("device_id", deviceId, "key", key, "value", newValue)
        );
    }

    private void updateDeviceParameters(DeviceParameters newParameters, DeviceParameters oldParameters, DeviceType type, String deviceId) {
        switch (type) {
            case LIGHT -> {
                LightParameters newLightParameters = (LightParameters) newParameters;
                LightParameters oldLightParameters = (LightParameters) oldParameters;
                Integer newBrightness = newLightParameters.getBrightness();
                String newColor = newLightParameters.getColor();
                Boolean newIsDimmable = newLightParameters.isDimmable();
                Boolean newDynamicColor = newLightParameters.isDynamicColor();
                if (newBrightness != null) {
                    gaugeManager.setNumericGauge(
                            "light_brightness",
                            metricDescriptions.get("light_brightness"),
                            newBrightness,
                            Map.of(
                                    "device_id", deviceId,
                                    "is_dimmable", Boolean.toString(newIsDimmable != null ? newIsDimmable : oldLightParameters.isDimmable())
                            )
                    );
                }
                if (newColor != null) {
                    String hex = newColor.startsWith("#") ? newColor.substring(1) : newColor;
                    gaugeManager.setNumericGauge(
                            "light_color",
                            metricDescriptions.get("light_color"),
                            Integer.parseInt(hex, 16),
                            Map.of(
                                    "device_id", deviceId,
                                    "dynamic_color", Boolean.toString(newDynamicColor != null ? newDynamicColor : oldLightParameters.isDynamicColor())
                            )
                    );

                    gaugeManager.setBooleanGauge(
                            "light_color_info",
                            metricDescriptions.get("light_color_info"),
                            false,
                            Map.of(
                                    "device_id", deviceId,
                                    "dynamic_color", Boolean.toString(oldLightParameters.isDynamicColor()),
                                    "color", oldLightParameters.getColor()
                            )
                    );
                    gaugeManager.setBooleanGauge(
                            "light_color_info",
                            metricDescriptions.get("light_color_info"),
                            true,
                            Map.of(
                                    "device_id", deviceId,
                                    "dynamic_color", Boolean.toString(newDynamicColor != null ? newDynamicColor : oldLightParameters.isDynamicColor()),
                                    "color", newColor
                            )
                    );
                    if (newIsDimmable != null) {
                        flipDeviceBooleanFlag(
                                "light_is_dimmable",
                                deviceId,
                                newIsDimmable
                        );
                    }
                    if (newDynamicColor != null) {
                        flipDeviceBooleanFlag(
                                "light_dynamic_color",
                                deviceId,
                                newDynamicColor
                        );
                    }
                }
            }
            case WATER_HEATER -> {
                WaterHeaterParameters newWaterHeaterParameters = (WaterHeaterParameters) newParameters;
                WaterHeaterParameters oldWaterHeaterParameters = (WaterHeaterParameters) oldParameters;
                Integer newTemperature = newWaterHeaterParameters.getTemperature();
                Integer newTargetTemperature = newWaterHeaterParameters.getTargetTemperature();
                Boolean newIsHeating = newWaterHeaterParameters.isHeating();
                Boolean newTimerEnabled = newWaterHeaterParameters.isTimerEnabled();
                String newScheduledOn = newWaterHeaterParameters.getScheduledOn();
                String newScheduledOff = newWaterHeaterParameters.getScheduledOff();
                if (newTemperature != null) {
                    gaugeManager.setNumericGauge(
                            "water_heater_temperature",
                            metricDescriptions.get("water_heater_temperature"),
                            newTemperature.doubleValue(),
                            Map.of("device_id", deviceId)
                    );
                }
                if (newTargetTemperature != null) {
                    gaugeManager.setNumericGauge(
                            "water_heater_target_temperature",
                            metricDescriptions.get("water_heater_target_temperature"),
                            newTargetTemperature.doubleValue(),
                            Map.of("device_id", deviceId)
                    );
                }
                if (newIsHeating != null) {
                    flipDeviceBooleanFlag(
                            "water_heater_is_heating_status",
                            deviceId,
                            newIsHeating
                    );
                }
                if (newTimerEnabled != null) {
                    flipDeviceBooleanFlag(
                            "water_heater_timer_enabled_status",
                            deviceId,
                            newTimerEnabled
                    );
                }
                if (newScheduledOn != null && newScheduledOff != null) {
                    // Mark the old values as stale
                    gaugeManager.setNumericGauge(
                            "water_heater_schedule_info",
                            metricDescriptions.get("water_heater_schedule_info"),
                            0.0,
                            Map.of(
                                    "device_id", deviceId,
                                    "scheduled_on", oldWaterHeaterParameters.getScheduledOn(),
                                    "scheduled_off", oldWaterHeaterParameters.getScheduledOff()
                            )
                    );
                    gaugeManager.setNumericGauge(
                            "water_heater_schedule_info",
                            metricDescriptions.get("water_heater_schedule_info"),
                            1.0,
                            Map.of(
                                    "device_id", deviceId,
                                    "scheduled_on", newScheduledOn,
                                    "scheduled_off", newScheduledOff
                            )
                    );
                } else if (newScheduledOn != null) {
                    // Mark the old values as stale
                    gaugeManager.setNumericGauge(
                            "water_heater_schedule_info",
                            metricDescriptions.get("water_heater_schedule_info"),
                            0.0,
                            Map.of(
                                    "device_id", deviceId,
                                    "scheduled_on", oldWaterHeaterParameters.getScheduledOn(),
                                    "scheduled_off", oldWaterHeaterParameters.getScheduledOff()
                            )
                    );
                    gaugeManager.setNumericGauge(
                            "water_heater_schedule_info",
                            metricDescriptions.get("water_heater_schedule_info"),
                            1.0,
                            Map.of(
                                    "device_id", deviceId,
                                    "scheduled_on", newScheduledOn,
                                    "scheduled_off", oldWaterHeaterParameters.getScheduledOff()
                            )
                    );
                } else if (newScheduledOff != null) {
                    // Mark the old values as stale
                    gaugeManager.setNumericGauge(
                            "water_heater_schedule_info",
                            metricDescriptions.get("water_heater_schedule_info"),
                            0.0,
                            Map.of(
                                    "device_id", deviceId,
                                    "scheduled_on", oldWaterHeaterParameters.getScheduledOn(),
                                    "scheduled_off", oldWaterHeaterParameters.getScheduledOff()
                            )
                    );
                    gaugeManager.setNumericGauge(
                            "water_heater_schedule_info",
                            metricDescriptions.get("water_heater_schedule_info"),
                            1.0,
                            Map.of(
                                    "device_id", deviceId,
                                    "scheduled_on", oldWaterHeaterParameters.getScheduledOn(),
                                    "scheduled_off", newScheduledOff
                            )
                    );
                }
            }
            case AIR_CONDITIONER -> {
                AirConditionerParameters newAcParameters = (AirConditionerParameters) newParameters;
                Integer newTemperature = newAcParameters.getTemperature();
                Mode newMode = newAcParameters.getMode();
                FanSpeed newFanSpeed = newAcParameters.getFanSpeed();
                Swing newSwing = newAcParameters.getSwing();
                if (newTemperature != null) {
                    gaugeManager.setNumericGauge(
                            "ac_temperature",
                            metricDescriptions.get("ac_temperature"),
                            newTemperature.doubleValue(),
                            Map.of("device_id", deviceId)
                    );
                }
                if (newMode != null) {
                    gaugeManager.setEnumGauge(
                            "ac_mode_status",
                            metricDescriptions.get("ac_mode_status"),
                            newMode,
                            Map.of("device_id", deviceId)
                    );
                }
                if (newFanSpeed != null) {
                    gaugeManager.setEnumGauge(
                            "ac_fan_status",
                            metricDescriptions.get("ac_fan_status"),
                            newFanSpeed,
                            Map.of("device_id", deviceId)
                    );
                }
                if (newSwing != null) {
                    gaugeManager.setEnumGauge(
                            "ac_swing_status",
                            metricDescriptions.get("ac_swing_status"),
                            newSwing,
                            Map.of("device_id", deviceId)
                    );
                }
            }
            case DOOR_LOCK -> {
                DoorLockParameters newLockParameters = (DoorLockParameters) newParameters;
                Integer newBatteryLevel = newLockParameters.getBatteryLevel();
                Boolean newAutoLockEnabled = newLockParameters.isAutoLockEnabled();
                if (newBatteryLevel != null) {
                    gaugeManager.setNumericGauge(
                            "lock_battery_level",
                            metricDescriptions.get("lock_battery_level"),
                            newBatteryLevel.doubleValue(),
                            Map.of("device_id", deviceId)
                    );
                }
                if (newAutoLockEnabled != null) {
                    flipDeviceBooleanFlag(
                            "auto_lock_enabled",
                            deviceId,
                            newAutoLockEnabled
                    );
                }
            }
            case CURTAIN -> {
                CurtainParameters newCurtainParameters = (CurtainParameters) newParameters;
                Integer newPosition = newCurtainParameters.getPosition();
                if (newPosition != null) {
                    gaugeManager.setNumericGauge(
                            "curtain_position",
                            metricDescriptions.get("curtain_position"),
                            newPosition,
                            Map.of("device_id", deviceId)
                    );
                }
            }
        }
    }

    @Override
    public void updateDevice(DeviceUpdateDto oldValues, DeviceUpdateDto update, DeviceType type, String deviceId) {
        String newName = update.getName();
        String newRoom = update.getRoom();
        String newStatus = update.getStatus();
        DeviceParameters newParameters = update.getParameters();

        if (newName != null) {
            updateDeviceMetadata("name", oldValues.getName(), newName, deviceId);
        }
        if (newRoom != null) {
            updateDeviceMetadata("room", oldValues.getRoom(), newRoom, deviceId);
        }
        if (newStatus != null) {
            updateDeviceStatus(deviceId, type, oldValues.getStatus(), newStatus);
        }
        if (newParameters != null) {
            updateDeviceParameters(newParameters, oldValues.getParameters(), type, deviceId);
        }
    }

    @Override
    public void deleteDevice(String deviceId) {
        try {
            trackingService.closeLastInterval(deviceId, Instant.now());
        } catch (IllegalStateException _) {
            // Ignore if nothing to close
        }
        trackingService.removeDeviceSeen(deviceId);
    }
}
