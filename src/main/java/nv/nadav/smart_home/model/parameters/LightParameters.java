package nv.nadav.smart_home.model.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import nv.nadav.smart_home.validation.Validators;

import java.util.ArrayList;
import java.util.List;

import static nv.nadav.smart_home.constants.Constants.MAX_BRIGHTNESS;
import static nv.nadav.smart_home.constants.Constants.MIN_BRIGHTNESS;
import static nv.nadav.smart_home.validation.Validators.verifyTypeAndRange;

import nv.nadav.smart_home.validation.Validators.ValidationResult;

public class LightParameters extends DeviceParameters {
    private Integer brightness;
    private String color;
    @JsonProperty("is_dimmable")
    private Boolean isDimmable;
    @JsonProperty("dynamic_color")
    private Boolean dynamicColor;

    public Integer getBrightness() {
        return brightness;
    }

    public void setBrightness(Integer brightness) {
        this.brightness = brightness;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Boolean isDimmable() {
        return isDimmable;
    }

    public void setDimmable(Boolean dimmable) {
        isDimmable = dimmable;
    }

    public Boolean isDynamicColor() {
        return dynamicColor;
    }

    public void setDynamicColor(Boolean dynamicColor) {
        this.dynamicColor = dynamicColor;
    }

    @Override
    public Validators.ValidationResult validate(boolean isUpdate) {
        List<String> errors = new ArrayList<>();

        if (color != null) {
            ValidationResult result = verifyTypeAndRange(color, "color", String.class, "color");
            if (!result.isValid()) {
                errors.addAll(result.errorMessages());
            }
        }
        if (brightness != null) {
            ValidationResult result = verifyTypeAndRange(brightness, "brightness", Integer.class, List.of(MIN_BRIGHTNESS, MAX_BRIGHTNESS));
            if (!result.isValid()) {
                errors.addAll(result.errorMessages());
            }
        }
        if (isDimmable != null) {
            if (isUpdate) {
                errors.add("Cannot update read-only parameter 'is_dimmable'");
            } else {
                ValidationResult result = verifyTypeAndRange(isDimmable, "is_dimmable", Boolean.class);
                if (!result.isValid()) {
                    errors.addAll(result.errorMessages());
                }
            }
        }
        if (dynamicColor != null) {
            if (isUpdate) {
                errors.add("Cannot update read-only parameter 'dynamic_color'");
            } else {
                ValidationResult result = verifyTypeAndRange(dynamicColor, "dynamic_color", Boolean.class);
                if (!result.isValid()) {
                    errors.addAll(result.errorMessages());
                }
            }
        }

        if (errors.isEmpty()) {
            return new Validators.ValidationResult(true, null);
        } else {
            return new Validators.ValidationResult(false, errors);
        }
    }
}
