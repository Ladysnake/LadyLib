package ladysnake.milksnake;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Locale;

@Config(modid = Milksnake.MOD_ID)
@Mod.EventBusSubscriber(modid = Milksnake.MOD_ID)
public class MilksnakeConfig {
    private MilksnakeConfig() { }

    public static boolean enableResourcePack = true;

    public static Flavours flavour = Flavours.BITTER_APPLE;

    public enum Flavours {
        BITTER_APPLE, CURACAO, ROSOLIO;


        @Override
        public String toString() {
            return super.toString().toLowerCase(Locale.ENGLISH);
        }
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(Milksnake.MOD_ID)) {
            ConfigManager.sync(Milksnake.MOD_ID, Config.Type.INSTANCE);
        }
    }
}
