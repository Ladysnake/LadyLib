package ladylib.modwinder.client.gui;

import joptsimple.internal.Strings;
import ladylib.client.shader.ShaderUtil;
import ladylib.modwinder.ModWinder;
import ladylib.modwinder.installer.AddonInstaller;
import ladylib.modwinder.installer.InstallationState;
import ladylib.modwinder.installer.ModEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.client.GuiScrollingList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class GuiInstallerModList extends GuiScrollingList {

    public static final ResourceLocation INSTALLATION_STATUS_ICON = new ResourceLocation(ModWinder.MOD_ID, "textures/gui/installation_status_icons.png");
    public static final ResourceLocation VERSION_CHECK_ICONS = new ResourceLocation(ForgeVersion.MOD_ID, "textures/gui/version_check_icons.png");

    private final GuiModBar parent;
    private List<ModEntry> flattenedEntries;
    private int selected = -1;

    public GuiInstallerModList(GuiModBar parent, List<ModEntry> entries, int listWidth, int slotHeight) {
        super(Minecraft.getMinecraft(), listWidth, parent.height, 32, parent.height - 88 + 4, 10, slotHeight, parent.width, parent.height);
        this.parent = parent;
        flattenEntries(entries);
    }

    @Override
    protected int getSize() {
        return flattenedEntries.size();
    }

    @Override
    protected void elementClicked(int index, boolean doubleClick) {
        this.selected = index;
        this.parent.onModEntrySelected();
        if (doubleClick) {
            ModEntry selected = flattenedEntries.get(index);
            if (!selected.isInstalled() && selected.getInstallationState().getStatus() == InstallationState.Status.NONE) {
                AddonInstaller.installLatestFromCurseforge(selected);
            }
        }
    }

    @Override
    protected boolean isSelected(int index) {
        return index == selected;
    }

    @Nullable
    public ModEntry getSelected() {
        return selected >= 0 ? flattenedEntries.get(selected) : null;
    }

    @Override
    protected void drawBackground() {
        this.parent.drawDefaultBackground();
    }

    @Override
    protected void drawSlot(int slotIdx, int entryRight, int slotTop, int height, Tessellator tess) {
        ModEntry entry = flattenedEntries.get(slotIdx);
        String name = entry.getName();
        String version = entry.getLatestVersion();
        String author = entry.getAuthor();
        FontRenderer font = parent.getFontRenderer();

        int left = this.left + 3;

        if (entry.isDlc()) {
            left += height + 5;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(entry.getLogo());
        GlStateManager.enableAlpha();
        ShaderUtil.useShader(MWShaders.ROUNDISH);
        ShaderUtil.setUniform("saturation", entry.isInstalled() ? 1f : 0f);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        BufferBuilder bufferbuilder = tess.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos((double)(left + 0.0F  ), (double)(slotTop + (float)height), (double)1).tex(0, 1).endVertex();
        bufferbuilder.pos((double)(left + height), (double)(slotTop + (float)height), (double)1).tex(1, 1).endVertex();
        bufferbuilder.pos((double)(left + height), (double)(slotTop + 0.0F         ), (double)1).tex(1, 0).endVertex();
        bufferbuilder.pos((double)(left + 0.0F  ), (double)(slotTop + 0.0F         ), (double)1).tex(0, 0).endVertex();
        tess.draw();
        ShaderUtil.revert();

        left += height + 5;

        int color = 0x00E6D2;

        final int iconX = right - (height / 2 + 4);
        final int iconY = slotTop + (height / 2 - 4);
        final int iconWidth = 8;
        final int iconHeight = 8;

        if ((!entry.isInstalled() || entry.isOutdated()) && entry.getInstallationState().getStatus().shouldDisplay()) {
            InstallationState state = entry.getInstallationState();
            Minecraft.getMinecraft().getTextureManager().bindTexture(INSTALLATION_STATUS_ICON);
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.pushMatrix();
            int v = (((System.currentTimeMillis() / 800 & 1)) == 1) ? 8 : 0;
            Gui.drawModalRectWithCustomSizedTexture(iconX, iconY, state.getStatus().getSheetOffset() * iconWidth, v, iconWidth, iconHeight, 88, 16);
            GlStateManager.popMatrix();
            if (mouseX > iconX && mouseX < iconX + iconWidth && mouseY > iconY && mouseY < iconY + iconHeight) {
                parent.setHoveringText(state.getMessage());
            }
        } else if (!entry.isInstalled()) {
            color = 0x9E9E9E;
        } else if (entry.isOutdated()) {
            color = 0xFF6E00;
            Minecraft.getMinecraft().getTextureManager().bindTexture(VERSION_CHECK_ICONS);
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.pushMatrix();
            int v = (((System.currentTimeMillis() / 800 & 1)) == 1) ? 8 : 0;
            Gui.drawModalRectWithCustomSizedTexture(right - (height / 2 + 4), slotTop + (height / 2 - 4), 3 * iconWidth, v, iconWidth, iconHeight, 64, 16);
            GlStateManager.popMatrix();
            if (mouseX > iconX && mouseX < iconX + iconWidth && mouseY > iconY && mouseY < iconY + iconHeight) {
                parent.setHoveringText(
                        I18n.format("modwinder.status.outdated"),
                        I18n.format("modwinder.status.outdated.current_version", version)
                );
            }
        }

        // write the mod's name
        font.drawString(font.trimStringToWidth(name,    listWidth - 10), left, slotTop +  2, 0xFFFFFF);
        // write the author's name
        if (!Strings.isNullOrEmpty(author)) {
            font.drawString(font.trimStringToWidth("by " + author, listWidth - 10), left + font.getStringWidth(name) + 5, slotTop + 2, 0x808080);
        }
        // write the mod's latest version
        font.drawString(font.trimStringToWidth(version, listWidth - (5 + height)), left, slotTop + 12, color);
        // write the mod's number of DLCs
        if (entry.getDlcs().size() > 0) {
            font.drawString(font.trimStringToWidth(I18n.format("modwinder.dlc", entry.getDlcs().size()), listWidth - (5 + height)), left, slotTop + 22, 0xCCCCCC);
        }

        GlStateManager.disableAlpha();
    }

    @Override
    protected int getContentHeight() {
        return Math.max(this.bottom - this.top, super.getContentHeight());
    }

    public int getListWidth() {
        return listWidth;
    }

    public void reloadMods() {
        // regenerate the flattened list
        flattenEntries(
                ModEntry.getLadysnakeMods().stream()
                .filter(me -> me.getName().toLowerCase(Locale.ENGLISH).contains(parent.getSearchText()))    // select entries matching the search
                .sorted(this.parent.getSortType())                              // sort according to current selection
                .collect(Collectors.toList())
        );
    }

    private void flattenEntries(List<ModEntry> entries) {
        this.flattenedEntries = entries.stream().flatMap(me -> {
            List<ModEntry> ret = new ArrayList<>(me.getDlcs());
            ret.sort(this.parent.getSortType());
            ret.add(0, me);
            return ret.stream();
        }).collect(Collectors.toList());
    }

    public int getTop() {
        return top;
    }
}
