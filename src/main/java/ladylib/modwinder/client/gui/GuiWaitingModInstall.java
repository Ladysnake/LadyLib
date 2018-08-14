package ladylib.modwinder.client.gui;

import ladylib.modwinder.installer.InstallationState;
import ladylib.modwinder.installer.ModEntry;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class GuiWaitingModInstall extends GuiScreen {

    private GuiScreen previousScreen;

    public GuiWaitingModInstall(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    @Override
    public void initGui() {
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 120 + 12, I18n.format("gui.cancel")));
    }

    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawBackground(0);
        this.drawCenteredString(this.fontRenderer, I18n.format("modwinder.waiting"), this.width / 2, this.height / 2 - 50, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        this.mc.displayGuiScreen(previousScreen);
    }

    @Override
    public void updateScreen() {
        // check if there is still a mod being installed
        if (ModEntry.getLadysnakeMods().stream()
                        .map(ModEntry::getInstallationState)
                        .map(InstallationState::getStatus)
                        .noneMatch(InstallationState.Status.INSTALLING::equals)) {
            FMLCommonHandler.instance().exitJava(0, false);
        }
    }
}
