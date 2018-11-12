package ladylib.client.shader;

import ladylib.LadyLib;
import ladylib.misc.PublicApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.shader.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.message.FormattedMessage;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A post processing shader that is applied to the main framebuffer
 * <p>
 * See <tt>assets/minecraft/shaders</tt> for examples
 * </p>
 */
public final class ScreenShader {

    private static List<ScreenShader> screenShaders = new ArrayList<>();
    private static int oldDisplayWidth = Minecraft.getMinecraft().displayWidth;
    private static int oldDisplayHeight = Minecraft.getMinecraft().displayHeight;

    @SubscribeEvent
    static void onTickRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START && ShaderUtil.shouldUseShaders() && !screenShaders.isEmpty()) {
            updateScreenShaders();
        }
    }

    static void resetShaders() {
        for (ScreenShader ss : screenShaders) {
            ss.shaderGroup = null;
        }
    }

    private static void updateScreenShaders() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.displayWidth != oldDisplayWidth || oldDisplayHeight != mc.displayHeight) {
            for (ScreenShader ss : screenShaders) {
                if (ss.shaderGroup != null) {
                    ss.shaderGroup.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
                }
            }

            oldDisplayWidth = mc.displayWidth;
            oldDisplayHeight = mc.displayHeight;
        }
    }

    /**
     * Loads a post processing shader from a json definition file
     * @param location the location of the json within your mod's assets
     * @return a lazily initialized screen shader
     */
    @PublicApi
    public static ScreenShader loadScreenShader(ResourceLocation location) {
        return loadScreenShader(location, () -> {});
    }

    /**
     * Loads a post processing shader from a json definition file
     * @param location the location of the json within your mod's assets
     * @param uniformInitBlock a block ran once to initialize uniforms
     * @return a lazily initialized screen shader
     */
    @PublicApi
    public static ScreenShader loadScreenShader(ResourceLocation location, Runnable uniformInitBlock) {
        ScreenShader ret = new ScreenShader(location, uniformInitBlock);
        screenShaders.add(ret);
        return ret;
    }

    private final ResourceLocation location;
    private final Runnable uniformInitBlock;
    @Nullable private ShaderGroup shaderGroup;

    private ScreenShader(ResourceLocation location, Runnable uniformInitBlock) {
        this.location = location;
        this.uniformInitBlock = uniformInitBlock;
    }

    /**
     * Creates and initializes this shader's {@link ShaderGroup} if it doesn't exist
     */
    public ShaderGroup getShaderGroup() {
        if (shaderGroup == null) {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                this.shaderGroup = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), this.location);
                this.shaderGroup.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
                this.uniformInitBlock.run();
            } catch (IOException e) {
                LadyLib.LOGGER.error(new FormattedMessage("Could not create screen shader {}", location), e);
            }
        }
        return shaderGroup;
    }

    /**
     * Renders this shader
     *
     * <p>
     *     This method should be called every frame when the shader is active.
     *     Uniforms should be set before rendering.
     */
    public void render(float partialTicks) {
        GlStateManager.matrixMode(GL11.GL_TEXTURE);
        GlStateManager.loadIdentity();
        getShaderGroup().render(partialTicks);
        Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); // restore blending
        GlStateManager.enableDepth();
    }

    /**
     * Forwards to {@link #setupDynamicUniforms(int, Runnable)} with an index of 0
     * @param dynamicSetBlock a block in which dynamic uniforms are set
     */
    @PublicApi
    public void setupDynamicUniforms(Runnable dynamicSetBlock) {
        this.setupDynamicUniforms(0, dynamicSetBlock);
    }

    /**
     * Runs the given block while the shader at the given index is active
     *
     * @param index the shader index within the group
     * @param dynamicSetBlock a block in which dynamic name uniforms are set
     */
    @PublicApi
    public void setupDynamicUniforms(int index, Runnable dynamicSetBlock) {
        ShaderManager sm = this.getShaderGroup().listShaders.get(index).getShaderManager();
        ShaderUtil.useShader(sm.getProgram());
        dynamicSetBlock.run();
        ShaderUtil.revert();
    }

    /**
     * Sets the value of a uniform declared in json
     * @param uniformName the name of the uniform field in the shader source file
     * @param value int value
     */
    @PublicApi
    public void setUniformValue(String uniformName, int value) {
        setUniformValue(uniformName, value, 0, 0, 0);
    }

    /**
     * Sets the value of a uniform declared in json
     * @param uniformName the name of the uniform field in the shader source file
     * @param value0 int value
     * @param value1 int value
     */
    @PublicApi
    public void setUniformValue(String uniformName, int value0, int value1) {
        setUniformValue(uniformName, value0, value1, 0, 0);
    }

    /**
     * Sets the value of a uniform declared in json
     * @param uniformName the name of the uniform field in the shader source file
     * @param value0 int value
     * @param value1 int value
     * @param value2 int value
     */
    @PublicApi
    public void setUniformValue(String uniformName, int value0, int value1, int value2) {
        setUniformValue(uniformName, value0, value1, value2, 0);
    }

    /**
     * Sets the value of a uniform declared in json
     * @param uniformName the name of the uniform field in the shader source file
     * @param value0 int value
     * @param value1 int value
     * @param value2 int value
     * @param value3 int value
     */
    @PublicApi
    public void setUniformValue(String uniformName, int value0, int value1, int value2, int value3) {
        for (Shader shader : getShaderGroup().listShaders) {
            shader.getShaderManager().getShaderUniformOrDefault(uniformName).set(value0, value1, value2, value3);
        }
    }

    /**
     * Sets the value of a uniform declared in json
     * @param uniformName the name of the uniform field in the shader source file
     * @param value float value
     */
    @PublicApi
    public void setUniformValue(String uniformName, float value) {
        getShaderGroup();
        for (Shader shader : getShaderGroup().listShaders) {
            ShaderUniform uniform = shader.getShaderManager().getShaderUniformOrDefault(uniformName);
            uniform.set(value);
        }
    }

    /**
     * Sets the value of a uniform declared in json
     * @param uniformName the name of the uniform field in the shader source file
     * @param value0 float value
     * @param value1 float value
     */
    @PublicApi
    public void setUniformValue(String uniformName, float value0, float value1) {
        for (Shader shader : getShaderGroup().listShaders) {
            ShaderUniform uniform = shader.getShaderManager().getShaderUniformOrDefault(uniformName);
            uniform.set(value0, value1);
        }
    }

    /**
     * Sets the value of a uniform declared in json
     * @param uniformName the name of the uniform field in the shader source file
     * @param value0 float value
     * @param value1 float value
     * @param value2 float value
     */
    @PublicApi
    public void setUniformValue(String uniformName, float value0, float value1, float value2) {
        for (Shader shader : getShaderGroup().listShaders) {
            ShaderUniform uniform = shader.getShaderManager().getShaderUniformOrDefault(uniformName);
            uniform.set(value0, value1, value2);
        }
    }

    /**
     * Sets the value of a uniform declared in json
     * @param uniformName the name of the uniform field in the shader source file
     * @param value0 float value
     * @param value1 float value
     * @param value2 float value
     * @param value3 float value
     */
    @PublicApi
    public void setUniformValue(String uniformName, float value0, float value1, float value2, float value3) {
        for (Shader shader : getShaderGroup().listShaders) {
            ShaderUniform uniform = shader.getShaderManager().getShaderUniformOrDefault(uniformName);
            uniform.set(value0, value1, value2, value3);
        }
    }

    /**
     * Sets the value of a sampler uniform declared in json
     * @param samplerName the name of the sampler uniform field in the shader source file and json
     * @param texture a texture object
     */
    @PublicApi
    public void setScreenSampler(String samplerName, ITextureObject texture) {
        setScreenSampler(samplerName, (Object) texture);
    }

    /**
     * Sets the value of a sampler uniform declared in json
     * @param samplerName the name of the sampler uniform field in the shader source file and json
     * @param textureFbo a framebuffer which main texture will be used
     */
    @PublicApi
    public void setScreenSampler(String samplerName, Framebuffer textureFbo) {
        setScreenSampler(samplerName, (Object) textureFbo);
    }

    /**
     * Sets the value of a sampler uniform declared in json
     * @param samplerName the name of the sampler uniform field in the shader source file and json
     * @param textureName an opengl texture name
     */
    @PublicApi
    public void setScreenSampler(String samplerName, int textureName) {
        setScreenSampler(samplerName, Integer.valueOf(textureName));
    }

    private void setScreenSampler(String samplerName, Object texture) {
        for (Shader shader : getShaderGroup().listShaders) {
            shader.getShaderManager().addSamplerTexture(samplerName, texture);
        }
    }

}
