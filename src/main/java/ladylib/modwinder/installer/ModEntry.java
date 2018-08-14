package ladylib.modwinder.installer;

import com.google.common.collect.ImmutableList;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import ladylib.modwinder.ModWinder;
import ladylib.modwinder.ModsFetchedEvent;
import ladylib.networking.http.HTTPRequestException;
import ladylib.networking.http.HTTPRequestHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.versioning.ComparableVersion;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.libraries.Artifact;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModEntry {
    public static final String MOD_BAR_URL = "https://ladysnake.glitch.me/milksnake-bar";

    static final Gson GSON = new GsonBuilder().setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    private static ImmutableList<ModEntry> ladysnakeMods = ImmutableList.of();

    /**
     * Retrieves the list of mods featured on <a href=http://ladysnake.glitch.me/data/milksnake-bar.json>Ladysnake's website</a>
     * and processes it.
     * <p>
     * This process is asynchronous, as such the method should return instantly even though the whole process is likely to
     * take several seconds.
     */
    public static void refillModBar() {
        HTTPRequestHelper.getJSON(MOD_BAR_URL, json -> {
            try {
                final Type type = new TypeToken<List<ModEntry>>() {}.getType();
                final List<ModEntry> retrieved = GSON.fromJson(json, type);
                retrieved.forEach(ModEntry::init);
                MinecraftForge.EVENT_BUS.post(new ModsFetchedEvent(retrieved));
                ladysnakeMods = ImmutableList.copyOf(retrieved);
            } catch (Exception e) {
                ModWinder.LOGGER.warn("Could not create the list of Ladysnake mods", e);
            }
        });
    }

    /**
     * @return a list of mod entries gathered during {@link #refillModBar()}
     */
    public static ImmutableList<ModEntry> getLadysnakeMods() {
        return ladysnakeMods;
    }

    @SerializedName("modid")
    private String modId;
    @SerializedName("curseid")
    private int curseId;
    private String name;
    private String author;
    private URL updateUrl;
    private List<ModEntry> dlcs;

    // the following variables are calculated from the serialized ones

    /**
     * Set to true after {@link #init(ModEntry)} has been called once
     */
    private transient boolean initialized;
    private transient boolean installed;
    private transient boolean outdated;
    private transient InstallationState installationState = InstallationState.NAUGHT;
    private transient String installedVersion = "";
    private transient String latestVersion = "";
    private transient ResourceLocation logoTexture;
    private transient Artifact localArtifact;
    /**The list of mods this entry is a DLC of*/
    private transient Set<ModEntry> parents = new HashSet<>();
    private Map<ComparableVersion, String> changelog;

    private ModEntry() {
        super();
        this.dlcs = Collections.emptyList();
        // gets a default logo while the right one is loaded
        setLogo(null);
    }

    public ModEntry(String modId, int curseId, String name, String author, URL updateUrl, List<ModEntry> dlcs) {
        this();
        this.modId = modId;
        this.curseId = curseId;
        this.name = name;
        this.author = author;
        this.updateUrl = updateUrl;
        this.dlcs = dlcs;
        init();
    }

    protected synchronized void init(ModEntry parent) {
        this.parents.add(parent);
        init();
    }

    protected synchronized void init() {
        if (initialized) {
            return;
        }
        ModContainer installedMod = Loader.instance().getIndexedModList().get(modId);
        this.localArtifact = AddonInstaller.LOCAL_MODS.getArtifact(this, AddonInstaller.MOD_LIST.getRepository());
        if (installedMod != null) {
            this.installed = true;
            this.installedVersion = installedMod.getVersion();
        } else {
            this.installedVersion = AddonInstaller.LOCAL_MODS.get(this.modId).map(LocalModList.LocalModEntry::getVersion).orElse("");
        }
        // get the logo
        getLogo:
        try {
            if (FMLCommonHandler.instance().getSide() == Side.SERVER) {
                break getLogo;
            }
            URL curseApi = new URL("https://curse.nikky.moe/api/addon/" + curseId);
            HTTPRequestHelper.getJSON(curseApi)
                    .thenApply(json -> {
                        // get the logo's url
                        for (JsonElement attachment : json.getAsJsonObject().get("attachments").getAsJsonArray()) {
                            if (attachment.getAsJsonObject().get("default").getAsBoolean()) {
                                return GSON.fromJson(attachment.getAsJsonObject().get("url"), URL.class);
                            }
                        }
                        throw new HTTPRequestException("No logo found for project " + json.getAsJsonObject().get("name"));
                    })
                    .thenAccept(logo -> {
                        try {
                            // create a texture from the url
                            setLogo(ImageIO.read(logo));
                        } catch (IOException e) {
                            throw new CompletionException(e);
                        }
                    }).exceptionally(t -> {
                ModWinder.LOGGER.error("Could not download logo", t);
                return null;
            });
        } catch (MalformedURLException e) {
            ModWinder.LOGGER.error("Invalid curse project id " + curseId, e);
        }
        if (this.latestVersion.isEmpty()) {
            // Forge's version check does not have a callback so it is easier to just check ourselves even if the mod is installed
            HTTPRequestHelper.getJSON(this.getUpdateUrl())
                    .thenAccept(json -> {
                        String latest = json.getAsJsonObject()
                                .get("promos").getAsJsonObject()
                                .get(MinecraftForge.MC_VERSION + "-latest").getAsString();
                        this.setLatestVersion(latest);
                        outdated = (new ComparableVersion(this.installedVersion).compareTo(new ComparableVersion(latest)) < 0);
                        Map<String, String> temp = GSON.fromJson(json.getAsJsonObject().get(MinecraftForge.MC_VERSION + ""), new TypeToken<Map<String, String>>() {
                        }.getType());
                        this.setChangelog(temp.keySet().stream().collect(Collectors.toMap(ComparableVersion::new, temp::get)));
                    });
        }
        this.initialized = true;
        this.dlcs.forEach(modEntry -> modEntry.init(this));
    }

    public synchronized Map<ComparableVersion, String> getChangelog() {
        return changelog;
    }

    public synchronized void setChangelog(Map<ComparableVersion, String> changelog) {
        this.changelog = changelog;
    }

    @Nullable
    public synchronized Artifact getLocalArtifact() {
        return localArtifact;
    }

    public synchronized void setLocalArtifact(@Nullable Artifact localArtifact) {
        this.localArtifact = localArtifact;
    }

    public boolean isDlc() {
        return !parents.isEmpty();
    }

    public Stream<ModEntry> getParentMods() {
        return this.parents.stream();
    }

    public boolean isOutdated() {
        return outdated;
    }

    public String getModId() {
        return modId;
    }

    public int getCurseId() {
        return curseId;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public URL getUpdateUrl() {
        return updateUrl;
    }

    public List<ModEntry> getDlcs() {
        return ImmutableList.copyOf(dlcs);
    }

    public boolean isInstalled() {
        return installed;
    }

    public String getInstalledVersion() {
        return installedVersion;
    }

    public synchronized String getLatestVersion() {
        return latestVersion;
    }

    public synchronized void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
    }

    public synchronized InstallationState getInstallationState() {
        return installationState;
    }

    public synchronized void setInstallationState(InstallationState installationState) {
        this.installationState = installationState;
    }

    public ResourceLocation getLogo() {
        return logoTexture;
    }

    public void setLogo(@Nullable BufferedImage logo) {
        // don't try to call graphic methods on a dedicated server
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            // run in the client thread for the OpenGL context
            // note that addScheduledTask synchronizes concurrent calls
            Minecraft.getMinecraft().addScheduledTask(() -> {
                DynamicTexture texture = logo == null ? TextureUtil.MISSING_TEXTURE : new DynamicTexture(logo);
                this.logoTexture = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("curse-logo-" + getModId(), texture);
            });
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModEntry modEntry = (ModEntry) o;
        return Objects.equals(modId, modEntry.modId) &&
                Objects.equals(curseId, modEntry.curseId) &&
                Objects.equals(name, modEntry.name) &&
                Objects.equals(updateUrl, modEntry.updateUrl) &&
                Objects.equals(dlcs, modEntry.dlcs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modId, curseId, name, updateUrl, dlcs);
    }

    @Override
    public String toString() {
        return "ModEntry{" +
                "modId='" + modId + '\'' +
                ", curseId='" + curseId + '\'' +
                ", name='" + name + '\'' +
                ", updateUrl='" + updateUrl + '\'' +
                ", dlcs=" + dlcs +
                ", installed=" + installed +
                ", installedVersion=" + installedVersion +
                ", latestVersion=" + latestVersion +
                '}';
    }
}
