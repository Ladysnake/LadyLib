package ladylib.modwinder.data;

import com.google.common.collect.ImmutableList;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import ladylib.modwinder.ModWinder;
import ladylib.modwinder.client.gui.GuiModBar;
import ladylib.modwinder.installer.AddonInstaller;
import ladylib.modwinder.installer.InstallationState;
import ladylib.networking.http.HTTPRequestException;
import ladylib.networking.http.HTTPRequestHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.versioning.ComparableVersion;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.libraries.Artifact;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents information about a mod that is required to display it in {@link GuiModBar} or to manage its installation
 */
public class ModEntry {

    public static final Gson GSON = new GsonBuilder()
            .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapterFactory(new ModEntryTypeAdapterFactory())
            .create();

    /**@see Mod#modid()*/
    @SerializedName("modid")
    private String modId;
    /**The curse project id for this mod*/
    @SerializedName("curseid")
    private int curseId;
    /**A display name for this mod entry*/
    private String name;
    /**The author of the mod*/
    private String author;
    /**@see Mod#updateJSON()*/
    @Nullable private URL updateUrl;
    /**Downloadable content for this mod*/
    private List<ModEntry> dlcs;

    // the following variables are calculated from the serialized ones

    /**Set to true after {@link #init()} has been called once*/
    private transient boolean initialized;
    /**True if the mod is active on the current minecraft instance*/
    private transient boolean installed;
    /**True if the mod is installed and its installed version is less than the latest version*/
    private transient boolean outdated;
    /**The current installation state of the mod. Installation is triggered by user commands*/
    private transient InstallationState installationState = InstallationState.NAUGHT;
    /**The version declared by the currently installed mod container*/
    private transient String installedVersion = "";
    /**The latest version declared by the mod's update JSON*/
    private transient String latestVersion = "";
    /**The location of the dynamic texture used to draw the mod's logo*/
    private transient ResourceLocation logoTexture;
    /**The local artifact from {@link AddonInstaller#LOCAL_MODS}. Can exist even if the mod is not installed*/
    private transient Artifact localArtifact;
    /**The list of mods this entry is a DLC of*/
    private transient Set<ModEntry> parents = new HashSet<>();
    /**The changelog as declared in the mod's update json*/
    private transient Map<ComparableVersion, String> changelog;

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

    private synchronized void init(ModEntry parent) {
        this.parents.add(parent);
        init();
    }

    protected synchronized void init() {
        if (this.initialized) {
            return;
        }
        this.setLocalArtifact(AddonInstaller.LOCAL_MODS.getArtifact(this, AddonInstaller.MOD_LIST.getRepository()));
        this.checkInstall();
        this.fetchLogo();
        this.fetchUpdateJson();
        this.initialized = true;
        // recursively initialize child entries (DLCs)
        this.dlcs.forEach(modEntry -> modEntry.init(this));
    }

    /**
     * Checks if the mod represented by this entry is currently installed on this Minecraft instance and updates
     * relevant fields if it is the case
     */
    private void checkInstall() {
        ModContainer installedMod = Loader.instance().getIndexedModList().get(modId);
        if (installedMod != null) {
            this.installed = true;
            this.installedVersion = installedMod.getVersion();
        } else {
            this.installedVersion = AddonInstaller.LOCAL_MODS.get(this.modId).map(LocalModList.LocalModEntry::getVersion).orElse("");
        }
    }

    /**
     * Fetches the curse avatar for this mod's project page and sets the logo texture
     */
    private void fetchLogo() {
        if (FMLCommonHandler.instance().getSide() == Side.SERVER) {
            return;
        }
        URL curseApi;
        try {
            curseApi = new URL("https://curse.nikky.moe/api/addon/" + curseId);
        } catch (MalformedURLException e) {
            throw new HTTPRequestException("Invalid curse project id " + curseId, e);
        }
        HTTPRequestHelper.getJSON(curseApi)
                .thenApply(json -> {
                    // get the logo's url
                    for (JsonElement attachment : json.getAsJsonObject().get("attachments").getAsJsonArray()) {
                        if (attachment.getAsJsonObject().get("isDefault").getAsBoolean()) {
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
    }

    /**
     * Fetches the update json from the given update URL and updates relevant fields
     */
    private void fetchUpdateJson() {
        if (this.latestVersion.isEmpty() && this.getUpdateUrl() != null) {
            // Forge's version check does not have a callback so it is easier to just check ourselves even if the mod is installed
            HTTPRequestHelper.getJSON(this.getUpdateUrl())
                    .thenAccept(json -> {
                        String latest = json.getAsJsonObject()
                                .get("promos").getAsJsonObject()
                                .get(MinecraftForge.MC_VERSION + "-latest").getAsString();
                        this.setLatestVersion(latest);
                        outdated = installed && (new ComparableVersion(this.installedVersion).compareTo(new ComparableVersion(latest)) < 0);
                        Map<String, String> temp = GSON.fromJson(json.getAsJsonObject().get(MinecraftForge.MC_VERSION + ""), new TypeToken<Map<String, String>>() {
                        }.getType());
                        this.setChangelog(temp.keySet().stream().collect(Collectors.toMap(ComparableVersion::new, temp::get)));
                    });
        }
    }

    @Nullable
    public synchronized Map<ComparableVersion, String> getChangelog() {
        return changelog;
    }

    private synchronized void setChangelog(Map<ComparableVersion, String> changelog) {
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

    @Nullable
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
        return name + "(" + modId + ") {" +
                (installed ? "installed," : "") +
                (outdated ? "outdated," : "") +
                "curseId='" + curseId + '\'' +
                ", installedVersion=" + installedVersion +
                ", latestVersion=" + latestVersion +
                '}';
    }
}
