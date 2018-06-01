package ladylib.client.particle;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nonnull;

/**
 * A {@link net.minecraft.client.particle.Particle particle} with special properties
 *
 * @see SpecialParticle
 * @see LLParticleManager
 * @see IParticleDrawingStage
 */
public interface ISpecialParticle {

    /**
     * The drawing stage this particle should be rendered in
     * @see DrawingStages
     */
    @Nonnull
    IParticleDrawingStage getDrawStage();

    /**
     * Called every frame to render the particle on the screen
     *
     * @param buffer       the buffer where vertex information should be uploaded
     * @param player       the client player viewing this particle
     * @param partialTicks the client partial ticks
     * @param x            The X component of the player's yaw rotation
     * @param xz           The combined X and Z components of the player's pitch rotation
     * @param z            The Z component of the player's yaw rotation
     * @param yz           The Y component (scaled along the Z axis) of the player's pitch rotation
     * @param xy           The Y component (scaled along the X axis) of the entity's pitch rotation
     */
    void renderParticle(@Nonnull BufferBuilder buffer, EntityPlayer player, float partialTicks, float x, float xz, float z, float yz, float xy);

    /**
     * Called every tick to update the particle state.
     */
    // This needs to be called something else than onUpdate otherwise it will crash on obfuscated clients
    void updateParticle();

    /**
     * If true, this particle will get removed from the particle manager and will not be updated nor rendered anymore
     *
     * @return whether the particle should be removed
     */
    boolean isDead();
}
