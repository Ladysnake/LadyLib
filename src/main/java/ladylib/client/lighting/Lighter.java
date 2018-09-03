package ladylib.client.lighting;

import com.google.common.annotations.Beta;
import ladylib.LadyLib;
import ladylib.client.shader.ShaderRegistryEvent;
import ladylib.client.shader.ShaderUtil;
import ladylib.compat.EnhancedBusSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;

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

    private final List<Vec3d> lights = new ArrayList<>();

    public Lighter() {

    }

    @SubscribeEvent
    public void onShaderRegistry(ShaderRegistryEvent event) {
        event.registerShader(LIGHT_SHADER);
    }

/*
    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        if (!LadyLib.isDevEnv()) return;
        if (event.getEntityItem().getItem().getItem() == Items.GLOWSTONE_DUST || event.getEntityItem().getItem().getItem() == Items.COAL) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (event.getEntityItem().getItem().getItem() == Items.GLOWSTONE_DUST) {
                    lights.add(event.getPlayer().getPositionVector());
                } else if (event.getEntityItem().getItem().getItem() == Items.COAL) {
                    lights.clear();
                }
            });
            event.setCanceled(true);
        }
    }
*/

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (lights.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        // we need access to those options for rendering
        if (mc.getRenderManager().options == null) return;

        int depthTexture = FramebufferReplacement.getMainDepthTexture();

        setActiveTexture(GL_TEXTURE2);
        bindTexture(depthTexture);
        setActiveTexture(defaultTexUnit);
        mc.getTextureManager().bindTexture(LIGHT_TEXTURE);
        // Don't write lights to depth
        depthMask(false);
        disableDepth();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.scale(1F, 1F, 1F);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE); // Modulate blending
        ShaderUtil.useShader(LIGHT_SHADER);
        ShaderUtil.setUniform("DepthSampler", 2);
        Entity camera = mc.getRenderViewEntity();
        if (camera != null) {
            ShaderUtil.setUniform("PlayerPosition", (float) camera.posX, (float) camera.posY, (float) camera.posZ);
        }
        ShaderUtil.setUniform("ViewPort", 0, 0, mc.displayWidth, mc.displayHeight);
        ShaderUtil.setUniformValue("ViewMatrix", loc -> GL20.glUniformMatrix4(loc, false, ShaderUtil.getModelViewMatrix()));
        ShaderUtil.setUniformValue("ProjectionMatrix", loc -> GL20.glUniformMatrix4(loc, false, ShaderUtil.getProjectionMatrix()));

        lights.forEach(this::renderLight);
        ShaderUtil.revert();

        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // restore blending
        enableDepth();
        depthMask(true);
    }

    private void renderLight(Vec3d pos) {
        double x = pos.x - TileEntityRendererDispatcher.staticPlayerX;
        double y = pos.y - TileEntityRendererDispatcher.staticPlayerY;
        double z = pos.z - TileEntityRendererDispatcher.staticPlayerZ;
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();

        Matrix4f viewMatrix = new Matrix4f();
        viewMatrix.load(ShaderUtil.getModelViewMatrix());

        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.rotate(180.0F - renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((float) (renderManager.options.thirdPersonView == 2 ? -1 : 1) * -renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        ShaderUtil.setUniform("LightPosition", (float)x, (float)y, (float) z);
        ShaderUtil.setUniformValue("ModelMatrix", loc -> GL20.glUniformMatrix4(loc, false, ShaderUtil.getModelViewMatrix()));
        ShaderUtil.setUniformValue("InverseTransformMatrix", loc -> {
            Matrix4f projectionMatrix = new Matrix4f();
            projectionMatrix.load(ShaderUtil.getProjectionMatrix());

            Matrix4f projectionViewMatrix      = Matrix4f.mul(projectionMatrix, viewMatrix, null);
            // reuse the projection matrix instead of creating a new one
            Matrix4f inverseTransformMatrix    = Matrix4f.invert(projectionViewMatrix, projectionMatrix);

            FloatBuffer buf = ShaderUtil.getTempBuffer();
            inverseTransformMatrix.store(buf);
            buf.rewind();

            GL20.glUniformMatrix4(loc, false, buf);
        });
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
        bufferbuilder.pos(-2.0D, -2D, 0.0D).tex(0D, 0D).normal(0.0F, 1.0F, 0.0F).endVertex();
        bufferbuilder.pos(2.0D, -2D, 0.0D).tex(0D, 1D).normal(0.0F, 1.0F, 0.0F).endVertex();
        bufferbuilder.pos(2.0D, 2D, 0.0D).tex(1D, 1D).normal(0.0F, 1.0F, 0.0F).endVertex();
        bufferbuilder.pos(-2.0D, 2D, 0.0D).tex(1D, 0D).normal(0.0F, 1.0F, 0.0F).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
    }
}
