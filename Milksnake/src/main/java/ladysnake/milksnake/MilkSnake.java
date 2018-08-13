package ladysnake.milksnake;

import ladylib.LLibContainer;
import ladylib.LadyLib;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
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

    @Mod.EventHandler
    public void onFingerprintViolation(FMLFingerprintViolationEvent event) {
        if (LadyLib.isDevEnv())
            LOGGER.info("Ignoring invalid fingerprint as we are in a development environment");
        else
            LOGGER.warn("Invalid fingerprint detected! The file " + event.getSource().getName() + " may have been tampered with. This version will NOT be supported by the author!");
    }
}
