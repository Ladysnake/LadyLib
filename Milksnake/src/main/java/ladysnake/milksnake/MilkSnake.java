package ladysnake.milksnake;

import ladylib.LLibContainer;
import ladylib.LadyLib;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = MilkSnake.MOD_ID, version = "@VERSION@", dependencies = "required-after:ladylib;", clientSideOnly = true, certificateFingerprint = "@FINGERPRINT@")
public class MilkSnake {
    public static final String MOD_ID = "milksnake";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @LadyLib.LLInstance
    private static LLibContainer libContainer;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (MilkSnakeConfig.enableResourcePack) {
            libContainer.addVanillaResourceOverride("textures/gui", "options_background.png", "widgets.png", "title/edition.png", "title/minecraft.png");
        }
    }
}
