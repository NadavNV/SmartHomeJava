package nv.nadav.smart_home.model.parameters;

import nv.nadav.smart_home.validation.Validators;
import org.junit.jupiter.api.Test;

import static nv.nadav.smart_home.constants.Constants.MAX_BRIGHTNESS;
import static nv.nadav.smart_home.constants.Constants.MIN_BRIGHTNESS;
import static org.junit.jupiter.api.Assertions.*;

public class LightParametersTest {

    @Test
    void testValidateValidBrightness() {
        LightParameters parameters = new LightParameters();
        parameters.setBrightness((MIN_BRIGHTNESS + MAX_BRIGHTNESS) / 2);
        assertTrue(parameters.validate(false).isValid());
    }

    @Test
    void testValidateValid_Color() {
        LightParameters parameters = new LightParameters();
        parameters.setColor("#1A2B3C");
        assertTrue(parameters.validate(false).isValid());
    }

    @Test
    void testValidateInvalid_TooHigh() {
        LightParameters parameters = new LightParameters();
        parameters.setBrightness(MAX_BRIGHTNESS + 1);
        Validators.ValidationResult result = parameters.validate(false);
        assertFalse(result.isValid());
        String error = String.format("'%s' must be between %d and %d, got %d instead.",
                "brightness", MIN_BRIGHTNESS, MAX_BRIGHTNESS, MAX_BRIGHTNESS + 1);
        assertEquals(error, result.errorMessages().getFirst());
    }

    @Test
    void testValidateInvalid_TooLow() {
        LightParameters parameters = new LightParameters();
        parameters.setBrightness(MIN_BRIGHTNESS - 1);
        Validators.ValidationResult result = parameters.validate(false);
        assertFalse(result.isValid());
        String error = String.format("'%s' must be between %d and %d, got %d instead.",
                "brightness", MIN_BRIGHTNESS, MAX_BRIGHTNESS, MIN_BRIGHTNESS - 1);
        assertEquals(error, result.errorMessages().getFirst());
    }

    @Test
    void testValidateInvalid_ReadOnly() {
        LightParameters parameters = new LightParameters();
        parameters.setDimmable(true);
        parameters.setDynamicColor(true);
        Validators.ValidationResult result = parameters.validate(true);
        assertFalse(result.isValid());
        assertTrue(result.errorMessages().contains("Cannot update read-only parameter 'is_dimmable'"));
        assertTrue(result.errorMessages().contains("Cannot update read-only parameter 'dynamic_color'"));
    }

    @Test
    void testValidateInvalid_Color() {
        LightParameters parameters = new LightParameters();
        parameters.setColor("Steve");
        Validators.ValidationResult result = parameters.validate(true);
        assertFalse(result.isValid());
        assertTrue(result.errorMessages().contains("'Steve' is not a valid hex color string."));
    }
}
