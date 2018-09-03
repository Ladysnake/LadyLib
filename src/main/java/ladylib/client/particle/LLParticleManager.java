package ladylib.client.particle;

import ladylib.LadyLib;
import ladylib.config.LLConfig;
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

/**
 * A particle manager that handles custom particles updates and rendering. <br/>
 * This particle manager makes use of a hard limit on the number of particles drawn to alleviate the load on the client framerate.
 * <p>
 * Particles are classed by {@link IParticleDrawingStage}, with each drawing stage describing a specific rendering context.
 * Particles sharing the same drawing stage will be drawn as part of the same batch.
 * </p>
 *
 * <p>
 * This class was originally adapted from embers' source code under GNU Lesser General Public License 2.1
 * https://github.com/RootsTeam/Embers/blob/master/src/main/java/teamroots/embers/particle/ParticleRenderer.java
 * </p>
 *
 * @author Pyrofab
 * @author Elucent

 * @see ISpecialParticle
 * @see IParticleDrawingStage
 */
@SideOnly(Side.CLIENT)
public class LLParticleManager {

    /**Stores each currently active particle in a queue, depending on its drawing stage*/
    private final Map<IParticleDrawingStage, Queue<ISpecialParticle>> particles = new HashMap<>();
    private final Set<ResourceLocation> particleTextures = new HashSet<>();

    public static LLParticleManager getInstance() {
        return LadyLib.getParticleManager();
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
        if (event.phase == TickEvent.Phase.START) {
            updateParticles();
        }
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
                if (++count > 3 * LLConfig.maxParticles) {
                    break;
                }
                ISpecialParticle particle = iterator.next();
                particle.updateParticle();
                if (particle.isDead()) {
                    iterator.remove();
                }
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
            updateParticleFields(partialTicks, player);

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
                    // check that we don't render past the limit
                    if (++particleCount > LLConfig.maxParticles) {
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

    private static void updateParticleFields(float partialTicks, EntityPlayer player) {
        Particle.interpPosX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        Particle.interpPosY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        Particle.interpPosZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        Particle.cameraViewDir = player.getLook(partialTicks);
    }

    public void addParticle(ISpecialParticle p) {
        // If we can't even tick them, don't add them
        if (particles.values().stream().mapToInt(Collection::size).sum() < LLConfig.maxParticles * 3) {
            particles.computeIfAbsent(p.getDrawStage(), i -> new ArrayDeque<>()).add(p);
        }
    }

}