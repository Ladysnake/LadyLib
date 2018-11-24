package ladylib.client.lighting;

import com.google.common.annotations.Beta;
import ladylib.LadyLib;
import ladylib.client.particle.ISpecialParticle;
import ladylib.client.shader.PostProcessShader;
import ladylib.client.shader.ShaderException;
import ladylib.client.shader.ShaderUtil;
import ladylib.compat.EnhancedBusSubscriber;
import ladylib.misc.PublicApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.util.vector.Matrix4f;

import javax.annotation.Nonnull;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.stream.Stream;

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
public class CheapLightManager {

    private static final ResourceLocation LIGHT_SHADER = new ResourceLocation(LadyLib.MOD_ID,"shaders/post/cheap_light.json");

    @EnhancedBusSubscriber(value = LadyLib.MOD_ID, side = Side.CLIENT)
    public static final CheapLightManager INSTANCE = new CheapLightManager();
    public static final int MAX_LIGHTS = 100;

    private final List<CheapLight> cheapLights = new ArrayList<>();

    private PostProcessShader shader = PostProcessShader.loadShader(LIGHT_SHADER, this::initShader);

    // fancy shader stuff
    private Matrix4f projectionMatrix = new Matrix4f();
    private Matrix4f viewMatrix = new Matrix4f();
    private Frustum frustum = new Frustum();

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
    public void onTickClientTick(TickEvent.ClientTickEvent event) {
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

    /**
     * Applies the darkness shader after the world has been fully rendered
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (this.shader != null && !this.shader.isErrored()) {
            shader.setupDynamicUniforms(this::setupFancyUniforms);
            shader.render(event.getPartialTicks());
        }
    }

    private void setupFancyUniforms() {
        Minecraft mc = Minecraft.getMinecraft();
        Entity camera = Objects.requireNonNull(mc.getRenderViewEntity());
        frustum.setPosition(camera.posX, camera.posY, camera.posZ);
        ShaderUtil.setUniform("InverseTransformMatrix", computeInverseTransformMatrix());
        int lightCount = 0;
        float[] colorComponents = new float[4];
        for (CheapLight light : this.cheapLights) {
            double posX = light.getPosX();
            double posY = light.getPosY();
            double posZ = light.getPosZ();
            float lightValue = light.getRadius();
            if (frustum.isBoxInFrustum(posX - lightValue, posY - lightValue, posZ - lightValue, posX + lightValue, posY + lightValue, posZ + lightValue)) {
                float x = (float) (posX - Particle.interpPosX);
                float y = (float) (posY - Particle.interpPosY);
                float z = (float) (posZ - Particle.interpPosZ);
                ShaderUtil.setUniform("Lights[" + lightCount + "].position", x, y, z);
                ShaderUtil.setUniform("Lights[" + lightCount + "].radius", lightValue);
                ShaderUtil.setUniform("Lights[" + lightCount + "].color", light.getColor().getColorComponents(colorComponents));
                lightCount++;
            }
        }
        ShaderUtil.setUniform("LightCount", lightCount);
    }

    /**
     * @return the matrix allowing computation of eye space coordinates from window space
     */
    @Nonnull
    private FloatBuffer computeInverseTransformMatrix() {
        projectionMatrix.load(ShaderUtil.getProjectionMatrix());

        viewMatrix.load(ShaderUtil.getModelViewMatrix());

        Matrix4f projectionViewMatrix = Matrix4f.mul(projectionMatrix, viewMatrix, null);
        // reuse the projection matrix instead of creating a new one
        Matrix4f inverseTransformMatrix = Matrix4f.invert(projectionViewMatrix, projectionMatrix);

        FloatBuffer buf = ShaderUtil.getTempBuffer();
        inverseTransformMatrix.store(buf);
        buf.rewind();
        return buf;
    }

    private void initShader(PostProcessShader s) {
        Minecraft mc = Minecraft.getMinecraft();
        int depth = ShaderUtil.getDepthTexture();
        if (depth < 0) {
            MinecraftForge.EVENT_BUS.unregister(this);
            s.dispose(true);
            throw new ShaderException("Depth texture missing, disabling dynamic lighting");
        }
        s.setSamplerUniform("DepthSampler", ShaderUtil.getDepthTexture());
        s.setUniformValue("ViewPort", 0, 0, mc.displayWidth, mc.displayHeight);
    }
}
