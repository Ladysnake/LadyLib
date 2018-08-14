package ladylib.client.shader;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import ladylib.LadyLib;
import ladylib.misc.MatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.message.FormattedMessage;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.client.renderer.OpenGlHelper.glGetProgramInfoLog;
import static net.minecraft.client.renderer.OpenGlHelper.glGetProgrami;
import static org.lwjgl.opengl.GL20.*;

/**
 * A class offering several utility methods to create, use and configure shaders
 */
@SideOnly(Side.CLIENT)
public final class ShaderUtil {
    private ShaderUtil() { }

    public static final String SHADER_LOCATION_PREFIX = "shaders/";

    private static int prevProgram = 0;
    private static int currentProgram = 0;
    private static final Map<ResourceLocation, ShaderGroup> screenShaders = new HashMap<>();
    private static boolean resetScreenShaders;
    private static int oldDisplayWidth = Minecraft.getMinecraft().displayWidth;
    private static int oldDisplayHeight = Minecraft.getMinecraft().displayHeight;

    private static final Object2IntMap<ResourceLocation> linkedShaders = new Object2IntOpenHashMap<>();
    private static boolean initialized = false;

    private static boolean shouldUseShaders() {
        return OpenGlHelper.shadersSupported;
    }

    /**
     * Subscribes this class to Minecraft's resource manager to reload shaders like normal assets.
     */
    public static void init() {
        if (!initialized) {
            Minecraft mc = Minecraft.getMinecraft();
            ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(ShaderUtil::loadShaders);
            initialized = true;
        }
    }

    /**
     * Sets the currently used program to a previously registered shader
     *
     * @param id the resource location used to register the shader
     */
    public static void useShader(ResourceLocation id) {
        useShader(linkedShaders.getInt(id));
    }

    /**
     * Sets the currently used program
     *
     * @param program the reference to the desired shader (0 to remove any current shader)
     */
    public static void useShader(int program) {
        if (!shouldUseShaders()) {
            return;
        }

        prevProgram = GlStateManager.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        OpenGlHelper.glUseProgram(program);

        currentProgram = program;
    }

    /**
     * Sets the value of an int uniform from the current shader program
     *
     * @param uniformName the name of the uniform field in the shader source file
     * @param value       an int value for this uniform
     */
    public static void setUniform(String uniformName, int value) {
        if (!shouldUseShaders() || currentProgram == 0) {
            return;
        }

        int uniform = GL20.glGetUniformLocation(currentProgram, uniformName);
        if (uniform != -1) {
            GL20.glUniform1i(uniform, value);
        }
    }

    /**
     * Sets the value of an uniform from the current shader program
     * If exactly 1 value is supplied, will set the value of a float uniform field
     * If between 2 and 4 values are supplied, will set the value of a vec uniform of corresponding length
     *
     * @param uniformName the name of the uniform field in the shader source file
     * @param values      between 1 and 4 float values
     */
    public static void setUniform(String uniformName, float... values) {
        if (!shouldUseShaders()) {
            return;
        }

        int uniform = GL20.glGetUniformLocation(currentProgram, uniformName);
        if (uniform != -1) {
            switch (values.length) {
                case 1:
                    GL20.glUniform1f(uniform, values[0]);
                    break;
                case 2:
                    GL20.glUniform2f(uniform, values[0], values[1]);
                    break;
                case 3:
                    GL20.glUniform3f(uniform, values[0], values[1], values[2]);
                    break;
                case 4:
                    GL20.glUniform4f(uniform, values[0], values[1], values[2], values[3]);
                    break;
                default:
                    throw new IllegalArgumentException("Shader float uniforms only support between 1 and 4 values");
            }
        }
    }

    /**
     * Sets the value of a mat4 uniform in the current shader
     *
     * @param uniformName the name of the uniform field in the shader source file
     * @param mat4        a raw array of float values
     */
    public static void setUniform(String uniformName, FloatBuffer mat4) {
        if (!shouldUseShaders()) {
            return;
        }

        int uniform = GL20.glGetUniformLocation(currentProgram, uniformName);
        if (uniform != -1) {
            GL20.glUniformMatrix4(uniform, true, mat4);
        }
    }

    /**
     * Binds any number of additional textures to be used by the current shader.
     * <p>
     * The default texture (0) is unaffected.
     * Shaders can access these textures by using uniforms named "textureN" with N
     * being the index of the additional texture, starting at 1.
     * </p>
     *
     * <u>Example:</u> The call {@code bindAdditionalTextures(rl1, rl2, rl3)} will let the shader
     * access those textures via the uniforms <pre>{@code
     * uniform sampler2D texture;   // the texture that's currently being drawn
     * uniform sampler2D texture1;  // the texture designated by rl1
     * uniform sampler2D texture2;  // the texture designated by rl2
     * uniform sampler2D texture3;  // the texture designated by rl3
     * }</pre>
     */
    public static void bindAdditionalTextures(ResourceLocation... textures) {
        for (int i = 0; i < textures.length; i++) {
            ResourceLocation texture = textures[i];
            // don't mess with the lightmap (1) nor the default texture (0)
            GlStateManager.setActiveTexture(i + OpenGlHelper.defaultTexUnit + 2);
            Minecraft.getMinecraft().renderEngine.bindTexture(texture);
            // start texture uniforms at 1, as 0 would be the default texture which doesn't require any special operation
            setUniform("texture" + (i + 1), i + 2);
        }
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    public static FloatBuffer getProjectionMatrix() {
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, (FloatBuffer) projection.position(0));
        projection.position(0);
        return projection;
    }

    public static FloatBuffer getProjectionMatrixInverse() {
        FloatBuffer projection = ShaderUtil.getProjectionMatrix();
        FloatBuffer projectionInverse = BufferUtils.createFloatBuffer(16);
        MatUtil.invertMat4FBFA((FloatBuffer) projectionInverse.position(0), (FloatBuffer) projection.position(0));
        projection.position(0);
        projectionInverse.position(0);
        return projectionInverse;
    }

    /**
     * This one is actually broken for some reason
     */
    public static FloatBuffer getModelViewMatrix() {
        FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, (FloatBuffer) modelView.position(0));
        modelView.position(0);
        return modelView;
    }

    public static FloatBuffer getModelViewMatrixInverse() {
        FloatBuffer modelView = ShaderUtil.getModelViewMatrix();
        FloatBuffer modelViewInverse = ByteBuffer.allocateDirect(16 * Float.BYTES).asFloatBuffer();
        MatUtil.invertMat4FBFA((FloatBuffer) modelViewInverse.position(0), (FloatBuffer) modelView.position(0));
        modelView.position(0);
        modelViewInverse.position(0);
        return modelViewInverse;
    }

    /**
     * Reverts to the previous shader used
     */
    public static void revert() {
        useShader(prevProgram);
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
      Screen shaders, shaders that are applied during post processing
    * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * Enables a screen shader.
     * <p>
     *     Once such a shader is enabled, it will keep displaying until it is disabled or the game is quit.
     * <p>
     *     The location should point to a JSON file describing a shader in the vanilla Minecraft format.
     *     Following vanilla's convention, it should be in <tt>yourmod:shaders/post/</tt>.
     *     See vanilla shaders for examples.
     * <p>
     *     Unlike shaders enabled through {@link EntityRenderer#loadEntityShader(Entity)}, multiple shaders can
     *     be enabled at once.
     * @param location the location of the shader to load.
     * @see #disableScreenShader(ResourceLocation) 
     */
    public static void enableScreenShader(ResourceLocation location) {
        if (shouldUseShaders() && !screenShaders.containsKey(location)) {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                resetScreenShaders = true;
                screenShaders.put(location, new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), location));
            } catch (IOException e) {
                LadyLib.LOGGER.error(new FormattedMessage("Could not enable screen shader {}", location), e);
            }
        }
    }

    /**
     * Disables a screen shader.
     * <p>
     *     Calling this method when the shader has not been enabled has no effect.
     * @param location the location used to load the shader
     * @see #enableScreenShader(ResourceLocation)
     */
    public static void disableScreenShader(ResourceLocation location) {
        if (screenShaders.containsKey(location)) {
            screenShaders.remove(location).deleteShaderGroup();
        }
    }

    @SubscribeEvent
    public static void renderScreenShaders(RenderGameOverlayEvent.Pre event) {
        if (shouldUseShaders() && !screenShaders.isEmpty() && event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            resetScreenShaders();
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.loadIdentity();
            for (ShaderGroup shaderGroup : screenShaders.values()) {
                GlStateManager.pushMatrix();
                setScreenUniform(shaderGroup);
                shaderGroup.render(event.getPartialTicks());
                GlStateManager.popMatrix();
            }
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
        }
    }

    public static void setScreenUniform(ShaderGroup shaderGroup) {
        for (Shader shader : shaderGroup.listShaders) {
            shader.getShaderManager().getShaderUniformOrDefault("SystemTime").set(System.currentTimeMillis());
        }
    }

    private static void resetScreenShaders() {
        Minecraft mc = Minecraft.getMinecraft();
        if (resetScreenShaders || mc.displayWidth != oldDisplayWidth || oldDisplayHeight != mc.displayHeight) {
            for (ShaderGroup sg : screenShaders.values()) {
                sg.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
            }

            oldDisplayWidth = mc.displayWidth;
            oldDisplayHeight = mc.displayHeight;
            resetScreenShaders = false;
        }
    }

    /**
     * Loads and links all registered shaders
     *
     * @param resourceManager Minecraft's resource manager
     */
    private static void loadShaders(IResourceManager resourceManager) {
        if (!shouldUseShaders()) {
            return;
        }
        Map<ResourceLocation, ShaderPair> registeredShaders = new HashMap<>();
        MinecraftForge.EVENT_BUS.post(new ShaderRegistryEvent(registeredShaders));
        registeredShaders.forEach((rl, sh) -> {
            try {
                linkedShaders.put(rl, loadShader(resourceManager, sh.getVertex(), sh.getFragment()));
            } catch (Exception e) {
                LadyLib.LOGGER.error(new FormattedMessage("Could not create shader {} from vertex {} and fragment {}", rl, sh.getVertex(), sh.getFragment(), e));
            }
        });
    }

    /**
     * Initializes a program with one or two shaders
     *
     * @param vertexLocation   the name or relative location of the vertex shader
     * @param fragmentLocation the name or relative location of the fragment shader
     * @return the reference to the initialized program
     */
    private static int loadShader(IResourceManager resourceManager, @Nullable ResourceLocation vertexLocation, @Nullable ResourceLocation fragmentLocation) throws IOException {

        // program creation
        int programId = OpenGlHelper.glCreateProgram();

        int vertexShaderId = 0;
        int fragmentShaderId = 0;

        // vertex shader creation
        if (vertexLocation != null) {
            vertexShaderId = OpenGlHelper.glCreateShader(OpenGlHelper.GL_VERTEX_SHADER);
            ARBShaderObjects.glShaderSourceARB(vertexShaderId, fromFile(resourceManager, vertexLocation));
            ARBShaderObjects.glCompileShaderARB(vertexShaderId);
            ARBShaderObjects.glAttachObjectARB(programId, vertexShaderId);
            String log = glGetShaderInfoLog(vertexShaderId, 1024);
            if (!log.isEmpty()) {
                LadyLib.LOGGER.error("Could not compile vertex shader " + vertexLocation + ": " + log);
            }
        }

        // fragment shader creation
        if (fragmentLocation != null) {
            fragmentShaderId = OpenGlHelper.glCreateShader(OpenGlHelper.GL_FRAGMENT_SHADER);
            ARBShaderObjects.glShaderSourceARB(fragmentShaderId, fromFile(resourceManager, fragmentLocation));
            ARBShaderObjects.glCompileShaderARB(fragmentShaderId);
            ARBShaderObjects.glAttachObjectARB(programId, fragmentShaderId);
            String log = glGetShaderInfoLog(fragmentShaderId, 1024);
            if (!log.isEmpty()) {
                LadyLib.LOGGER.error("Could not compile fragment shader " + fragmentLocation + ": " + log);
            }
        }

        OpenGlHelper.glLinkProgram(programId);
        // check potential linkage errors
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new ShaderException("Error linking Shader code: " + glGetProgramInfoLog(programId, 1024));
        }

        // free up the vertex and fragment shaders
        if (vertexShaderId != 0) {
            glDetachShader(programId, vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            glDetachShader(programId, fragmentShaderId);
        }

        // validate the program
        // this may fail even when it works fine so only show the message in a dev environment
        if (LadyLib.isDevEnv()) {
            glValidateProgram(programId);
            if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
                LadyLib.LOGGER.warn("Warning validating Shader code:" + glGetProgramInfoLog(programId, 1024));
            }
        }


        return programId;
    }

    /**
     * Reads a text file into a single String
     *
     * @param fileLocation the path to the file to read
     * @return a string with the content of the file
     */
    private static String fromFile(IResourceManager resourceManager, ResourceLocation fileLocation) throws IOException {
        StringBuilder source = new StringBuilder();

        try (InputStream in = resourceManager.getResource(fileLocation).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                source.append(line).append('\n');
            }
        }
        return source.toString();
    }

}
