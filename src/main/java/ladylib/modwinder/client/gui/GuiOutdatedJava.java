package ladylib.modwinder.client.gui;

import ladylib.LadyLib;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.net.URI;
import java.net.URISyntaxException;

public class GuiOutdatedJava extends GuiScreen {

    private GuiScreen previousScreen;

    public GuiOutdatedJava(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    public void initGui() {
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, this.width / 2 - 155, this.height / 6 + 96,150, 20,  I18n.format("modwinder.java_warning.info")));
        this.buttonList.add(new GuiButton(1, this.width / 2 - 155 + 160, this.height / 6 + 96, 150, 20, I18n.format("gui.cancel")));
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawBackground(0);
        this.drawCenteredString(this.fontRenderer, I18n.format("modwinder.java_warning_1"), this.width / 2, this.height / 2 - 50, 0xFF5500);
        this.drawCenteredString(this.fontRenderer, I18n.format("modwinder.java_warning_2"), this.width / 2, this.height / 2 - 40, 0xFF5500);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0: {
                String url = "https://github.com/Ladysnake/Ladylib/blob/master/help/update_java.md";
                try {
                    this.clickedLinkURI = new URI(url);
                    GuiConfirmOpenLink link = new GuiConfirmOpenLink(this, url, 31102009, true);
                    link.disableSecurityWarning();
                    this.mc.displayGuiScreen(link);
                } catch (URISyntaxException e) {
                    LadyLib.LOGGER.error("Can't open url for {}", url, e);
                }
                break;
            }
            case 1: {
                this.mc.displayGuiScreen(previousScreen);
                break;
            }
        }
    }
}
