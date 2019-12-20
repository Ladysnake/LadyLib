package ladylib.modwinder;

import ladylib.LadyLib;
import ladylib.modwinder.data.ModWinderList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

@Mod(modid = ModWinder.MOD_ID, version = "1.1", dependencies = LadyLib.DEPENDENCIES)
public class ModWinder {

    public static final String MOD_ID = "modwinder";
    public static final String MOD_NAME = "ModWinder";
    public static final Logger LOGGER = LogManager.getLogger(LadyLib.MOD_NAME + "/" + MOD_NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        refillModBar();
    }

    /**
     * Retrieves the list of mods featured on <a href=https://github.com/Ladysnake/ModWinder>Ladysnake's github repo</a>
     * and processes it.
     * <p>
     * This process is asynchronous, as such the method should return instantly even though the whole process is likely to
     * take several seconds.
     */
    private static void refillModBar() {
        //This will send a report to the console on whether or not the list was loaded.
        boolean foundList = false;
        try {
            ModWinderList.retrieveList(new URL("https://raw.githubusercontent.com/Ladysnake/ModWinder/master/milksnakebar.json")).thenAccept(map -> map.forEach(ModWinderList::addList));
            foundList = true;
        } catch (MalformedURLException e) {
            throw new AssertionError(e); // Oi this is hardcoded
        }
        if (!foundList) {
            LadyLib.LOGGER.warn("Error while fetching mod list! ModWinder was unable to access the JSON file. Are you connected to the internet? If you are, send an issue on sschr15's ModWinder page at https://github.com/sschr15/ModWinder");
        }
    }

}
