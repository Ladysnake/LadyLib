package ladylib.modwinder;

import ladylib.LadyLib;
import ladylib.modwinder.data.ModWinderList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

@Mod(modid = ModWinder.MOD_ID, version = "1.0", dependencies = LadyLib.DEPENDENCIES)
public class ModWinder {

    public static final String MOD_ID = "modwinder";
    public static final String MOD_NAME = "ModWinder";
    public static final Logger LOGGER = LogManager.getLogger(LadyLib.MOD_NAME + "/" + MOD_NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        refillModBar();
    }

    /**
     * Retrieves the list of mods featured on <a href=https://ladysnake.glitch.me/milksnake-bar>Ladysnake's website</a>
     * and processes it.
     * <p>
     * This process is asynchronous, as such the method should return instantly even though the whole process is likely to
     * take several seconds.
     */
    private static void refillModBar() {
        try {
            ModWinderList.retrieveList(new URL("https://ladysnake.glitch.me/milksnake-bar-v2")).thenAccept(map -> map.forEach(ModWinderList::addList));
        } catch (MalformedURLException e) {
            throw new AssertionError(e); // Oi this is hardcoded
        }
    }

}
