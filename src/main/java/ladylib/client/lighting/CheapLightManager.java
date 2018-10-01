package ladylib.client.lighting;

import com.google.common.annotations.Beta;
import ladylib.LadyLib;
import ladylib.client.particle.ISpecialParticle;
import ladylib.client.shader.ShaderRegistryEvent;
import ladylib.client.shader.ShaderUtil;
import ladylib.compat.EnhancedBusSubscriber;
import ladylib.misc.PublicApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.util.vector.Matrix4f;

import javax.annotation.Nonnull;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static net.minecraft.client.renderer.GlStateManager.*;
import static net.minecraft.client.renderer.OpenGlHelper.GL_TEXTURE2;
import static net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit;
import static org.lwjgl.opengl.GL11.*;

/**
 * A light manager that handles custom dynamic lights updates and rendering.
 * <p>
 * The lighting implementation consists of a simple depth test, allowing a minimal impact on the framerate.
 * As a drawback, the quality of larger lights can be lower than expected. <br>
 * The number of lights is also hard capped at a 100, due to limitations of shader array uniforms.
 * <p>
 * Light sources behave mostly like {@link ISpecialParticle particles} in that they are updated each tick and
 * will only be removed once they are marked as dead.
 *
 * @see CheapLight
 */
@Beta
@EnhancedBusSubscriber(side = Side.CLIENT)
public class CheapLightManager {

    private static final ResourceLocation LIGHT_TEXTURE = new ResourceLocation(LadyLib.MOD_ID, "textures/light.png");
    private static final ResourceLocation LIGHT_SHADER = new ResourceLocation(LadyLib.MOD_ID,"cheap_light");

    public static final int MAX_LIGHTS = 100;
    public static final CheapLightManager INSTANCE = new CheapLightManager();

    private final List<CheapLight> cheapLights = new ArrayList<>();

    /**
     * Adds a light source to this manager. It will then be ticked and rendered until it's marked {@link CheapLight#isExpired() expired}.
     * @param light the light source to be added
     */
    @PublicApi
    public void addLight(CheapLight light) {
        cheapLights.add(light);
    }

    /**
     * Gets a stream of every light currently being managed
     */
    @PublicApi
    public Stream<CheapLight> getLights() {
        return cheapLights.stream();
    }

    @SubscribeEvent
    void onShaderRegistry(ShaderRegistryEvent event) {
        event.registerShader(LIGHT_SHADER);
    }

    @SubscribeEvent
    void onTickClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            for (Iterator<CheapLight> iterator = cheapLights.iterator(); iterator.hasNext(); ) {
                CheapLight cheapLight = iterator.next();
                cheapLight.tick();
                if (cheapLight.isExpired()) {
                    iterator.remove();
                }
            }
            if (cheapLights.size() > MAX_LIGHTS) {
                // sort lights (closest first) so that
                Entity camera = Minecraft.getMinecraft().getRenderViewEntity();
                if (camera != null) {
                    cheapLights.sort(Comparator.comparing(l -> camera.getDistanceSq(l.getPosX(), l.getPosY(), l.getPosZ())));
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    void onRenderWorldLast(RenderWorldLastEvent event) {
        if (cheapLights.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        Entity camera = mc.getRenderViewEntity();
        if (camera == null) return;

        // Check that the depth texture exists
        int depthTexture = FramebufferReplacement.getMainDepthTexture();
        if (depthTexture < 0) return;

        float partialTicks = event.getPartialTicks();

        // Pass the depth texture as a secondary texture input
        setActiveTexture(GL_TEXTURE2);
        bindTexture(depthTexture);
        setActiveTexture(defaultTexUnit);
        mc.getTextureManager().bindTexture(LIGHT_TEXTURE);
        // Ignore depth when rendering lights
        disableDepth();

        pushMatrix();
        color(1.0F, 1.0F, 1.0F, 1.0F);
        scale(1F, 1F, 1F);
        enableBlend();
        disableAlpha();     // enable transparency
        blendFunc(GL_SRC_ALPHA, GL_ONE); // Modulate blending

        ShaderUtil.useShader(LIGHT_SHADER);
        ShaderUtil.setUniform("InverseTransformMatrix", computeInverseTransformMatrix());

        // Setup overlay rendering
        ScaledResolution scaledRes = new ScaledResolution(mc);
        matrixMode(GL_PROJECTION);
        loadIdentity();
        ortho(0.0D, scaledRes.getScaledWidth_double(), scaledRes.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
        matrixMode(GL_MODELVIEW);
        loadIdentity();
        translate(0.0F, 0.0F, -2000.0F);

        ShaderUtil.setUniform("DepthSampler", 2);
        ShaderUtil.setUniform("PlayerPosition", (float) camera.posX, (float) camera.posY, (float) camera.posZ);
        ShaderUtil.setUniform("ViewPort", 0, 0, mc.displayWidth, mc.displayHeight);
        ShaderUtil.setUniform("ViewMatrix", ShaderUtil.getModelViewMatrix());
        ShaderUtil.setUniform("ProjectionMatrix", ShaderUtil.getProjectionMatrix());
        // Model Matrix remains identity because we are drawing directly in eye space

        int size = Math.min(MAX_LIGHTS, cheapLights.size());
        ShaderUtil.setUniform("u_lightCount", size);

        for (int i = 0; i < size; i++) {
            CheapLight light = cheapLights.get(i);
            double x = (float)(light.getLastPosX() + (light.getPosX() - light.getLastPosX()) * partialTicks - Particle.interpPosX);
            double y = (float)(light.getLastPosY() + (light.getPosY() - light.getLastPosY()) * partialTicks - Particle.interpPosY);
            double z = (float)(light.getLastPosZ() + (light.getPosZ() - light.getLastPosZ()) * partialTicks - Particle.interpPosZ);
            ShaderUtil.setUniform("u_light[" + i + "].position", (float)x, (float)y, (float) z);
            ShaderUtil.setUniform("u_light[" + i + "].color", light.getColor().getRGBComponents(null));
            ShaderUtil.setUniform("u_light[" + i + "].radius", light.getRadius());
        }
        // Draw quad over the screen
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(0.0D, (double)scaledRes.getScaledHeight(), -90.0D).tex(0.0D, 1.0D).endVertex();
        bufferbuilder.pos((double)scaledRes.getScaledWidth(), (double)scaledRes.getScaledHeight(), -90.0D).tex(1.0D, 1.0D).endVertex();
        bufferbuilder.pos((double)scaledRes.getScaledWidth(), 0.0D, -90.0D).tex(1.0D, 0.0D).endVertex();
        bufferbuilder.pos(0.0D, 0.0D, -90.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();
        ShaderUtil.revert();

        // restore old values
        popMatrix();
        disableBlend();
        enableAlpha();
        blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // restore blending
        enableDepth();
    }

    private Matrix4f projectionMatrix = new Matrix4f();
    private Matrix4f viewMatrix = new Matrix4f();

    /**
     * @return the matrix allowing computation of eye space coordinates from window space
     */
    @Nonnull
    private FloatBuffer computeInverseTransformMatrix() {
        projectionMatrix.load(ShaderUtil.getProjectionMatrix());

        viewMatrix.load(ShaderUtil.getModelViewMatrix());

        Matrix4f projectionViewMatrix      = Matrix4f.mul(projectionMatrix, viewMatrix, null);
        // reuse the projection matrix instead of creating a new one
        Matrix4f inverseTransformMatrix    = Matrix4f.invert(projectionViewMatrix, projectionMatrix);

        FloatBuffer buf = ShaderUtil.getTempBuffer();
        inverseTransformMatrix.store(buf);
        buf.rewind();
        return buf;
    }
}
