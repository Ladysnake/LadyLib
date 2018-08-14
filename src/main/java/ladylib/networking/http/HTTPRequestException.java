package ladylib.networking.http;

public class HTTPRequestException extends RuntimeException {
    public HTTPRequestException(String message, Exception cause) {
        super(message, cause);
    }

    public HTTPRequestException(String message) {
        super(message);
    }
}
