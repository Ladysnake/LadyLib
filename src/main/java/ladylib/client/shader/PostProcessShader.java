package ladylib.client.shader;

import ladylib.LadyLib;
import ladylib.compat.EnhancedBusSubscriber;
import ladylib.misc.PublicApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.shader.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.message.FormattedMessage;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A post processing shader that is applied to the main framebuffer
 * <p>
 *     Post shaders loaded through {@link #loadScreenShader(ResourceLocation, Runnable)} are self-managed and will be
 *     reloaded when shader assets are reloaded (through <tt>F3-T</tt> or <tt>/ladylib_shader_reload</tt>) or the
 *     screen resolution changes.
 * <p>
 * @see "<tt>assets/minecraft/shaders</tt> for examples"
 */
@Mod.EventBusSubscriber(modid = LadyLib.MOD_ID)
public final class PostProcessShader {

    private static List<PostProcessShader> postProcessShaders = new ArrayList<>();

    /**
     * Loads a post processing shader from a json definition file
     * @param location the location of the json within your mod's assets
     * @return a lazily initialized screen shader
     */
    @PublicApi
    public static PostProcessShader loadScreenShader(ResourceLocation location) {
        return loadScreenShader(location, () -> {});
    }

    /**
     * Loads a post processing shader from a json definition file
     * @param location the location of the json within your mod's assets
     * @param uniformInitBlock a block ran once to initialize uniforms
     * @return a lazily initialized screen shader
     */
    @PublicApi
    public static PostProcessShader loadScreenShader(ResourceLocation location, Runnable uniformInitBlock) {
        PostProcessShader ret = new PostProcessShader(location, uniformInitBlock);
        postProcessShaders.add(ret);
        return ret;
    }

    private final ResourceLocation location;
    private final Runnable uniformInitBlock;
    @Nullable private ShaderGroup shaderGroup;

    private PostProcessShader(ResourceLocation location, Runnable uniformInitBlock) {
        this.location = location;
        this.uniformInitBlock = uniformInitBlock;
    }

    /**
     * Returns this shader's {@link ShaderGroup}, creating and initializing it if it doesn't exist.
     * <p>
     *     <em>Note: calling this before the graphic context is ready will cause issues.</em>
     */
    @PublicApi
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
     * Renders this shader.
     *
     * <p>
     *     Calling this method first setups the graphic state for rendering,
     *     then uploads uniforms to the GPU if they have been changed since last
     *     draw, draws the {@link Minecraft#getFramebuffer() main framebuffer}'s texture
     *     to intermediate {@link Framebuffer framebuffers} as defined by the JSON files
     *     and resets part of the graphic state.
     * <p>
     *     This method should be called every frame when the shader is active.
     *     Uniforms should be set before rendering.
     */
    @PublicApi
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

    @EnhancedBusSubscriber(side = Side.CLIENT)
    public static class ReloadHandler implements ISelectiveResourceReloadListener {
        static final ReloadHandler INSTANCE = new ReloadHandler();

        private static int oldDisplayWidth = Minecraft.getMinecraft().displayWidth;
        private static int oldDisplayHeight = Minecraft.getMinecraft().displayHeight;

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
            if (resourcePredicate.test(VanillaResourceType.SHADERS)) {
                for (PostProcessShader ss : postProcessShaders) {
                    ss.shaderGroup = null;
                }
            }
        }

        @SubscribeEvent
        public void onTickRenderTick(TickEvent.RenderTickEvent event) {
            if (event.phase == TickEvent.Phase.START && ShaderUtil.shouldUseShaders() && !postProcessShaders.isEmpty()) {
                updateScreenShaders();
            }
        }

        private void updateScreenShaders() {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.displayWidth != oldDisplayWidth || oldDisplayHeight != mc.displayHeight) {
                for (PostProcessShader ss : postProcessShaders) {
                    if (ss.shaderGroup != null) {
                        ss.shaderGroup.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
                    }
                }

                oldDisplayWidth = mc.displayWidth;
                oldDisplayHeight = mc.displayHeight;
            }
        }
    }

}
