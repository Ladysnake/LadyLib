package ladylib.modwinder;

import ladylib.modwinder.data.ModEntry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * This event is fired when the list of available mod entries for ModWinder's mod bar
 * has been retrieved from the remote API.
 * <p>
 * <em>Note: This event will not be fired on the main thread !</em>
 * Use scheduled tasks if you need to interact with the latter.
 * <br>
 * This event is not {@link Cancelable}.<br>
 * <br>
 * This event does not have a result. {@link HasResult}<br>
 * <br>
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
 */
public class ModsFetchedEvent extends Event {
    private final Map<String, List<ModEntry>> retrievedMods;
    private final URL listUrl;

    public ModsFetchedEvent(URL listUrl, Map<String, List<ModEntry>> retrievedMods) {
        this.listUrl = listUrl;
        this.retrievedMods = retrievedMods;
    }

    public Stream<ModEntry> readRetrievedMods(String listName) {
        return retrievedMods.get(listName).stream();
    }

    public void addModEntry(String listName, ModEntry entry) {
        this.retrievedMods.get(listName).add(entry);
    }

    public Stream<String> getListNames() {
        return retrievedMods.keySet().stream();
    }

    /**
     * The URL at which the mod list was retrieved
     */
    public URL getListUrl() {
        return listUrl;
    }
}
