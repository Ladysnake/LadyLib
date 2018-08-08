package ladylib.modwinder;

import ladylib.modwinder.installer.ModEntry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = ModWinder.MOD_ID)
public class ModWinder {

    public static final String MOD_ID = "modwinder";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModEntry.refillModBar();
    }

}
