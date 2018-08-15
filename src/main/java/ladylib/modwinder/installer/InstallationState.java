package ladylib.modwinder.installer;

import ladylib.modwinder.data.ModEntry;

public class InstallationState {
    public static final InstallationState NAUGHT = new InstallationState(Status.NONE, "");

    public enum Status {
        /**No attempt has been made to change the mod's installation state*/
        NONE(false, 0, true) {
            @Override
            public boolean canInstall(ModEntry selected) {
                return !selected.isInstalled() || selected.isOutdated();
            }
        },
        /**There was an error trying to change the mod's installation state*/
        FAILED(true, 3, true),
        /**The latest version of the mod is being installed*/
        INSTALLING(true, 6, false),
        /**The latest version of the mod has been installed during this session*/
        INSTALLED(true, 9, false),
        /**The mod has been uninstalled during this session*/
        UNINSTALLED(true, 12, true);

        private final boolean display;
        private final int sheetOffset;
        private final boolean canInstall;

        Status(boolean display, int sheetOffset, boolean canInstall) {
            this.display = display;
            this.sheetOffset = sheetOffset;
            this.canInstall = canInstall;
        }

        public boolean shouldDisplay() {
            return display;
        }

        public float getSheetOffset() {
            return sheetOffset;
        }

        public boolean canInstall(ModEntry selected) {
            return canInstall;
        }
    }

    private final Status status;
    private final String[] message;

    public InstallationState(Status status, String... message) {
        this.status = status;
        this.message = message;
    }

    public Status getStatus() {
        return status;
    }

    public String[] getMessage() {
        return message;
    }
}
