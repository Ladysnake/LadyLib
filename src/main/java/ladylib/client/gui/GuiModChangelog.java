package ladylib.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.client.GuiScrollingList;
import net.minecraftforge.fml.common.versioning.ComparableVersion;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GuiModChangelog extends GuiScreen {

    private static final int DONE_BUTTON_ID = 6;
    private GuiScreen previous;
    private Changelog changelogScreen;
    private Map<ComparableVersion, String> changelog;

    public GuiModChangelog(GuiScreen previous, Map<ComparableVersion, String> changelog) {
        this.previous = previous;
        this.changelog = changelog;
    }

    @Override
    public void initGui() {
        // "Done" button
        this.buttonList.add(new GuiButton(DONE_BUTTON_ID, ((this.width) / 2) - 100, this.height - 38, I18n.format("gui.done")));
        this.changelogScreen = new Changelog(this.width - 30, changelog);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        this.changelogScreen.drawScreen(mouseX, mouseY, partialTicks);
        int left = ((this.width - 38) / 2) + 30;
        this.drawCenteredString(this.fontRenderer, "Changelog", left, 16, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void handleMouseInput() throws IOException {
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        super.handleMouseInput();
        this.changelogScreen.handleMouseInput(mouseX, mouseY);
    }

    /**
     * Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
     */
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.enabled) {
            switch (button.id) {
                case DONE_BUTTON_ID: {
                    this.mc.displayGuiScreen(this.previous);
                    return;
                }
            }
        }
        super.actionPerformed(button);
    }

    private class Changelog extends GuiScrollingList {
        private List<ITextComponent> lines;

        Changelog(int width, Map<ComparableVersion, String> lines) {
            super(GuiModChangelog.this.mc,
                    width,
                    GuiModChangelog.this.height,
                    32, GuiModChangelog.this.height - 88 + 4,
                    20, 60,
                    GuiModChangelog.this.width,
                    GuiModChangelog.this.height);
            this.lines = resizeContent(lines.entrySet().stream()
                    .sorted((v1, v2) -> v2.getKey().compareTo(v1.getKey()))
                    .map(e -> e.getKey() + "\n" + e.getValue() + "\n \n ")
                    .flatMap(s -> Arrays.stream(s.split("\n")))
                    .collect(Collectors.toList())
            );

            this.setHeaderInfo(true, getHeaderHeight());
        }

        @Override
        protected int getSize() {
            return 0;
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick) { }

        @Override
        protected boolean isSelected(int index) {
            return false;
        }

        @Override
        protected void drawBackground() { }

        @Override
        protected void drawSlot(int slotIdx, int entryRight, int slotTop, int slotBuffer, Tessellator tess) { }

        private List<ITextComponent> resizeContent(List<String> lines) {
            List<ITextComponent> ret = new ArrayList<>();
            for (String line : lines) {
                if (line == null) {
                    ret.add(null);
                    continue;
                }

                ITextComponent chat = ForgeHooks.newChatWithLinks(line, false);
                int maxTextLength = this.listWidth - 8;
                if (maxTextLength >= 0) {
                    ret.addAll(GuiUtilRenderComponents.splitText(chat, maxTextLength, GuiModChangelog.this.fontRenderer, false, true));
                }
            }
            return ret;
        }

        private int getHeaderHeight() {
            int height = 0;
            height += (lines.size() * 10);
            if (height < this.bottom - this.top - 8) height = this.bottom - this.top - 8;
            return height;
        }


        @Override
        protected void drawHeader(int entryRight, int relativeY, Tessellator tess) {
            int top = relativeY;

            for (ITextComponent line : lines) {
                if (line != null) {
                    GlStateManager.enableBlend();
                    GuiModChangelog.this.fontRenderer.drawStringWithShadow(line.getFormattedText(), this.left + 4, top, 0xFFFFFF);
                    GlStateManager.disableAlpha();
                    GlStateManager.disableBlend();
                }
                top += 10;
            }
        }

        @Override
        protected void clickHeader(int x, int y) {
            if (y <= 0)
                return;

            int lineIdx = y / 10;
            if (lineIdx >= lines.size())
                return;

            ITextComponent line = lines.get(lineIdx);
            if (line != null) {
                int k = -4;
                for (ITextComponent part : line) {
                    if (!(part instanceof TextComponentString))
                        continue;
                    k += GuiModChangelog.this.fontRenderer.getStringWidth(((TextComponentString) part).getText());
                    if (k >= x) {
                        GuiModChangelog.this.handleComponentClick(part);
                        break;
                    }
                }
            }
        }
    }
}
