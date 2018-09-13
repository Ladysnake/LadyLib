package ladylib.modwinder.data;

import com.google.common.collect.ImmutableList;
import com.google.gson.reflect.TypeToken;
import ladylib.modwinder.ModWinder;
import ladylib.modwinder.ModsFetchedEvent;
import ladylib.networking.http.HTTPRequestHelper;
import net.minecraftforge.common.MinecraftForge;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ModWinderList {
    public static final String LADYSNAKE = "ladysnake";
    public static final ModWinderList ALL = new ModWinderList("all", ImmutableList.of());
    private static final TreeMap<String, ModWinderList> ALL_LISTS = new TreeMap<>();

    static {
        ALL_LISTS.put("all", ALL);
    }

    private ImmutableList<ModEntry> list;
    private final String name;

    private ModWinderList(String name, List<ModEntry> list) {
        this.name = name;
        this.list = ImmutableList.copyOf(list);
    }

    /**
     * @return a list of mod entries gathered during {@link #retrieveList(URL)}
     */
    public ImmutableList<ModEntry> getModEntries() {
        return list;
    }

    public String getUnlocalizedName() {
        return "modwinder.list." + this.getName().toLowerCase(Locale.ENGLISH);
    }

    public String getName() {
        return name;
    }

    public static void addList(String name, List<ModEntry> list) {
        ALL_LISTS.put(name, new ModWinderList(name, list));
        ALL.list = ALL_LISTS.values().stream().flatMap(mwl -> mwl.list.stream()).distinct().collect(ImmutableList.toImmutableList());
    }

    public static ModWinderList getList(String name) {
        return ALL_LISTS.get(name);
    }

    public static ModWinderList getNext(ModWinderList list) {
        Map.Entry<String, ModWinderList> entry = ALL_LISTS.higherEntry(list.name);
        if (entry == null) {
            entry = ALL_LISTS.firstEntry();
        }
        return entry.getValue();
    }

    /**
     * Retrieves a list of mods available on a website under JSON format and processes it.
     */
    public static CompletableFuture<Map<String, List<ModEntry>>> retrieveList(URL url) {
        return HTTPRequestHelper.getJSON(url).thenApply(json -> {
            final Type type = new TypeToken<Map<String, List<ModEntry>>>() {}.getType();
            final Map<String, List<ModEntry>> retrieved = ModEntry.GSON.fromJson(json, type);
            MinecraftForge.EVENT_BUS.post(new ModsFetchedEvent(url, retrieved));
            return retrieved;
        }).exceptionally(t -> {
            ModWinder.LOGGER.warn("Could not create the list of Ladysnake mods", t);
            return Collections.emptyMap();
        });
    }
}
