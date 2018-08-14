package ladysnake.milksnake;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = MilkSnake.MOD_ID)
@Mod.EventBusSubscriber(modid = MilkSnake.MOD_ID)
public class MilkSnakeConfig {
    private MilkSnakeConfig() { }

    public static boolean enableResourcePack = false;

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(MilkSnake.MOD_ID)) {
            ConfigManager.sync(MilkSnake.MOD_ID, Config.Type.INSTANCE);
        }
    }
}
