package ladysnake.testmod.client.shader;

import ladylib.client.shader.PostProcessShader;
import ladylib.compat.EnhancedBusSubscriber;
import ladysnake.testmod.TestMod;
import net.minecraft.client.Minecraft;
import net.minecraft.init.MobEffects;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@EnhancedBusSubscriber(side = Side.CLIENT, owner = TestMod.MODID)
public class BitsShader {
    private ResourceLocation BITS_SHADER = new ResourceLocation("minecraft:shaders/post/bits.json");
    private PostProcessShader shader = PostProcessShader.loadShader(BITS_SHADER);

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (Minecraft.getMinecraft().player.isPotionActive(MobEffects.GLOWING)) {
            shader.render(event.getPartialTicks());
        }
    }
}
