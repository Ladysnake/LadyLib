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
     * This needs to be called something else than {@code onUpdate} otherwise it will crash on obfuscated clients
     */
    void updateParticle();

    boolean isDead();
}
