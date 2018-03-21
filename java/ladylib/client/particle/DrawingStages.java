package ladylib.client.particle;

import net.minecraft.client.renderer.GlStateManager;

public enum DrawingStages implements IParticleDrawingStage {
    NORMAL(GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, false),
    ADDITIVE(GlStateManager.DestFactor.ONE, false),
    GHOST(GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, true),
    GHOST_ADDITIVE(GlStateManager.DestFactor.ONE, true);
    static DrawingStages[] VALUES = values();

    GlStateManager.DestFactor destFactor;
    boolean renderThroughBlocks;

    DrawingStages(GlStateManager.DestFactor destFactor, boolean renderThroughBlocks) {
        this.destFactor = destFactor;
        this.renderThroughBlocks = renderThroughBlocks;
    }

    @Override
    public void prepareRender() {
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, destFactor);
        if (this.renderThroughBlocks)
            GlStateManager.disableDepth();
    }

    @Override
    public void clear() {
        if (this.renderThroughBlocks)
            GlStateManager.enableDepth();
    }

}
