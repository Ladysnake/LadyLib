package ladylib.installer;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = "modwinder")
public class ModWinder {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModEntry.searchLadysnakeMods();
    }

}
