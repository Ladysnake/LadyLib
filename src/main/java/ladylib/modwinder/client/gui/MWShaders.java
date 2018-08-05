package ladylib.modwinder.client.gui;

import ladylib.client.shader.ShaderRegistryEvent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import static ladylib.modwinder.ModWinder.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, value = Side.CLIENT)
public final class MWShaders {
    public static final ResourceLocation ROUNDISH = new ResourceLocation(MOD_ID, "roundish");

    @SubscribeEvent
    public static void onShaderRegistry(ShaderRegistryEvent event) {
        event.registerFragmentShader(MWShaders.ROUNDISH);
    }

    private MWShaders() {
        super();
    }
}
