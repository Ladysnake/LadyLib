package ladylib.networking.http;

/**
 * This exception is raised when a HTTP request has failed
 */
public class HTTPRequestException extends RuntimeException {
    public HTTPRequestException(String message, Exception cause) {
        super(message, cause);
    }

    public HTTPRequestException(String message) {
        super(message);
    }
}
