package nv.nadav.smart_home.model.parameters;

import nv.nadav.smart_home.validation.Validators;
import org.junit.jupiter.api.Test;

import static nv.nadav.smart_home.constants.Constants.MIN_BATTERY;
import static nv.nadav.smart_home.constants.Constants.MAX_BATTERY;
import static org.junit.jupiter.api.Assertions.*;

public class DoorLockParametersTest {

    @Test
    void testValidateValid() {
        DoorLockParameters parameters = new DoorLockParameters();
        parameters.setBatteryLevel((MIN_BATTERY + MAX_BATTERY) / 2);
        assertTrue(parameters.validate(false).isValid());
    }

    @Test
    void testValidateInvalid_TooHigh() {
        DoorLockParameters parameters = new DoorLockParameters();
        parameters.setBatteryLevel(MAX_BATTERY + 1);
        Validators.ValidationResult result = parameters.validate(false);
        assertFalse(result.isValid());
        String error = String.format("'%s' must be between %d and %d, got %d instead.",
                "battery_level", MIN_BATTERY, MAX_BATTERY, MAX_BATTERY + 1);
        assertEquals(error, result.errorMessages().getFirst());
    }

    @Test
    void testValidateInvalid_TooLow() {
        DoorLockParameters parameters = new DoorLockParameters();
        parameters.setBatteryLevel(MIN_BATTERY - 1);
        Validators.ValidationResult result = parameters.validate(false);
        assertFalse(result.isValid());
        String error = String.format("'%s' must be between %d and %d, got %d instead.",
                "battery_level", MIN_BATTERY, MAX_BATTERY, MIN_BATTERY - 1);
        assertEquals(error, result.errorMessages().getFirst());
    }
}
