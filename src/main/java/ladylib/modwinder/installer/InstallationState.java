package ladylib.modwinder.installer;

public class InstallationState {
    public static final InstallationState NAUGHT = new InstallationState(Status.NONE, "");

    public enum Status {
        NONE(false, 0),
        FAILED(true, 3),
        INSTALLING(true, 6),
        INSTALLED(true, 9);

        private final boolean display;
        private final int sheetOffset;

        Status(boolean display, int sheetOffset) {
            this.display = display;
            this.sheetOffset = sheetOffset;
        }

        public boolean shouldDisplay() {
            return display;
        }

        public int getSheetOffset() {
            return sheetOffset;
        }
    }

    private final Status status;
    private String[] message;

    public InstallationState(Status status, String... message) {
        this.status = status;
        this.message = message;
    }

    public Status getStatus() {
        return status;
    }

    public synchronized void setMessage(String... message) {
        this.message = message;
    }

    public synchronized String[] getMessage() {
        return message;
    }
}
