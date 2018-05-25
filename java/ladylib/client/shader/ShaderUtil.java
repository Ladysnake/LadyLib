package ladylib.client.shader;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import ladylib.LadyLib;
import ladylib.misc.MatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.HashMap;
import java.util.Map;

/**
 * A class offering several utility methods to create, use and configure shaders
 */
@SideOnly(Side.CLIENT)
public class ShaderUtil {

    private static int prevProgram = 0, currentProgram = 0;
    static final String SHADER_LOCATION_PREFIX = "shaders/";

    private static final Object2IntMap<ResourceLocation> linkedShaders = new Object2IntOpenHashMap<>();
    private static boolean initialized = false;

    private static Framebuffer framebufferLL;

    private static boolean shouldNotUseShaders() {
        return !OpenGlHelper.shadersSupported;
    }

    /**
     * Subscribes this class to Minecraft's resource manager to reload shaders like normal assets.
     */
    public static void init() {
        if (!initialized) {
            Minecraft mc = Minecraft.getMinecraft();
            ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(ShaderUtil::loadShaders);
            framebufferLL = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
            framebufferLL.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
            initialized = true;
        }
    }

    /**
     * Loads and links all registered shaders
     *
     * @param resourceManager Minecraft's resource manager
     */
    private static void loadShaders(IResourceManager resourceManager) {
        if (shouldNotUseShaders())
            return;
        Map<ResourceLocation, Pair<ResourceLocation, ResourceLocation>> registeredShaders = new HashMap<>();
        MinecraftForge.EVENT_BUS.post(new ShaderRegistryEvent(registeredShaders));
        registeredShaders.forEach((rl, sh) -> linkedShaders.put(rl, loadShader(resourceManager, sh.getLeft(), sh.getRight())));
    }

    /**
     * Initializes a program with one or two shaders
     *
     * @param vertexLocation   the name or relative location of the vertex shader
     * @param fragmentLocation the name or relative location of the fragment shader
     * @return the reference to the initialized program
     */
    private static int loadShader(IResourceManager resourceManager, @Nullable ResourceLocation vertexLocation, @Nullable ResourceLocation fragmentLocation) {

        // program creation
        int program = OpenGlHelper.glCreateProgram();

        // vertex shader creation
        if (vertexLocation != null) {
            int vertexShader = OpenGlHelper.glCreateShader(OpenGlHelper.GL_VERTEX_SHADER);
            ARBShaderObjects.glShaderSourceARB(vertexShader, fromFile(resourceManager, vertexLocation));
            OpenGlHelper.glCompileShader(vertexShader);
            OpenGlHelper.glAttachShader(program, vertexShader);
        }

        // fragment shader creation
        if (fragmentLocation != null) {
            int fragmentShader = OpenGlHelper.glCreateShader(OpenGlHelper.GL_FRAGMENT_SHADER);
            ARBShaderObjects.glShaderSourceARB(fragmentShader, fromFile(resourceManager, fragmentLocation));
            OpenGlHelper.glCompileShader(fragmentShader);
            OpenGlHelper.glAttachShader(program, fragmentShader);
        }

        OpenGlHelper.glLinkProgram(program);

        return program;
    }

    /**
     * Renders Minecraft's main framebuffer using the current shader.
     * <p>
     *     Note that the shader will be reverted automatically during the operation.
     *     Calling this method with no shader bound will not have any apparent result.
     * </p>
     * FIXME I don't know how framebuffers work
     */
    public static void renderMainWithShaders() {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableAlpha();
        GlStateManager.disableFog();
        GlStateManager.disableLighting();
        GlStateManager.disableColorMaterial();
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(0);

        Framebuffer framebufferIn = Minecraft.getMinecraft().getFramebuffer();
        Framebuffer framebufferOut = framebufferLL;
        framebufferIn.unbindFramebuffer();
        float f = framebufferOut.framebufferTextureWidth;
        float f1 = framebufferOut.framebufferTextureHeight;
        GlStateManager.viewport(0, 0, (int)f, (int)f1);

        framebufferOut.framebufferClear();
        framebufferOut.bindFramebuffer(false);
        GlStateManager.depthMask(false);
        GlStateManager.colorMask(true, true, true, true);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(0.0D, (double)f1, 500.0D).color(255, 255, 255, 255).endVertex();
        bufferbuilder.pos((double)f, (double)f1, 500.0D).color(255, 255, 255, 255).endVertex();
        bufferbuilder.pos((double)f, 0.0D, 500.0D).color(255, 255, 255, 255).endVertex();
        bufferbuilder.pos(0.0D, 0.0D, 500.0D).color(255, 255, 255, 255).endVertex();
        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.colorMask(true, true, true, true);
        framebufferOut.unbindFramebuffer();
        framebufferIn.unbindFramebufferTexture();
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
        if (shouldNotUseShaders())
            return;

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
        if (shouldNotUseShaders() || currentProgram == 0)
            return;

        int uniform = GL20.glGetUniformLocation(currentProgram, uniformName);
        if (uniform != -1)
            GL20.glUniform1i(uniform, value);
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
        if (shouldNotUseShaders())
            return;

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
        if (shouldNotUseShaders())
            return;

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

    /**
     * Reads a text file into a single String
     *
     * @param fileLocation the path to the file to read
     * @return a string with the content of the file
     */
    private static String fromFile(IResourceManager resourceManager, ResourceLocation fileLocation) {
        StringBuilder source = new StringBuilder();

        try (InputStream in = resourceManager.getResource(fileLocation).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null)
                source.append(line).append('\n');
        } catch (IOException exc) {
            LadyLib.LOGGER.error(exc);
        } catch (NullPointerException e) {
            LadyLib.LOGGER.error(e + " : " + fileLocation + " does not exist");
        }

        return source.toString();
    }

}
