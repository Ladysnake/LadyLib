package ladylib.client.shader;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.IContextSetter;

import java.util.Map;

/**
 * Register your shaders when you receive this event
 */
public class ShaderRegistryEvent extends Event implements IContextSetter {
    private final Map<ResourceLocation, ShaderPair> registeredShaders;

    public ShaderRegistryEvent(Map<ResourceLocation, ShaderPair> registeredShaders) {
        this.registeredShaders = registeredShaders;
    }

    /**
     * Convenience method to register a shader with the fragment and vertex shaders having the same name <br>
     * The corresponding program will be created and linked during the next ResourceManager reloading.
     *
     * <p>
     *     <u>Example:</u> Using the identifier <tt>gaspunk:gas_overlay</tt> will register a shader using
     *     the file <tt>assets/gaspunk/shaders/gas_overlay.vsh</tt> as its vertex shader and
     *     <tt>assets/gaspunk/shaders/gas_overlay.fsh</tt> as its fragment shader.
     * </p>
     *
     * @param identifier the common name or relative location of both shaders, minus the file extension
     */
    public void registerShader(ResourceLocation identifier) {
        registerShader(
                identifier,
                new ResourceLocation(identifier.getNamespace(), ShaderUtil.SHADER_LOCATION_PREFIX + identifier.getPath() + ".vsh"),
                new ResourceLocation(identifier.getNamespace(), ShaderUtil.SHADER_LOCATION_PREFIX + identifier.getPath() + ".fsh")
        );
    }

    /**
     * Registers a shader using the given vertex and fragment. <br>
     * The corresponding program will be created and linked during the next ResourceManager reloading
     *
     * @param identifier a unique resource location that will be used to load this shader
     * @param vertex     the file name of the vertex shader, extension included
     * @param fragment   the file name of the fragment shader, extension included
     */
    public void registerShader(ResourceLocation identifier, ResourceLocation vertex, ResourceLocation fragment) {
        registeredShaders.put(identifier, new ShaderPair(fragment, vertex));
    }

}
