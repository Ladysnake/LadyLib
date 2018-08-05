package ladylib.client.shader;

import ladylib.modwinder.client.gui.MWShaders;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import static ladylib.LadyLib.MOD_ID;
import static ladylib.client.shader.ShaderUtil.SHADER_LOCATION_PREFIX;

@Mod.EventBusSubscriber(modid = MOD_ID, value = Side.CLIENT)
public final class BaseShaders {

    public static final ResourceLocation BASE_VERTEX = new ResourceLocation(MOD_ID, SHADER_LOCATION_PREFIX + "vertex_base.vsh");
    public static final ResourceLocation BASE_FRAGMENT = new ResourceLocation(MOD_ID, SHADER_LOCATION_PREFIX + "fragment_base.fsh");

    public static final ResourceLocation GREYSCALE = new ResourceLocation(MOD_ID, "greyscale");

    @SubscribeEvent
    public static void onShaderRegistry(ShaderRegistryEvent event) {
        event.registerFragmentShader(GREYSCALE);
        event.registerFragmentShader(MWShaders.ROUNDISH);
    }

    private BaseShaders() {
        super();
    }
}
