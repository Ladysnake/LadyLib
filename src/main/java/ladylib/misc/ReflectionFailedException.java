package ladylib.misc;

/**
 * An unchecked exception thrown when some reflection-related operation fails
 */
public class ReflectionFailedException extends RuntimeException {
    public ReflectionFailedException(String message) {
        super(message);
    }

    public ReflectionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
