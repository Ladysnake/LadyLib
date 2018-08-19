package ladylib.modwinder.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.reflect.TypeToken;
import ladylib.modwinder.ModWinder;
import ladylib.modwinder.ModsFetchedEvent;
import ladylib.networking.http.HTTPRequestHelper;
import net.minecraftforge.common.MinecraftForge;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public enum ModWinderLists {
    ALL {
        @Override
        public CompletableFuture<ImmutableList<ModEntry>> load() {
            return Arrays.stream(ModWinderLists.values())
                    .filter(l -> l != this)
                    .map(ModWinderLists::load)
                    .reduce((c1, c2) -> c1.thenCombine(c2, (l1, l2) -> ImmutableSet.<ModEntry>builder().addAll(l1).addAll(l2).build().asList()))
                    .orElse(CompletableFuture.completedFuture(ImmutableList.of()))
                    .thenApply(l -> list = ImmutableList.copyOf(new LinkedHashSet<>(l)));       // remove duplicates
        }
    },
    LADYSNAKE_APPROVED("https://ladysnake.glitch.me/milksnake-bar"),
    OTHERS("https://ladysnake.glitch.me/");

    private final URL url;
    protected ImmutableList<ModEntry> list = ImmutableList.of();

    ModWinderLists() {
        this.url = null;
    }

    ModWinderLists(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL", e);
        }
    }

    public CompletableFuture<ImmutableList<ModEntry>> load() {
        return ModWinderLists.retrieveList(getListUrl()).thenApply(ImmutableList::copyOf).thenApply(l -> this.list = l);
    }

    public URL getListUrl() {
        return this.url;
    }

    /**
     * @return a list of mod entries gathered during {@link #load()}
     */
    public ImmutableList<ModEntry> getModEntries() {
        return list;
    }

    public String getUnlocalizedName() {
        return "modwinder.list." + this.toString().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Retrieves a list of mods available on a website under JSON format and processes it.
     */
    public static CompletableFuture<List<ModEntry>> retrieveList(URL url) {
        return HTTPRequestHelper.getJSON(url).thenApply(json -> {
            final Type type = new TypeToken<List<ModEntry>>() {}.getType();
            final List<ModEntry> retrieved = ModEntry.GSON.fromJson(json, type);
            retrieved.forEach(ModEntry::init);
            MinecraftForge.EVENT_BUS.post(new ModsFetchedEvent(url, retrieved));
            return retrieved;
        }).exceptionally(t -> {
            ModWinder.LOGGER.warn("Could not create the list of Ladysnake mods", t);
            return Collections.emptyList();
        });
    }
}
