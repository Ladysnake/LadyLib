package ladylib.client.lighting;

import ladylib.LadyLib;
import ladylib.misc.ReflectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL11;

import static net.minecraft.client.renderer.OpenGlHelper.*;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT;
import static org.lwjgl.opengl.EXTPackedDepthStencil.GL_DEPTH24_STENCIL8_EXT;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_DEFAULT;

@Mod.EventBusSubscriber(modid = LadyLib.MOD_ID, value = Side.CLIENT)
public class FramebufferReplacement extends Framebuffer {
    private int depthTexture = -1;

    public FramebufferReplacement(int width, int height, boolean useDepthIn) {
        super(width, height, useDepthIn);
    }

    @Override
    public void createFramebuffer(int width, int height) {
        this.framebufferWidth = width;
        this.framebufferHeight = height;
        this.framebufferTextureWidth = width;
        this.framebufferTextureHeight = height;

        if (!isFramebufferEnabled()) {
            this.framebufferClear();
        } else {
            this.framebufferObject = glGenFramebuffers();
            this.framebufferTexture = TextureUtil.glGenTextures();

            this.setFramebufferFilter(9728);
            GlStateManager.bindTexture(this.framebufferTexture);
            GlStateManager.glTexImage2D(3553, 0, 32856, this.framebufferTextureWidth, this.framebufferTextureHeight, 0, 6408, 5121, null);
            glBindFramebuffer(GL_FRAMEBUFFER, this.framebufferObject);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, 3553, this.framebufferTexture, 0);

            if (this.useDepth) {
                //CREATE FB TEXTURE
                this.depthTexture = GL11.glGenTextures();
                GlStateManager.bindTexture(this.depthTexture);
                GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                // attach the texture to FBO depth attachment point
                if (!this.isStencilEnabled()) {
                    GlStateManager.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, this.framebufferTextureWidth, this.framebufferTextureHeight, 0,GL_DEPTH_COMPONENT, GL_UNSIGNED_BYTE, null);
                    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D, this.depthTexture,0);
                } else {
                    GlStateManager.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH24_STENCIL8_EXT, this.framebufferTextureWidth, this.framebufferTextureHeight, 0,GL_DEPTH_COMPONENT, GL_UNSIGNED_BYTE, null);
                    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT_EXT, GL_TEXTURE_2D, this.depthTexture,0);
                    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT_EXT, GL_TEXTURE_2D, this.depthTexture,0);
                }
                GlStateManager.bindTexture(this.framebufferTexture);
            }

            this.framebufferClear();
            this.unbindFramebufferTexture();
        }
    }

    @Override
    public void deleteFramebuffer() {
        super.deleteFramebuffer();

        if (this.depthTexture > -1) {
            TextureUtil.deleteTexture(this.depthTexture);
            this.depthTexture = -1;
        }
    }


    private static int mainDepthTexture = -2;

    public static void replaceMinecraftFramebuffer() {
        Minecraft mc = Minecraft.getMinecraft();
        Framebuffer main = mc.getFramebuffer();
        Framebuffer replacement = new FramebufferReplacement(main.framebufferTextureWidth, main.framebufferTextureHeight, main.useDepth);
        ReflectionUtil.setPrivateValue(Minecraft.class, mc, "field_147124_at", Framebuffer.class, replacement);
    }

    /**
     * A return value < 0 means that the depth texture doesn't exist
     * @return the depth texture of Minecraft's main framebuffer
     */
    public static int getMainDepthTexture() {
        // -2 means it hasn't been initialized yet
        if (mainDepthTexture < -1) {
            int attachmentObjectType = ARBFramebufferObject.glGetFramebufferAttachmentParameteri(
                    GL_FRAMEBUFFER,
                    GL_DEPTH_ATTACHMENT,
                    ARBFramebufferObject.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
            );
            if (attachmentObjectType != GL_TEXTURE) {
                String type;
                if (attachmentObjectType == GL_RENDERBUFFER) {
                    type = "GL_RENDERBUFFER";
                } else if (attachmentObjectType == GL_NONE) {
                    type = "GL_NONE";
                } else if (attachmentObjectType == GL_FRAMEBUFFER_DEFAULT) {
                    type = "GL_FRAMEBUFFER_DEFAULT";
                } else {
                    type = "UNKNOWN (" + attachmentObjectType + ")";
                }
                // Our framebuffer replacement is not doing its job
                LadyLib.LOGGER.warn("Minecraft main framebuffer has not been replaced by our version (expected a texture attachment, got {} in {})", type, Minecraft.getMinecraft().getFramebuffer().getClass().getName());
                mainDepthTexture = -1;
            } else {
                mainDepthTexture = ARBFramebufferObject.glGetFramebufferAttachmentParameteri(
                        GL_FRAMEBUFFER,
                        GL_DEPTH_ATTACHMENT,
                        ARBFramebufferObject.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
                );
            }
        }
        return mainDepthTexture;
    }
}
