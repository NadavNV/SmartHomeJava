package nv.nadav.smart_home.model.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import nv.nadav.smart_home.validation.Validators.ValidationResult;

import static nv.nadav.smart_home.validation.Validators.verifyTypeAndRange;
import static nv.nadav.smart_home.constants.Constants.MIN_BATTERY;
import static nv.nadav.smart_home.constants.Constants.MAX_BATTERY;

import java.util.ArrayList;
import java.util.List;

public class DoorLockParameters extends DeviceParameters {
    @JsonProperty("auto_lock_enabled")
    private Boolean autoLockEnabled;
    @JsonProperty("battery_level")
    private Integer batteryLevel;

    public boolean isAutoLockEnabled() {
        return autoLockEnabled;
    }

    public void setAutoLockEnabled(boolean autoLockEnabled) {
        this.autoLockEnabled = autoLockEnabled;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    @Override
    public ValidationResult validate(boolean isUpdate) {
        List<String> errors = new ArrayList<>();

        if (autoLockEnabled != null) {
            ValidationResult result = verifyTypeAndRange(autoLockEnabled, "'auto_lock_enabled'", Boolean.class);
            if (!result.isValid()) {
                errors.addAll(result.errorMessages());
            }
        }
        if (batteryLevel != null) {
            ValidationResult result = verifyTypeAndRange(batteryLevel, "'battery_level'", Integer.class, List.of(MIN_BATTERY, MAX_BATTERY));
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
}
