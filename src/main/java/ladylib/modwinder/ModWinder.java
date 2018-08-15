package ladylib.modwinder;

import ladylib.LadyLib;
import ladylib.modwinder.data.ModEntry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = ModWinder.MOD_ID, version = "1.0")
public class ModWinder {

    public static final String MOD_ID = "modwinder";
    public static final String MOD_NAME = "ModWinder";
    public static final Logger LOGGER = LogManager.getLogger(LadyLib.MOD_NAME + "/" + MOD_NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModEntry.refillModBar();
    }

}
