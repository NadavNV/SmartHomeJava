package nv.nadav.smart_home.model.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import nv.nadav.smart_home.validation.Validators.ValidationResult;

import static nv.nadav.smart_home.validation.Validators.verifyTypeAndRange;
import static nv.nadav.smart_home.constants.Constants.MIN_BATTERY;
import static nv.nadav.smart_home.constants.Constants.MAX_BATTERY;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DoorLockParameters extends DeviceParameters {
    @JsonProperty("auto_lock_enabled")
    private Boolean autoLockEnabled;
    @JsonProperty("battery_level")
    private Integer batteryLevel;

    public Boolean isAutoLockEnabled() {
        return autoLockEnabled;
    }

    public void setAutoLockEnabled(Boolean autoLockEnabled) {
        this.autoLockEnabled = autoLockEnabled;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    @Override
    public ValidationResult validate(boolean isUpdate) {
        List<String> errors = new ArrayList<>();

        if (batteryLevel != null) {
            ValidationResult result = verifyTypeAndRange(batteryLevel, "battery_level", Integer.class, List.of(MIN_BATTERY, MAX_BATTERY));
            if (!result.isValid()) {
                errors.addAll(result.errorMessages());
            }
        }

        if (errors.isEmpty()) {
            return new ValidationResult(true, null);
        } else {
            return new ValidationResult(false, errors);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            // Same reference
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            // null or different class
            return false;
        }

        DoorLockParameters other = (DoorLockParameters) obj;
        return (Objects.equals(this.autoLockEnabled, other.isAutoLockEnabled()) &&
                Objects.equals(this.batteryLevel, other.getBatteryLevel())
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(autoLockEnabled, batteryLevel);
    }

    @Override
    public String toString() {
        String result = Stream.of(
                        Map.entry("Auto-Lock Enabled", autoLockEnabled),
                        Map.entry("Battery Level", batteryLevel)
                )
                .filter(e -> e.getValue() != null)
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
        return ((result.equals("{}")) ? "{Empty}" : result);
    }
}
