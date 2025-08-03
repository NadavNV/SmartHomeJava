package nv.nadav.smart_home.model.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import nv.nadav.smart_home.validation.Validators;

import java.util.ArrayList;
import java.util.List;

import static nv.nadav.smart_home.constants.Constants.MAX_WATER_TEMP;
import static nv.nadav.smart_home.constants.Constants.MIN_WATER_TEMP;
import static nv.nadav.smart_home.validation.Validators.verifyTypeAndRange;

import nv.nadav.smart_home.validation.Validators.ValidationResult;

public class WaterHeaterParameters extends DeviceParameters {
    private Integer temperature;
    @JsonProperty("target_temperature")
    private Integer targetTemperature;
    @JsonProperty("is_heating")
    private Boolean isHeating;
    @JsonProperty("timer_enabled")
    private Boolean timerEnabled;
    @JsonProperty("scheduled_on")
    private String scheduledOn;
    @JsonProperty("scheduled_off")
    private String scheduledOff;

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getTargetTemperature() {
        return targetTemperature;
    }

    public void setTargetTemperature(int targetTemperature) {
        this.targetTemperature = targetTemperature;
    }

    public boolean isHeating() {
        return isHeating;
    }

    public void setHeating(boolean heating) {
        isHeating = heating;
    }

    public boolean isTimerEnabled() {
        return timerEnabled;
    }

    public void setTimerEnabled(boolean timerEnabled) {
        this.timerEnabled = timerEnabled;
    }

    public String getScheduledOn() {
        return scheduledOn;
    }

    public void setScheduledOn(String scheduledOn) {
        this.scheduledOn = scheduledOn;
    }

    public String getScheduledOff() {
        return scheduledOff;
    }

    public void setScheduledOff(String scheduledOff) {
        this.scheduledOff = scheduledOff;
    }

    @Override
    public ValidationResult validate(boolean isUpdate) {
        List<String> errors = new ArrayList<>();

        if (temperature != null) {
            ValidationResult result = verifyTypeAndRange(temperature, "'temperature'", Integer.class);
            if (!result.isValid()) {
                errors.addAll(result.errorMessages());
            }
        }
        if (targetTemperature != null) {
            ValidationResult result = verifyTypeAndRange(targetTemperature, "'target_temperature'", Integer.class, List.of(MIN_WATER_TEMP, MAX_WATER_TEMP));
            if (!result.isValid()) {
                errors.addAll(result.errorMessages());
            }
        }
        if (isHeating != null) {
            ValidationResult result = verifyTypeAndRange(isHeating, "'is_heating'", Boolean.class);
            if (!result.isValid()) {
                errors.addAll(result.errorMessages());
            }
        }
        if (timerEnabled != null) {
            ValidationResult result = verifyTypeAndRange(timerEnabled, "'timer_enabled'", Boolean.class);
            if (!result.isValid()) {
                errors.addAll(result.errorMessages());
            }
        }
        if (scheduledOn != null) {
            ValidationResult result = verifyTypeAndRange(scheduledOn, "'scheduled_on'", String.class, "time");
            if (!result.isValid()) {
                errors.addAll(result.errorMessages());
            }
        }
        if (scheduledOff != null) {
            ValidationResult result = verifyTypeAndRange(scheduledOff, "'scheduled_off'", String.class, "time");
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
