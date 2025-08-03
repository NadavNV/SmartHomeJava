package nv.nadav.smart_home.model.parameters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nv.nadav.smart_home.validation.Validators.ValidationResult;

import static nv.nadav.smart_home.constants.Constants.MIN_AC_TEMP;
import static nv.nadav.smart_home.constants.Constants.MAX_AC_TEMP;

import java.util.ArrayList;
import java.util.List;

import static nv.nadav.smart_home.validation.Validators.verifyTypeAndRange;

public class AirConditionerParameters extends DeviceParameters {
    private Integer temperature;
    private Mode mode;
    private FanSpeed fanSpeed;
    private Swing swing;

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public FanSpeed getFanSpeed() {
        return fanSpeed;
    }

    public void setFanSpeed(FanSpeed fanSpeed) {
        this.fanSpeed = fanSpeed;
    }

    public Swing getSwing() {
        return swing;
    }

    public void setSwing(Swing swing) {
        this.swing = swing;
    }

    @Override
    public ValidationResult validate(boolean isUpdate) {
        List<String> errors = new ArrayList<>();
        if (temperature != null) {
            ValidationResult result = verifyTypeAndRange(temperature, "'temperature'", Integer.class, List.of(MIN_AC_TEMP, MAX_AC_TEMP));
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

    public enum Mode {
        COOL("cool"),
        HEAT("heat"),
        FAN("fan");

        private final String value;

        Mode(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static Mode fromValue(String value) {
            for (Mode mode : Mode.values()) {
                if (mode.value.equalsIgnoreCase(value)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Unknown mode: " + value);
        }
    }

    public enum FanSpeed {
        OFF("off"),
        LOW("low"),
        MEDIUM("medium"),
        HIGH("high");

        private final String value;

        FanSpeed(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static FanSpeed fromValue(String value) {
            for (FanSpeed speed : FanSpeed.values()) {
                if (speed.value.equalsIgnoreCase(value)) {
                    return speed;
                }
            }
            throw new IllegalArgumentException("Unknown fan speed: " + value);
        }
    }

    public enum Swing {
        OFF("off"),
        ON("on"),
        AUTO("auto");

        private final String value;

        Swing(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static Swing fromValue(String value) {
            for (Swing swing : Swing.values()) {
                if (swing.value.equalsIgnoreCase(value)) {
                    return swing;
                }
            }
            throw new IllegalArgumentException("Unknown swing: " + value);
        }
    }
}
