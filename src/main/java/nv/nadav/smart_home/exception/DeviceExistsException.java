package nv.nadav.smart_home.exception;

public class DeviceExistsException extends RuntimeException {
    public DeviceExistsException() {
        super();
    }

    public DeviceExistsException(String message) {
        super(message);
    }
}
