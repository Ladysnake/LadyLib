package ladylib.nbt;

public class MalformedNBTException extends RuntimeException {
    public MalformedNBTException(String message) {
        super(message);
    }

    public MalformedNBTException(Throwable cause) {
        super(cause);
    }

    public MalformedNBTException(String message, Throwable cause) {
        super(message, cause);
    }
}
