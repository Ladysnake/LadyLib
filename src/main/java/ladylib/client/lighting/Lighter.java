package ladylib.client.lighting;

import com.google.common.annotations.Beta;
import ladylib.LadyLib;
import ladylib.client.shader.ShaderRegistryEvent;
import ladylib.client.shader.ShaderUtil;
import ladylib.compat.EnhancedBusSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.client.renderer.GlStateManager.*;
import static net.minecraft.client.renderer.OpenGlHelper.GL_TEXTURE2;
import static net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit;
import static org.lwjgl.opengl.GL11.*;

@Beta
@EnhancedBusSubscriber(side = Side.CLIENT)
public class Lighter {

    private static final ResourceLocation LIGHT_TEXTURE = new ResourceLocation(LadyLib.MOD_ID, "textures/light.png");
    private static final ResourceLocation LIGHT_SHADER = new ResourceLocation(LadyLib.MOD_ID,"cheap_light");

    private final List<CheapLight> cheapLights = new ArrayList<>();

    public Lighter() {

    }

    @SubscribeEvent
    public void onShaderRegistry(ShaderRegistryEvent event) {
        event.registerShader(LIGHT_SHADER);
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        if (!LadyLib.isDevEnv()) return;
        if (event.getEntityItem().getItem().getItem() == Items.GLOWSTONE_DUST || event.getEntityItem().getItem().getItem() == Items.COAL) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (event.getEntityItem().getItem().getItem() == Items.GLOWSTONE_DUST) {
                    cheapLights.add(new CheapLight(event.getPlayer().getPositionVector(), 0.8f, new Color(0.5f, 0.3f, 1.0f, 1.0f)));
                } else if (event.getEntityItem().getItem().getItem() == Items.COAL) {
                    cheapLights.clear();
                }
            });
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (cheapLights.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();

        int depthTexture = FramebufferReplacement.getMainDepthTexture();

        setActiveTexture(GL_TEXTURE2);
        bindTexture(depthTexture);
        setActiveTexture(defaultTexUnit);
        mc.getTextureManager().bindTexture(LIGHT_TEXTURE);
        // Don't write lights to depth
        depthMask(false);
        disableDepth();

        pushMatrix();
        color(1.0F, 1.0F, 1.0F, 1.0F);
        scale(1F, 1F, 1F);
        enableBlend();
        disableAlpha();
        blendFunc(GL_SRC_ALPHA, GL_ONE); // Modulate blending


        ShaderUtil.useShader(LIGHT_SHADER);
        ShaderUtil.setUniformValue("InverseTransformMatrix", loc -> {
            Matrix4f projectionMatrix = new Matrix4f();
            projectionMatrix.load(ShaderUtil.getProjectionMatrix());

            Matrix4f viewMatrix = new Matrix4f();
            viewMatrix.load(ShaderUtil.getModelViewMatrix());

            Matrix4f projectionViewMatrix      = Matrix4f.mul(projectionMatrix, viewMatrix, null);
            // reuse the projection matrix instead of creating a new one
            Matrix4f inverseTransformMatrix    = Matrix4f.invert(projectionViewMatrix, projectionMatrix);

            FloatBuffer buf = ShaderUtil.getTempBuffer();
            inverseTransformMatrix.store(buf);
            buf.rewind();

            GL20.glUniformMatrix4(loc, false, buf);
        });

        // Setup overlay rendering
        ScaledResolution scaledRes = new ScaledResolution(mc);
        matrixMode(GL_PROJECTION);
        loadIdentity();
        ortho(0.0D, scaledRes.getScaledWidth_double(), scaledRes.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
        matrixMode(GL_MODELVIEW);
        loadIdentity();
        translate(0.0F, 0.0F, -2000.0F);

        ShaderUtil.setUniform("DepthSampler", 2);
        Entity camera = mc.getRenderViewEntity();
        if (camera != null) {
            ShaderUtil.setUniform("PlayerPosition", (float) camera.posX, (float) camera.posY, (float) camera.posZ);
        }
        ShaderUtil.setUniform("ViewPort", 0, 0, mc.displayWidth, mc.displayHeight);
        ShaderUtil.setUniformValue("ViewMatrix", loc -> GL20.glUniformMatrix4(loc, false, ShaderUtil.getModelViewMatrix()));
        ShaderUtil.setUniformValue("ProjectionMatrix", loc -> GL20.glUniformMatrix4(loc, false, ShaderUtil.getProjectionMatrix()));
        loadIdentity();
        ShaderUtil.setUniformValue("ModelMatrix", loc -> GL20.glUniformMatrix4(loc, false, ShaderUtil.getModelViewMatrix()));

        int size = Math.min(100, cheapLights.size());
        ShaderUtil.setUniform("u_lightCount", size);

        for (int i = 0; i < size; i++) {
            CheapLight light = cheapLights.get(i);
            Vec3d pos = light.getPos();
            double x = pos.x - TileEntityRendererDispatcher.staticPlayerX;
            double y = pos.y - TileEntityRendererDispatcher.staticPlayerY;
            double z = pos.z - TileEntityRendererDispatcher.staticPlayerZ;
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
        depthMask(true);
    }
}
