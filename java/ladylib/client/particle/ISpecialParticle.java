package ladylib.client.particle;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nonnull;

public interface ISpecialParticle {

    @Nonnull
    DrawingStages getDrawStage();

    void renderParticle(@Nonnull BufferBuilder buffer, EntityPlayer player, float partialTicks, float x, float xz, float z, float yz, float xy);

    void onUpdate();

    boolean isDead();
}
