package ladylib.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class SpecialParticle extends Particle implements ISpecialParticle {
    @Nonnull
    protected IParticleDrawingStage drawStage = DrawingStages.NORMAL;

    protected SpecialParticle(World worldIn, double posXIn, double posYIn, double posZIn) {
        super(worldIn, posXIn, posYIn, posZIn);
    }

    @Nonnull
    @Override
    public IParticleDrawingStage getDrawStage() {
        return drawStage;
    }

    public void setDrawStage(@Nonnull IParticleDrawingStage drawStage) {
        this.drawStage = drawStage;
    }

    @Override
    public void renderParticle(@Nonnull BufferBuilder buffer, EntityPlayer player, float partialTicks, float x, float xz, float z, float yz, float xy) {
        super.renderParticle(buffer, player, partialTicks, x, xz, z, yz, xy);
    }

    @Override
    public void updateParticle() {
        onUpdate();
    }

    @Override
    public boolean isDead() {
        return this.isExpired;
    }

    public void setTexture(ResourceLocation texture) {
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(texture.toString());
        this.setParticleTexture(sprite);
    }
}
