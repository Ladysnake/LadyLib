package ladylib.installer;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = ModWinder.MOD_ID)
public class ModWinder {

    public static final String MOD_ID = "modwinder";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModEntry.searchLadysnakeMods();
    }

}
