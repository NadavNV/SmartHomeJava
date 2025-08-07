package nv.nadav.smart_home.model.parameters;

import nv.nadav.smart_home.validation.Validators;
import org.junit.jupiter.api.Test;

import static nv.nadav.smart_home.constants.Constants.MIN_POSITION;
import static nv.nadav.smart_home.constants.Constants.MAX_POSITION;
import static org.junit.jupiter.api.Assertions.*;

public class CurtainParametersTest {

    @Test
    void testValidateValid() {
        CurtainParameters parameters = new CurtainParameters();
        parameters.setPosition((MIN_POSITION + MAX_POSITION) / 2);
        assertTrue(parameters.validate(false).isValid());
    }

    @Test
    void testValidateInvalid_TooHigh() {
        CurtainParameters parameters = new CurtainParameters();
        parameters.setPosition(MAX_POSITION + 1);
        Validators.ValidationResult result = parameters.validate(false);
        assertFalse(result.isValid());
        String error = String.format("'%s' must be between %d and %d, got %d instead.",
                "position", MIN_POSITION, MAX_POSITION, MAX_POSITION + 1);
        assertEquals(error, result.errorMessages().getFirst());
    }

    @Test
    void testValidateInvalid_TooLow() {
        CurtainParameters parameters = new CurtainParameters();
        parameters.setPosition(MIN_POSITION - 1);
        Validators.ValidationResult result = parameters.validate(false);
        assertFalse(result.isValid());
        String error = String.format("'%s' must be between %d and %d, got %d instead.",
                "position", MIN_POSITION, MAX_POSITION, MIN_POSITION - 1);
        assertEquals(error, result.errorMessages().getFirst());
    }
}
