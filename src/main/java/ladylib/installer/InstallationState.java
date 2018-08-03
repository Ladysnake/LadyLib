package ladylib.installer;

public class InstallationState {
    public static final InstallationState NAUGHT = new InstallationState(Status.NONE, "");

    public enum Status {
        NONE(false, 0f, 0f, 0f),
        INSTALLING(true, 0f, 0f, 1f),
        INSTALLED(true, 0f, 1f, 0f),
        FAILED(true, 1f, 0f, 0f);

        private final boolean display;
        private final float r, g, b;

        Status(boolean display, float red, float green, float blue) {
            this.display = display;
            this.r = red;
            this.g = green;
            this.b = blue;
        }

        public boolean shouldDisplay() {
            return display;
        }

        // TODO: 03/08/2018 replace the color offset with a custom sprite
        public float getRed() {
            return r;
        }

        public float getGreen() {
            return g;
        }

        public float getBlue() {
            return b;
        }
    }

    private final Status status;
    private String message;

    public InstallationState(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public Status getStatus() {
        return status;
    }

    public synchronized void setMessage(String message) {
        this.message = message;
    }

    public synchronized String getMessage() {
        return message;
    }
}
