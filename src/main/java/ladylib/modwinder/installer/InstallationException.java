package ladylib.modwinder.installer;

public class InstallationException extends RuntimeException {
    public InstallationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InstallationException(String message) {
        super(message);
    }
}
