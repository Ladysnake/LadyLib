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
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A post processing shader that is applied to the main framebuffer
 * <p>
 *     Post shaders loaded through {@link #loadShader(ResourceLocation, Consumer)} are self-managed and will be
 *     reloaded when shader assets are reloaded (through <tt>F3-T</tt> or <tt>/ladylib_shader_reload</tt>) or the
 *     screen resolution changes.
 * <p>
 * @since 2.4.0
 * @see ShaderUtil
 * @see "<tt>assets/minecraft/shaders</tt> for examples"
 */
@Mod.EventBusSubscriber(modid = LadyLib.MOD_ID)
public final class PostProcessShader {

    // Let shaders be garbage collected when no one uses them
    private static Set<PostProcessShader> postProcessShaders = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * Loads a post processing shader from a json definition file
     * @param location the location of the json within your mod's assets
     * @return a lazily initialized screen shader
     */
    @PublicApi
    public static PostProcessShader loadShader(ResourceLocation location) {
        return loadShader(location, s -> {});
    }

    /**
     * Loads a post processing shader from a json definition file
     * @param location the location of the json within your mod's assets
     * @param uniformInitBlock a block ran once to initialize uniforms
     * @return a lazily initialized screen shader
     */
    @PublicApi
    public static PostProcessShader loadShader(ResourceLocation location, Consumer<PostProcessShader> uniformInitBlock) {
        PostProcessShader ret = new PostProcessShader(location, uniformInitBlock);
        postProcessShaders.add(ret);
        return ret;
    }

    private final ResourceLocation location;
    private final Consumer<PostProcessShader> uniformInitBlock;
    private ShaderGroup shaderGroup;
    private boolean errored;

    private PostProcessShader(ResourceLocation location, Consumer<PostProcessShader> uniformInitBlock) {
        this.location = location;
        this.uniformInitBlock = uniformInitBlock;
    }

    /**
     * Returns this shader's {@link ShaderGroup}, creating and initializing it if it doesn't exist.
     * <p>
     *     This method will return <code>null</code> if an error occurs during initialization.
     * <p>
     *     <em>Note: calling this before the graphic context is ready will cause issues.</em>
     * @see #initialize()
     * @see #isInitialized()
     */
    @Nullable
    @PublicApi
    public ShaderGroup getShaderGroup() {
        if (!this.isInitialized() && !this.errored) {
            try {
                initialize();
            } catch (Exception e) {
                LadyLib.LOGGER.error("Could not create screen shader {}", location, e);
                this.errored = true;
            }
        }
        return this.shaderGroup;
    }

    /**
     * Initializes this shader, allocating required system resources
     * such as framebuffer objects, shaders objects and texture objects.
     * Any exception thrown during initialization is relayed to the caller.
     * <p>
     *     If the shader is already initialized, previously allocated
     *     resources will be disposed of before initializing new ones.
     * @apiNote Calling this method directly is not required in most cases.
     * @see #getShaderGroup()
     * @see #isInitialized()
     * @see #dispose(boolean)
     */
    @PublicApi
    public void initialize() throws IOException {
        this.dispose(false);
        Minecraft mc = Minecraft.getMinecraft();
        this.shaderGroup = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), this.location);
        this.shaderGroup.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
        this.uniformInitBlock.accept(this);
    }

    /**
     * Checks whether this shader is initialized. If it is not, next call to {@link #getShaderGroup()}
     * will setup the shader group.
     * @return true if this does not require initialization
     * @see #initialize()
     */
    @PublicApi
    public boolean isInitialized() {
        return this.shaderGroup != null;
    }

    /**
     * @return <code>true</code> if this shader erred during initialization
     */
    @PublicApi
    public boolean isErrored() {
        return this.errored;
    }

    /**
     * Releases this shader's resources.
     * <p>
     *     After this method is called, this shader will go back to its uninitialized state.
     *     Future calls to {@link #isInitialized()} will return false until {@link #initialize()}
     *     is called again, recreating the shader group.
     * <p>
     *     If <code>removeFromManaged</code> is true, this shader will also be removed from the global
     *     list of managed shaders, making it not respond to resource reloading and screen resizing.
     *     A <code>PostProcessShader</code> object cannot be used after <code>dispose(true)</code>
     *     has been called.
     * <p>
     *     Although the finalization process of the garbage collector
     *     also disposes of the same system resources, it is preferable
     *     to manually free the associated resources by calling this
     *     method rather than to rely on a finalization process which
     *     may not run to completion for a long period of time.
     * @param removeFromManaged whether this shader should stop being automatically managed
     * @see #isInitialized()
     * @see #getShaderGroup()
     * @see #finalize()
     */
    @PublicApi
    public void dispose(boolean removeFromManaged) {
        if (this.isInitialized()) {
            this.shaderGroup.deleteShaderGroup();
            this.shaderGroup = null;
        }
        this.errored = false;
        if (removeFromManaged) {
            postProcessShaders.remove(this);
        }
    }

    /**
     * Renders this shader.
     *
     * <p>
     *     Calling this method first setups the graphic state for rendering,
     *     then uploads uniforms to the GPU if they have been changed since last
     *     draw, draws the {@link Minecraft#getFramebuffer() main framebuffer}'s texture
     *     to intermediate {@link Framebuffer framebuffers} as defined by the JSON files
     *     and resets part of the graphic state. The shader will be {@link #initialize() initialized}
     *     if it has not been before.
     * <p>
     *     This method should be called every frame when the shader is active.
     *     Uniforms should be set before rendering.
     */
    @PublicApi
    public void render(float partialTicks) {
        ShaderGroup sg = this.getShaderGroup();
        if (sg != null) {
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.loadIdentity();
            sg.render(partialTicks);
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); // restore blending
            GlStateManager.enableDepth();
        }
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
        ShaderGroup sg = this.getShaderGroup();
        if (sg != null) {
            ShaderManager sm = sg.listShaders.get(index).getShaderManager();
            ShaderUtil.useShader(sm.getProgram());
            dynamicSetBlock.run();
            ShaderUtil.revert();
        }
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
        ShaderGroup sg = this.getShaderGroup();
        if (sg != null) {
            for (Shader shader : sg.listShaders) {
                shader.getShaderManager().getShaderUniformOrDefault(uniformName).set(value0, value1, value2, value3);
            }
        }
    }

    /**
     * Sets the value of a uniform declared in json
     * @param uniformName the name of the uniform field in the shader source file
     * @param value float value
     */
    @PublicApi
    public void setUniformValue(String uniformName, float value) {
        ShaderGroup sg = this.getShaderGroup();
        if (sg != null) {
            for (Shader shader : sg.listShaders) {
                ShaderUniform uniform = shader.getShaderManager().getShaderUniformOrDefault(uniformName);
                uniform.set(value);
            }
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
        ShaderGroup sg = this.getShaderGroup();
        if (sg != null) {
            for (Shader shader : sg.listShaders) {
                ShaderUniform uniform = shader.getShaderManager().getShaderUniformOrDefault(uniformName);
                uniform.set(value0, value1);
            }
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
        ShaderGroup sg = this.getShaderGroup();
        if (sg != null) {
            for (Shader shader : sg.listShaders) {
                ShaderUniform uniform = shader.getShaderManager().getShaderUniformOrDefault(uniformName);
                uniform.set(value0, value1, value2);
            }
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
        ShaderGroup sg = this.getShaderGroup();
        if (sg != null) {
            for (Shader shader : sg.listShaders) {
                ShaderUniform uniform = shader.getShaderManager().getShaderUniformOrDefault(uniformName);
                uniform.set(value0, value1, value2, value3);
            }
        }
    }

    /**
     * Sets the value of a uniform declared in json
     * @param uniformName the name of the uniform field in the shader source file
     * @param value a matrix
     */
    @PublicApi
    public void setUniformValue(String uniformName, Matrix4f value) {
        ShaderGroup sg = this.getShaderGroup();
        if (sg != null) {
            for (Shader shader : sg.listShaders) {
                ShaderUniform uniform = shader.getShaderManager().getShaderUniformOrDefault(uniformName);
                uniform.set(value);
            }
        }
    }

    /**
     * Sets the value of a sampler uniform declared in json
     * @param samplerName the name of the sampler uniform field in the shader source file and json
     * @param texture a texture object
     */
    @PublicApi
    public void setSamplerUniform(String samplerName, ITextureObject texture) {
        setSamplerUniform(samplerName, (Object) texture);
    }

    /**
     * Sets the value of a sampler uniform declared in json
     * @param samplerName the name of the sampler uniform field in the shader source file and json
     * @param textureFbo a framebuffer which main texture will be used
     */
    @PublicApi
    public void setSamplerUniform(String samplerName, Framebuffer textureFbo) {
        setSamplerUniform(samplerName, (Object) textureFbo);
    }

    /**
     * Sets the value of a sampler uniform declared in json
     * @param samplerName the name of the sampler uniform field in the shader source file and json
     * @param textureName an opengl texture name
     */
    @PublicApi
    public void setSamplerUniform(String samplerName, int textureName) {
        setSamplerUniform(samplerName, Integer.valueOf(textureName));
    }

    private void setSamplerUniform(String samplerName, Object texture) {
        ShaderGroup sg = this.getShaderGroup();
        if (sg != null) {
            for (Shader shader : sg.listShaders) {
                shader.getShaderManager().addSamplerTexture(samplerName, texture);
            }
        }
    }

    /**
     * Disposes of this shader once it is no longer referenced.
     * @see #dispose
     */
    @Override
    protected void finalize() {
        this.dispose(true);
    }

    @EnhancedBusSubscriber(side = Side.CLIENT)
    static class ReloadHandler implements ISelectiveResourceReloadListener {
        static final ReloadHandler INSTANCE = new ReloadHandler();

        private static int oldDisplayWidth = Minecraft.getMinecraft().displayWidth;
        private static int oldDisplayHeight = Minecraft.getMinecraft().displayHeight;

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
            if (resourcePredicate.test(VanillaResourceType.SHADERS)) {
                for (PostProcessShader ss : postProcessShaders) {
                    ss.dispose(false);
                }
            }
        }

        @SubscribeEvent
        public void onTickRenderTick(TickEvent.RenderTickEvent event) {
            if (event.phase == TickEvent.Phase.START && ShaderUtil.shouldUseShaders() && !postProcessShaders.isEmpty()) {
                refreshScreenShaders();
            }
        }

        private void refreshScreenShaders() {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.displayWidth != oldDisplayWidth || oldDisplayHeight != mc.displayHeight) {
                for (PostProcessShader ss : postProcessShaders) {
                    if (ss.isInitialized()) {
                        ss.shaderGroup.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
                        ss.uniformInitBlock.accept(ss);
                    }
                }

                oldDisplayWidth = mc.displayWidth;
                oldDisplayHeight = mc.displayHeight;
            }
        }
    }

}
