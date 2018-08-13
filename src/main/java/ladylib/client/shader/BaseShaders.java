package ladylib.client.shader;

import ladylib.modwinder.client.gui.MWShaders;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import static ladylib.LadyLib.MOD_ID;
import static ladylib.client.shader.ShaderUtil.SHADER_LOCATION_PREFIX;

/**
 * A few default shaders that other mods can use
 */
@Mod.EventBusSubscriber(modid = MOD_ID, value = Side.CLIENT)
public final class BaseShaders {
    private BaseShaders() { }

    public static final ResourceLocation BASE_VERTEX = new ResourceLocation(MOD_ID, SHADER_LOCATION_PREFIX + "vertex_base.vsh");
    public static final ResourceLocation BASE_FRAGMENT = new ResourceLocation(MOD_ID, SHADER_LOCATION_PREFIX + "fragment_base.fsh");

    /**Changes the saturation of the texture based on the <tt>saturation</tt> uniform*/
    public static final ResourceLocation SATURATION = new ResourceLocation(MOD_ID, "saturation");

    @SubscribeEvent
    public static void onShaderRegistry(ShaderRegistryEvent event) {
        event.registerFragmentShader(SATURATION);
        event.registerFragmentShader(MWShaders.ROUNDISH);
    }

}
