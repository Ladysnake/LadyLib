package ladylib.client.particle;

import net.minecraft.client.renderer.GlStateManager;

/**
 * Default drawing stages for standard particles
 */
public enum DrawingStages implements IParticleDrawingStage {
    /**
     * Normal particles render just like minecraft's default particles
     */
    NORMAL(GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, false),
    /**
     * Additive particles are rendered on their background using additive blending, making them flashier
     */
    ADDITIVE(GlStateManager.DestFactor.ONE, false),
    /**
     * Ghost particles are rendered through blocks
     */
    GHOST(GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, true),
    /**
     * Ghost additive particles are both rendered additively and through blocks
     */
    GHOST_ADDITIVE(GlStateManager.DestFactor.ONE, true);

    GlStateManager.DestFactor destFactor;
    boolean renderThroughBlocks;

    DrawingStages(GlStateManager.DestFactor destFactor, boolean renderThroughBlocks) {
        this.destFactor = destFactor;
        this.renderThroughBlocks = renderThroughBlocks;
    }

    @Override
    public void prepareRender() {
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, destFactor);
        if (this.renderThroughBlocks) {
            GlStateManager.disableDepth();
        }
    }

    @Override
    public void clear() {
        if (this.renderThroughBlocks) {
            GlStateManager.enableDepth();
        }
    }

}
