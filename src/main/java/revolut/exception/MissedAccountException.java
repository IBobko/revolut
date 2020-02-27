package revolut.exception;

public class MissedAccountException extends RuntimeException {
    public MissedAccountException(String message) {
        super(message);
    }
}
