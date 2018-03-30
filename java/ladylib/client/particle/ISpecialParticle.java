package ladylib.client.particle;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nonnull;

/**
 * A {@link net.minecraft.client.particle.Particle particle} with special properties
 * @see SpecialParticle
 * @see ParticleManager
 * @see IParticleDrawingStage
 */
public interface ISpecialParticle {

    @Nonnull
    IParticleDrawingStage getDrawStage();

    void renderParticle(@Nonnull BufferBuilder buffer, EntityPlayer player, float partialTicks, float x, float xz, float z, float yz, float xy);

    /**
     * Called every tick to update the particle state. <br/>
     * This needs to be called something else than {@code onUpdate} otherwise it will crash on obfuscated clients
     */
    void updateParticle();

    /**
     * If true, this particle will get removed from the particle manager and will not be updated nor rendered anymore
     * @return whether the particle should be removed
     */
    boolean isDead();
}
