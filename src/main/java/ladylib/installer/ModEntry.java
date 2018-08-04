package ladylib.installer;

import com.google.common.collect.ImmutableList;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import ladylib.LadyLib;
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

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class ModEntry {
    private static final String LADYSNAKE_MODS = "https://gist.githubusercontent.com/Pyrofab/000073b92e7a9de9f68b4da685ff80c5/raw/6f49c492c0d8879749175455d191a012a7e4431c/ladysnake_mods_test.json";
    private static final Gson GSON = new GsonBuilder().setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    private static List<ModEntry> ladysnakeMods;

    public static void searchLadysnakeMods() {
        HTTPRequestHelper.getJSON(LADYSNAKE_MODS, json -> {
            try {
                Type type = new TypeToken<List<ModEntry>>() {
                }.getType();
                ladysnakeMods = GSON.fromJson(json, type);
                ladysnakeMods.forEach(modEntry -> modEntry.init(false));
            } catch (Exception e) {
                LadyLib.LOGGER.warn("Could not create the list of Ladysnake mods", e);
            }
        });
    }

    public static List<ModEntry> getLadysnakeMods() {
        return ladysnakeMods == null ? ImmutableList.of() : ImmutableList.copyOf(ladysnakeMods);
    }

    private String modid;
    private int curseid;
    private String name;
    private URL updateUrl;
    private List<ModEntry> dlcs;

    // the following variables are calculated from the serialized ones
    private transient boolean isDlc;
    private transient boolean installed;
    private transient boolean outdated;
    private transient InstallationState installationState = InstallationState.NAUGHT;
    private transient String installedVersion = "";
    private transient String latestVersion = "";
    private transient ResourceLocation logo;
    private Map<ComparableVersion, String> changelog;

    private ModEntry() {
        super();
        this.dlcs = Collections.emptyList();
    }

    public ModEntry(String modid, int curseid, String name, URL updateUrl, List<ModEntry> dlcs) {
        this.modid = modid;
        this.curseid = curseid;
        this.name = name;
        this.updateUrl = updateUrl;
        this.dlcs = dlcs;
        init(false);
    }

    {
        // initializer, run in every constructor
        // gets a default logo while the right one is loaded
        setLogo(null);
    }

    protected void init(boolean isDlc) {
        this.isDlc = isDlc;
        ModContainer installedMod = Loader.instance().getIndexedModList().get(modid);
        if (installedMod != null) {
            this.installed = true;
            this.installedVersion = installedMod.getVersion();
            // reuse Forge update checker's result if possible
            /*ForgeVersion.CheckResult checkResult = ForgeVersion.getResult(installedMod);
            boolean successful = checkResult.status != ForgeVersion.Status.PENDING && checkResult.status != ForgeVersion.Status.FAILED;
            if (successful) {
                this.changelog = checkResult.changes;
            }
            if (checkResult.target != null) {
                this.latestVersion = checkResult.target.toString();
                this.outdated = true;
            } else if (successful) {
                // Forge's check has been completed successfully and no newer version has been found
                this.latestVersion = this.installedVersion;
            }*/

        }
        // get the logo
        getLogo:
        try {
            if (FMLCommonHandler.instance().getSide() == Side.SERVER) {
                break getLogo;
            }
            URL curseApi = new URL("https://curse.nikky.moe/api/addon/" + curseid);
            HTTPRequestHelper.getJSON(curseApi)
                    .thenApply(json -> {
                        // get the logo's url
                        for (JsonElement attachment : json.getAsJsonObject().get("attachments").getAsJsonArray()) {
                            if (attachment.getAsJsonObject().get("default").getAsBoolean()) {
                                return GSON.fromJson(attachment.getAsJsonObject().get("url"), URL.class);
                            }
                        }
                        throw new RuntimeException("No logo found for project " + json.getAsJsonObject().get("name"));
                    })
                    .thenAccept(logo -> {
                        try {
                            // create a texture from the url
                            setLogo(ImageIO.read(logo));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).exceptionally(t -> {
                LadyLib.LOGGER.error("Could not download logo", t);
                return null;
            });
        } catch (MalformedURLException e) {
            LadyLib.LOGGER.error("Invalid curse project id " + curseid, e);
        }
        if (this.latestVersion.isEmpty()) {
            // Either the mod is not installed or Forge's version check is too slow
            // Forge's version check does not have a callback so it is easier to just check ourselves
            HTTPRequestHelper.getJSON(this.getUpdateUrl())
                    .thenAccept(json -> {
                        String latestVersion = json.getAsJsonObject()
                                .get("promos").getAsJsonObject()
                                .get(MinecraftForge.MC_VERSION + "-latest").getAsString();
                        this.setLatestVersion(latestVersion);
                        outdated = (new ComparableVersion(this.installedVersion).compareTo(new ComparableVersion(latestVersion)) < 0);
                        Map<String, String> temp = GSON.fromJson(json.getAsJsonObject().get(MinecraftForge.MC_VERSION + ""), new TypeToken<Map<String, String>>() {}.getType());
                        this.setChangelog(temp.keySet().stream().collect(Collectors.toMap(ComparableVersion::new, temp::get, (s1, s2) -> s1, LinkedHashMap::new)));
                    });
        }
        this.dlcs.forEach(modEntry -> modEntry.init(true));

    }

    public synchronized Map<ComparableVersion, String> getChangelog() {
        return changelog;
    }

    public synchronized void setChangelog(Map<ComparableVersion, String> changelog) {
        this.changelog = changelog;
    }

    public boolean isDlc() {
        return isDlc;
    }

    public boolean isOutdated() {
        return outdated;
    }

    public String getModid() {
        return modid;
    }

    public int getCurseid() {
        return curseid;
    }

    public String getName() {
        return name;
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
        return logo;
    }

    public synchronized void setLogo(@Nullable BufferedImage logo) {
        // don't try to call graphic methods on a dedicated server
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            // run in the client thread for the OpenGL context
            Minecraft.getMinecraft().addScheduledTask(() -> {
                DynamicTexture texture = logo == null ? TextureUtil.MISSING_TEXTURE : new DynamicTexture(logo);
                this.logo = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("curse-logo-" + getModid(), texture);
            });
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModEntry modEntry = (ModEntry) o;
        return Objects.equals(modid, modEntry.modid) &&
                Objects.equals(curseid, modEntry.curseid) &&
                Objects.equals(name, modEntry.name) &&
                Objects.equals(updateUrl, modEntry.updateUrl) &&
                Objects.equals(dlcs, modEntry.dlcs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modid, curseid, name, updateUrl, dlcs);
    }

    @Override
    public String toString() {
        return "ModEntry{" +
                "modid='" + modid + '\'' +
                ", curseid='" + curseid + '\'' +
                ", name='" + name + '\'' +
                ", updateUrl='" + updateUrl + '\'' +
                ", dlcs=" + dlcs +
                ", installed=" + installed +
                ", installedVersion=" + installedVersion +
                ", latestVersion=" + latestVersion +
                '}';
    }
}
