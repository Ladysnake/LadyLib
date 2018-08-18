package ladysnake.milksnake;

import ladylib.LadyLib;
import ladylib.client.ResourceProxy;
import ladylib.modwinder.ModWinder;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Milksnake.MOD_ID, version = "@VERSION@", dependencies = "required-after:ladylib;required-after:forge@[14.23.3.2665,)", clientSideOnly = true, certificateFingerprint = "@FINGERPRINT@")
public class Milksnake {
    public static final String MOD_ID = "milksnake";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private ResourceProxy resourceProxy;

    @Mod.EventHandler
    public void construction(FMLConstructionEvent event) {
        resourceProxy = new MSResourceProxy("minecraft", ModWinder.MOD_ID);
        resourceProxy.hook();
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (MilksnakeConfig.enableResourcePack) {
            resourceProxy.addResourceOverride(MOD_ID, "minecraft","textures/gui", "options_background.png", "widgets.png", "title/edition.png", "title/minecraft.png");
            resourceProxy.addResourceOverride(MOD_ID, ModWinder.MOD_ID, "textures/gui", "modbar_widget.png");
        }
    }

    @Mod.EventHandler
    public void onFingerprintViolation(FMLFingerprintViolationEvent event) {
        if (LadyLib.isDevEnv()) {
            LOGGER.info("Ignoring invalid fingerprint as we are in a development environment");
        } else {
            LOGGER.warn("Invalid fingerprint detected! The file " + event.getSource().getName() + " may have been tampered with. This version will NOT be supported by the author!");
        }
    }
}
