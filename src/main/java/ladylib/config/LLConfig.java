package ladylib.config;

import ladylib.LadyLib;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = LadyLib.MOD_ID)
@Mod.EventBusSubscriber(modid = LadyLib.MOD_ID)
public class LLConfig {
    @Config.Comment("The maximum amount of particles that should be displayed on the screen at the same time")
    public static int maxParticles = 300;

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(LadyLib.MOD_ID)) {
            ConfigManager.sync(LadyLib.MOD_ID, Config.Type.INSTANCE);
        }
    }
}
