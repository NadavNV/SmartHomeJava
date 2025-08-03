package nv.nadav.smart_home.exception;

public class UserAlreadyExistsException extends RuntimeException {
  public UserAlreadyExistsException(String message) {
    super(message);
  }
}
