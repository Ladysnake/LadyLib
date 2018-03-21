package ladylib.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.function.Supplier;

/**
 * This class has been adapted from embers' source code under GNU Lesser General Public License 2.1
 * https://github.com/RootsTeam/Embers/blob/master/src/main/java/teamroots/embers/particle/ParticleRenderer.java
 *
 * @author Elucent
 */
@SideOnly(Side.CLIENT)
public class ParticleManager {

    /**Stores each currently active particle in a queue, depending on its drawing stage*/
    private final Map<IParticleDrawingStage, Queue<ISpecialParticle>> particles = new HashMap<>();
    private final Set<ResourceLocation> particleTextures = new HashSet<>();
    private Supplier<Integer> maxParticles = () -> 300;

    public void setMaxParticlesConfig(Supplier<Integer> maxParticles) {
        this.maxParticles = maxParticles;
    }

    /**
     * Convenience method that can be called at preinit to register particle textures to be added in the atlas. <br/>
     * Using this method to register particle textures is not mandatory, mods can use their own event subscriber.
     *
     * @param location a resource location indicating where to find the texture in assets
     */
    public void registerParticleTexture(ResourceLocation location) {
        particleTextures.add(location);
    }

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        particleTextures.forEach(event.getMap()::registerSprite);
    }

    @SubscribeEvent
    public void onGameTick(TickEvent.ClientTickEvent event) {
        // TODO check if particles look weird when updated only once per tick
        if (event.phase == TickEvent.Phase.START)
            updateParticles();
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        renderParticles(event.getPartialTicks());
    }

    private void updateParticles() {
        int count = 0;
        // go through every particle indiscriminately
        for (Queue<ISpecialParticle> particleQueue : particles.values()) {
            for (Iterator<ISpecialParticle> iterator = particleQueue.iterator(); iterator.hasNext(); ) {
                // particles cost a lot less to update than to render so we can update more of them
                if (++count > 3 * maxParticles.get()) break;
                ISpecialParticle particle = iterator.next();
                particle.onUpdate();
                if (particle.isDead())
                    iterator.remove();
            }
        }
    }

    private void renderParticles(float partialTicks) {
        float x = ActiveRenderInfo.getRotationX();
        float z = ActiveRenderInfo.getRotationZ();
        float yz = ActiveRenderInfo.getRotationYZ();
        float xy = ActiveRenderInfo.getRotationXY();
        float xz = ActiveRenderInfo.getRotationXZ();
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player != null) {
            Particle.interpPosX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
            Particle.interpPosY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
            Particle.interpPosZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
            Particle.cameraViewDir = player.getLook(partialTicks);

            GlStateManager.pushMatrix();

            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569F);
            GlStateManager.disableCull();

            GlStateManager.depthMask(false);

            Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buffer = tess.getBuffer();

            int particleCount = 0;
            drawParticles:
            // render every particle grouped by particle stage
            for (Map.Entry<IParticleDrawingStage, Queue<ISpecialParticle>> particleStage : particles.entrySet()) {
                particleStage.getKey().prepareRender();
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
                // add every particle in the drawing stage to the buffer
                for (ISpecialParticle particle : particleStage.getValue()) {
                    if (++particleCount > maxParticles.get()) {
                        // upload whatever is currently in the buffer
                        tess.draw();
                        particleStage.getKey().clear();
                        break drawParticles;
                    }
                    particle.renderParticle(buffer, player, partialTicks, x, xz, z, yz, xy);
                }
                // apply custom stage effects and upload
                tess.draw();
                particleStage.getKey().clear();
            }

            GlStateManager.enableCull();
            GlStateManager.depthMask(true);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableBlend();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.popMatrix();
        }
    }

    public void addParticle(ISpecialParticle p) {
        // If we can't even tick them, don't add them
        if (particles.values().stream().mapToInt(Collection::size).sum() < maxParticles.get() * 3)
            particles.computeIfAbsent(p.getDrawStage(), i -> new ArrayDeque<>()).add(p);
    }

}