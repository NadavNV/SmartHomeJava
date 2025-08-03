package nv.nadav.smart_home.exception;

import java.util.List;

public class DeviceValidationException extends RuntimeException {
    public DeviceValidationException(String message) {
        super(message);
    }

    public DeviceValidationException(List<String> errors) {
        super(String.join("\n", errors));
    }
}
