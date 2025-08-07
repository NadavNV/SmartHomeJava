package nv.nadav.smart_home.model.parameters;

import nv.nadav.smart_home.validation.Validators;
import org.junit.jupiter.api.Test;

import static nv.nadav.smart_home.constants.Constants.MIN_WATER_TEMP;
import static nv.nadav.smart_home.constants.Constants.MAX_WATER_TEMP;
import static org.junit.jupiter.api.Assertions.*;

public class WaterHeaterParametersTest {

    @Test
    void testValidateValid() {
        WaterHeaterParameters parameters = new WaterHeaterParameters();
        parameters.setTargetTemperature((MIN_WATER_TEMP + MAX_WATER_TEMP) / 2);
        parameters.setScheduledOn("06:30");
        parameters.setScheduledOff("08:00");
        assertTrue(parameters.validate(false).isValid());
    }

    @Test
    void testValidateInvalid_TooHot() {
        WaterHeaterParameters parameters = new WaterHeaterParameters();
        parameters.setTargetTemperature(MAX_WATER_TEMP + 1);
        Validators.ValidationResult result = parameters.validate(false);
        assertFalse(result.isValid());
        String error = String.format("'%s' must be between %d and %d, got %d instead.",
                "target_temperature", MIN_WATER_TEMP, MAX_WATER_TEMP, MAX_WATER_TEMP + 1);
        assertTrue(result.errorMessages().contains(error));
    }

    @Test
    void testValidateInvalid_TooCold() {
        WaterHeaterParameters parameters = new WaterHeaterParameters();
        parameters.setTargetTemperature(MIN_WATER_TEMP - 1);
        Validators.ValidationResult result = parameters.validate(false);
        assertFalse(result.isValid());
        String error = String.format("'%s' must be between %d and %d, got %d instead.",
                "target_temperature", MIN_WATER_TEMP, MAX_WATER_TEMP, MIN_WATER_TEMP - 1);
        assertTrue(result.errorMessages().contains(error));
    }

    @Test
    void testValidateInvalid_OnTime() {
        WaterHeaterParameters parameters = new WaterHeaterParameters();
        parameters.setScheduledOn("steve");
        Validators.ValidationResult result = parameters.validate(false);
        assertFalse(result.isValid());
        String error = "'steve' is not a valid ISO format time string.";
        assertTrue(result.errorMessages().contains(error));
    }

    @Test
    void testValidateInvalid_OffTime() {
        WaterHeaterParameters parameters = new WaterHeaterParameters();
        parameters.setScheduledOff("steve");
        Validators.ValidationResult result = parameters.validate(false);
        assertFalse(result.isValid());
        String error = "'steve' is not a valid ISO format time string.";
        assertTrue(result.errorMessages().contains(error));
    }
}
