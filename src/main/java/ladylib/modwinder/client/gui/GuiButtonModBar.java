package ladylib.modwinder.client.gui;

import ladylib.modwinder.ModWinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class GuiButtonModBar extends GuiButton {
    public static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation(ModWinder.MOD_ID, "textures/gui/modbar_widget.png");

    public GuiButtonModBar(int buttonID, int xPos, int yPos) {
        super(buttonID, xPos, yPos, 20, 20, "");
    }

    /**
     * Draws this button to the screen.
     */
    @Override
    public void drawButton(@Nonnull Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (this.visible) {
            mc.getTextureManager().bindTexture(BUTTON_TEXTURE);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            boolean flag = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
            int i = 0;

            if (flag) {
                i += this.height;
            }

            Gui.drawModalRectWithCustomSizedTexture(this.x, this.y, 0, i, this.width, this.height, 20, 40);
        }
    }
}
