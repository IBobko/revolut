package revolut.exception;

public class TheSamePayerAndPayeeException extends IllegalArgumentException
{
    public TheSamePayerAndPayeeException(String message) {
        super(message);
    }
}
