package service;

public class ManagerValidateException extends RuntimeException {
    public ManagerValidateException(String message) {
        super(message);
    }

    public ManagerValidateException(String message, Throwable cause) {
        super(message, cause);
    }
}
