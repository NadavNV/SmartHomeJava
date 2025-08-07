package nv.nadav.smart_home.model.parameters;

import nv.nadav.smart_home.validation.Validators;
import org.junit.jupiter.api.Test;

import static nv.nadav.smart_home.constants.Constants.MIN_AC_TEMP;
import static nv.nadav.smart_home.constants.Constants.MAX_AC_TEMP;
import static org.junit.jupiter.api.Assertions.*;

public class AirConditionerParametersTest {

    @Test
    void testValidateValid() {
        AirConditionerParameters parameters = new AirConditionerParameters();
        parameters.setTemperature((MIN_AC_TEMP + MAX_AC_TEMP) / 2);
        assertTrue(parameters.validate(false).isValid());
    }

    @Test
    void testValidateInvalid_TooHot() {
        AirConditionerParameters parameters = new AirConditionerParameters();
        parameters.setTemperature(MAX_AC_TEMP + 1);
        Validators.ValidationResult result = parameters.validate(false);
        assertFalse(result.isValid());
        String error = String.format("'%s' must be between %d and %d, got %d instead.",
                "temperature", MIN_AC_TEMP, MAX_AC_TEMP, MAX_AC_TEMP + 1);
        assertEquals(error, result.errorMessages().getFirst());
    }

    @Test
    void testValidateInvalid_TooCold() {
        AirConditionerParameters parameters = new AirConditionerParameters();
        parameters.setTemperature(MIN_AC_TEMP - 1);
        Validators.ValidationResult result = parameters.validate(false);
        assertFalse(result.isValid());
        String error = String.format("'%s' must be between %d and %d, got %d instead.",
                "temperature", MIN_AC_TEMP, MAX_AC_TEMP, MIN_AC_TEMP - 1);
        assertEquals(error, result.errorMessages().getFirst());
    }

    @Test
    void testModeFromValueValid() {
        assertEquals(AirConditionerParameters.Mode.COOL, AirConditionerParameters.Mode.fromValue("cool"));
        assertEquals(AirConditionerParameters.Mode.HEAT, AirConditionerParameters.Mode.fromValue("HeAt"));
    }

    @Test
    void testModeFromValueInvalid() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                AirConditionerParameters.Mode.fromValue("Steve"));
        assertTrue(exception.getMessage().contains("Unknown mode: Steve"));
    }

    @Test
    void testModeToString() {
        assertEquals("cool", AirConditionerParameters.Mode.COOL.toString());
        assertEquals("cool", AirConditionerParameters.Mode.COOL.getValue());

        assertEquals("heat", AirConditionerParameters.Mode.HEAT.toString());
        assertEquals("heat", AirConditionerParameters.Mode.HEAT.getValue());

        assertEquals("fan", AirConditionerParameters.Mode.FAN.toString());
        assertEquals("fan", AirConditionerParameters.Mode.FAN.getValue());
    }

    @Test
    void testFanSpeedFromValueValid() {
        assertEquals(AirConditionerParameters.FanSpeed.LOW, AirConditionerParameters.FanSpeed.fromValue("low"));
        assertEquals(AirConditionerParameters.FanSpeed.MEDIUM, AirConditionerParameters.FanSpeed.fromValue("MeDiUm"));
    }

    @Test
    void testFanSpeedFromValueInvalid() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                AirConditionerParameters.FanSpeed.fromValue("Steve"));
        assertTrue(exception.getMessage().contains("Unknown fan speed: Steve"));
    }

    @Test
    void testFanSpeedToString() {
        assertEquals("off", AirConditionerParameters.FanSpeed.OFF.toString());
        assertEquals("off", AirConditionerParameters.FanSpeed.OFF.getValue());

        assertEquals("low", AirConditionerParameters.FanSpeed.LOW.toString());
        assertEquals("low", AirConditionerParameters.FanSpeed.LOW.getValue());

        assertEquals("medium", AirConditionerParameters.FanSpeed.MEDIUM.toString());
        assertEquals("medium", AirConditionerParameters.FanSpeed.MEDIUM.getValue());

        assertEquals("high", AirConditionerParameters.FanSpeed.HIGH.toString());
        assertEquals("high", AirConditionerParameters.FanSpeed.HIGH.getValue());
    }

    @Test
    void testSwingFromValueValid() {
        assertEquals(AirConditionerParameters.Swing.ON, AirConditionerParameters.Swing.fromValue("on"));
        assertEquals(AirConditionerParameters.Swing.AUTO, AirConditionerParameters.Swing.fromValue("aUtO"));
    }

    @Test
    void testSwingFromValueInvalid() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                AirConditionerParameters.Swing.fromValue("Steve"));
        assertTrue(exception.getMessage().contains("Unknown swing: Steve"));
    }

    @Test
    void testSwingToString() {
        assertEquals("on", AirConditionerParameters.Swing.ON.toString());
        assertEquals("on", AirConditionerParameters.Swing.ON.getValue());

        assertEquals("off", AirConditionerParameters.Swing.OFF.toString());
        assertEquals("off", AirConditionerParameters.Swing.OFF.getValue());

        assertEquals("auto", AirConditionerParameters.Swing.AUTO.toString());
        assertEquals("auto", AirConditionerParameters.Swing.AUTO.getValue());
    }
}
