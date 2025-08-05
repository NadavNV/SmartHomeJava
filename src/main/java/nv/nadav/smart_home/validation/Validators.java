package nv.nadav.smart_home.validation;


import jakarta.validation.Valid;
import nv.nadav.smart_home.dto.DeviceDto;
import nv.nadav.smart_home.model.DeviceType;
import nv.nadav.smart_home.dto.DeviceUpdateDto;
import nv.nadav.smart_home.model.parameters.DeviceParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nv.nadav.smart_home.constants.Constants.TIME_REGEX;
import static nv.nadav.smart_home.constants.Constants.COLOR_REGEX;
import static nv.nadav.smart_home.constants.Constants.AC_STATUSES;
import static nv.nadav.smart_home.constants.Constants.CURTAIN_STATUSES;
import static nv.nadav.smart_home.constants.Constants.DOOR_LOCK_STATUSES;
import static nv.nadav.smart_home.constants.Constants.LIGHT_STATUSES;
import static nv.nadav.smart_home.constants.Constants.WATER_HEATER_STATUSES;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Validators {
    private static final Logger logger = LoggerFactory.getLogger("smart_home.validation");

    public static boolean verifyTimeString(String s) {
        return Pattern.compile(TIME_REGEX).matcher(s).matches();
    }

    public static ValidationResult verifyTypeAndRange(Object value, String name, Class<?> cls, Object valueRange) {
        if (cls == Integer.class || cls == int.class) {
            int intValue;
            try {
                if (value instanceof String) {
                    intValue = Integer.parseInt((String) value);
                } else if (value instanceof Integer) {
                    intValue = (int) value;
                } else {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                String error = String.format("'%s' must be a numeric string, got '%s' instead.", name, value);
                logger.error(error);
                return new ValidationResult(false, List.of(error));
            }

            if (valueRange instanceof int[] range && range.length == 2) {
                int min = range[0];
                int max = range[1];
                if (intValue < min || intValue > max) {
                    String error = String.format("'%s' must be between %d and %d, got %d instead.", name, min, max, intValue);
                    logger.error(error);
                    return new ValidationResult(false, List.of(error));
                }
            }
            return new ValidationResult(true, null);
        }

        if (!cls.isInstance(value)) {
            String error = name + " must be a " + cls.getSimpleName() + ", got " + value.getClass().getSimpleName() + " instead.";
            logger.error(error);
            return new ValidationResult(false, List.of(error));
        }

        if (cls == String.class && value instanceof String strValue) {
            if (valueRange instanceof Set<?> set) {
                if (!set.contains(strValue)) {
                    String error = String.format(
                            "'%s' is not a valid value for %s. Must be one of %s.", strValue, name, set
                    );
                    logger.error(error);
                    return new ValidationResult(false, List.of(error));
                }
            } else if ("time".equals(valueRange)) {
                if (!Pattern.compile(TIME_REGEX).matcher(strValue).matches()) {
                    String error = "'" + strValue + "' is not a valid ISO format time string.";
                    logger.error(error);
                    return new ValidationResult(false, List.of(error));
                }
            } else if ("color".equals(valueRange)) {
                if (!Pattern.compile(COLOR_REGEX).matcher(strValue).matches()) {
                    String error = "'" + strValue + "' is not a valid hex color string.";
                    logger.error(error);
                    return new ValidationResult(false, List.of(error));
                }
            }
        }
        return new ValidationResult(true, null);
    }

    // Overload for optional valueRange
    public static ValidationResult verifyTypeAndRange(Object value, String name, Class<?> cls) {
        return verifyTypeAndRange(value, name, cls, null);
    }

    public record ValidationResult(boolean isValid, List<String> errorMessages) {
    }

    private static ValidationResult validateStatus(String status, DeviceType deviceType) {
        List<String> errors = new ArrayList<>();

        switch (deviceType) {
            case DOOR_LOCK -> {
                ValidationResult result = verifyTypeAndRange(status, "'status'", String.class, DOOR_LOCK_STATUSES);
                if (!result.isValid) {
                    errors.addAll(result.errorMessages);
                }
            }
            case CURTAIN -> {
                ValidationResult result = verifyTypeAndRange(status, "'status'", String.class, CURTAIN_STATUSES);
                if (!result.isValid) {
                    errors.addAll(result.errorMessages);
                }
            }
            case AIR_CONDITIONER -> {
                ValidationResult result = verifyTypeAndRange(status, "'status'", String.class, AC_STATUSES);
                if (!result.isValid) {
                    errors.addAll(result.errorMessages);
                }
            }
            case LIGHT -> {
                ValidationResult result = verifyTypeAndRange(status, "'status'", String.class, LIGHT_STATUSES);
                if (!result.isValid) {
                    errors.addAll(result.errorMessages);
                }
            }
            case WATER_HEATER -> {
                ValidationResult result = verifyTypeAndRange(status, "'status'", String.class, WATER_HEATER_STATUSES);
                if (!result.isValid) {
                    errors.addAll(result.errorMessages);
                }
            }
            default -> {
                ValidationResult result = verifyTypeAndRange(status, "'status'", String.class, Set.of("on", "off"));
                if (!result.isValid) {
                    errors.addAll(result.errorMessages);
                }
            }
        }
        if (errors.isEmpty()) {
            return new ValidationResult(true, null);
        } else {
            return new ValidationResult(false, errors);
        }
    }

    public static ValidationResult validateNewDeviceData(@Valid DeviceDto data) {
        List<String> errors = new ArrayList<>();
        ValidationResult result = validateStatus(data.getStatus(), data.getType());
        if (!result.isValid) {
            errors.addAll(result.errorMessages);
        }
        result = data.getParameters().validate(false);
        if (!result.isValid) {
            errors.addAll(result.errorMessages);
        }

        if (errors.isEmpty()) {
            return new ValidationResult(true, null);
        } else {
            return new ValidationResult(false, errors);
        }
    }

    public static ValidationResult validateDeviceData(DeviceUpdateDto data, DeviceType deviceType) {
        List<String> errors = new ArrayList<>();

        String status = data.getStatus();
        if (status != null) {
            ValidationResult result = validateStatus(status, deviceType);
            if (!result.isValid) {
                errors.addAll(result.errorMessages);
            }
        }

        DeviceParameters parameters = data.getParameters();
        if (parameters != null) {
            ValidationResult result = parameters.validate(true);
            if (!result.isValid) {
                errors.addAll(result.errorMessages);
            }
        }

        if (errors.isEmpty()) {
            return new ValidationResult(true, null);
        } else {
            return new ValidationResult(false, errors);
        }
    }
}
