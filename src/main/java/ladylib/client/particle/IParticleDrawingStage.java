package ladylib.client.particle;

/**
 * Defines a drawing stage for the {@link LLParticleManager}.
 * <p>
 * Particles sharing a drawing stage will be rendered in the same batch.
 * </p>
 *
 * @see ISpecialParticle
 * @see LLParticleManager
 * @see DrawingStages
 */
public interface IParticleDrawingStage {

    /**
     * This method is called right before particles of this stage are uploaded. <br/>
     * Use to set specific gl properties such as blend function or custom shaders.
     */
    void prepareRender();

    /**
     * This method is called after particles of this stage have been drawn.
     * <p>
     * After this method has been called, all OpenGL properties set in {@link #prepareRender()} must have been reset
     * to their state as it was before render.
     * </p>
     */
    void clear();
}
