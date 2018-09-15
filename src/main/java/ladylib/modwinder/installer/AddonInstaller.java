package ladylib.modwinder.installer;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ladylib.misc.ReflectionUtil;
import ladylib.modwinder.ModWinder;
import ladylib.modwinder.data.DummyModEntry;
import ladylib.modwinder.data.LocalModList;
import ladylib.modwinder.data.ModEntry;
import ladylib.modwinder.data.ModWinderList;
import ladylib.networking.http.HTTPRequestHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.libraries.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.message.FormattedMessage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This class offers methods to install and manage mods using Forge's {@link ModList} system.
 *
 * @see ModDeleter
 */
public class AddonInstaller {
    private AddonInstaller() {
    }

    public static final InstallationState DOWNLOAD_START = new InstallationState(InstallationState.Status.INSTALLING, "modwinder.status.downloading.start");
    public static final InstallationState DOWNLOAD_FAILED = new InstallationState(InstallationState.Status.FAILED, "modwinder.status.failed");
    public static final InstallationState INSTALLATION_COMPLETE = new InstallationState(InstallationState.Status.INSTALLED, "modwinder.status.installed", "modwinder.status.restart");
    public static final InstallationState INSTALLATION_END = new InstallationState(InstallationState.Status.INSTALLING, "modwinder.status.installing.end");
    public static final InstallationState UNINSTALLED = new InstallationState(InstallationState.Status.UNINSTALLED, "modwinder.status.uninstalled", "modwinder.status.restart");
    public static final InstallationState UNINSTALL_FAILED = new InstallationState(InstallationState.Status.FAILED, "modwinder.status.uninstalled.failed");

    /**
     * The list of every artifact available in the mods repository
     */
    public static final LocalModList LOCAL_MODS;
    /**
     * The list of every artifact loaded by Forge
     */
    public static final ModList MOD_LIST;

    /**
     * A single thread used to download and manage files
     */
    private static final Executor DOWNLOAD_THREAD = Executors.newSingleThreadExecutor(r -> new Thread(r, "Ladylib Installer"));
    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();
    private static final MethodHandle libraryManager$extractPacked = ReflectionUtil.findMethodHandleFromObfName(LibraryManager.class, "extractPacked", Pair.class, File.class, ModList.class, File[].class);

    static {
        Path modsPath = Paths.get("mods", MinecraftForge.MC_VERSION);
        LOCAL_MODS = LocalModList.create(modsPath.resolve("modwinder_local_mods.json"));
        File modsDir = modsPath.toFile();
        File modList = new File(modsDir, "mod_list.json");
        if (!modList.exists()) {
            if (!modsDir.exists() && !modsDir.mkdir()) {
                throw new IllegalStateException("No mods directory ?!");
            }
            Map<String, Object> baseList = new HashMap<>();
            baseList.put("repositoryRoot", "mods/1.12.2");
            try {
                Files.write(modList.toPath(), GSON.toJson(baseList).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new InstallationException("Could not write base mod list", e);
            }
        }
        MOD_LIST = ModList.create(modList, Launch.minecraftHome);
    }

    /**
     * Downloads and installs the latest file uploaded on Curseforge for the project associated with the given mod.
     * <p>
     * This method uses <a href="https://github.com/NikkyAI/CurseProxy/blob/master/README.md">NikkyAI's Curse API</a>
     * to obtain the latest file uploaded for the current Minecraft version. As such, <em>it is required that the passed in
     * mod entry has a valid Curseforge project ID.</em> It will then download that file
     * and install it using Forge's {@link ModList} system. Required project dependencies are installed recursively.
     * <p>
     * The returned {@link CompletableFuture} has as its result the list of every file that been successfully
     * downloaded during the operation.
     * Any exception thrown during the installation will be logged and update the mod entry's installation state,
     * notifying the user of the {@link InstallationState.Status#FAILED failure}.
     * <p>
     * The passed in mod entry will see its {@link ModEntry#getInstallationState() installation state}
     * updated adequately during the process.
     *
     * @param mod a mod entry containing at least the numeric id of the CF project
     * @return a future that can be used to trigger other tasks once the installation has ended
     */
    public static CompletableFuture<List<File>> installLatestFromCurseforge(ModEntry mod) {
        // local holder class for required values
        class ParamHolder {
            private String fileName;
            private String fileId;

            private ParamHolder(String fileName, String fileId) {
                this.fileName = fileName;
                this.fileId = fileId;
            }
        }
        URL apiUrl;
        try {
            apiUrl = new URL("https://curse.nikky.moe/api/addon/" + mod.getCurseId());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
        mod.setInstallationState(DOWNLOAD_START);
        return HTTPRequestHelper.getJSON(apiUrl)
                .thenApply(json -> {
                    // Identify the latest file for the current Minecraft Version
                    for (JsonElement el : json.getAsJsonObject().get("gameVersionLatestFiles").getAsJsonArray()) {
                        if (MinecraftForge.MC_VERSION.equals(el.getAsJsonObject().get("gameVersion").getAsString())) {
                            return new ParamHolder(
                                    el.getAsJsonObject().get("projectFileName").getAsString(),
                                    el.getAsJsonObject().get("projectFileID").getAsString()
                            );
                        }
                    }
                    throw new InstallationException("No file has been uploaded on project " + json.getAsJsonObject().get("webSiteURL") + " for version " + MinecraftForge.MC_VERSION);
                })
                .thenCompose(holder -> downloadFromCurseforge(mod, holder.fileName, holder.fileId))
                .thenApplyAsync(
                        list -> list.stream()
                                .map(CompletableFuture::join)
                                .flatMap(List::stream)
                                .collect(Collectors.toList()),
                        ForkJoinPool.commonPool()   // wait on another thread to avoid deadlock
                )
                .handle((result, t) -> {
                    if (t != null) {
                        ModWinder.LOGGER.error(new FormattedMessage("Could not download latest file of {} (project {})", mod.getName(), mod.getCurseId()), t);
                        mod.setInstallationState(DOWNLOAD_FAILED);
                        // rethrow the exception in case someone wants to do something else with it
                        Throwables.throwIfUnchecked(t);
                        throw new AssertionError(t);
                    } else {
                        mod.setInstallationState(INSTALLATION_COMPLETE);
                        return result;
                    }
                });
    }

    /**
     * Uninstalls the given mod by removing it from Forge's mod list. <br>
     * The mod file is not removed from the mods repository and can be reinstalled instantly using {@link #attemptReEnabling(ModEntry)}
     * <p>
     * If the target was not installed through the mod list system however, the deletion will be permanent (effective once the game exits)
     * </p>
     *
     * @param modEntry the entry of the mod to uninstall
     * @see #attemptReEnabling(ModEntry)
     */
    public static void uninstall(ModEntry modEntry) {
        try {
            for (ModEntry dlc : modEntry.getDlcs()) {
                uninstall(dlc);
            }
            // Hard uninstall
            if (LOCAL_MODS.getArtifact(modEntry, MOD_LIST.getRepository()) == null) {
                ModDeleter.scheduleModDeletion(modEntry.getModId());
                if (modEntry.isInstalled()) {
                    modEntry.setInstallationState(UNINSTALLED);
                } else {
                    modEntry.setInstallationState(InstallationState.NAUGHT);
                }
                return;
            }
            List<Artifact> artifacts = ReflectionHelper.getPrivateValue(ModList.class, MOD_LIST, "artifacts");
            Map<String, Artifact> art_map = ReflectionHelper.getPrivateValue(ModList.class, MOD_LIST, "art_map");
            artifacts.removeIf(artifact -> {
                if (artifact.matchesID(modEntry.getLocalArtifact())) {
                    art_map.remove(artifact.toString());
                    return true;
                }
                return false;
            });
            MOD_LIST.save();
            LOCAL_MODS.save();
            if (modEntry.isInstalled()) {
                modEntry.setInstallationState(UNINSTALLED);
            } else {
                modEntry.setInstallationState(InstallationState.NAUGHT);
            }
        } catch (Exception e) {
            ModWinder.LOGGER.error("Could not uninstall mod {} ({})", modEntry.getName(), modEntry.getModId(), e);
            modEntry.setInstallationState(UNINSTALL_FAILED);
        }
    }

    /**
     * Re-enables a mod that has been uninstalled but is still loaded in the minecraft instance
     *
     * @param modEntry the information regarding the mod to re-enable
     * @return true if the mod has been re-enabled, false if it needs to be installed from scratch
     * @see #uninstall(ModEntry)
     */
    public static boolean attemptReEnabling(ModEntry modEntry) {
        try {
            // if any of the parent mods is not up to date, just update everything
            if (modEntry.isDlc() && !modEntry.getParentMods().allMatch(AddonInstaller::attemptReEnabling)) {
                return false;
            }
            Artifact disabled = modEntry.getLocalArtifact();
            if (disabled == null) {
                if (Loader.isModLoaded(modEntry.getModId()) && !modEntry.isOutdated()) {
                    // mod is already loaded and up to date, just remove the current indication
                    ModDeleter.cancelModDeletion(modEntry.getModId());
                    modEntry.setInstallationState(InstallationState.NAUGHT);
                    return true;
                }
                return false;
            }
            MOD_LIST.add(disabled);
            MOD_LIST.save();
            if (Loader.isModLoaded(modEntry.getModId()) && !modEntry.isOutdated()) {
                // mod is already loaded and up to date, just remove the current indication
                modEntry.setInstallationState(InstallationState.NAUGHT);
            } else {
                // instantly complete the installation
                modEntry.setInstallationState(INSTALLATION_COMPLETE);
                // In case someone installed an old version manually after disabling the automatic one, people are weird
                ModDeleter.scheduleModDeletion(modEntry.getModId());
            }
            return true;
        } catch (Exception e) {
            ModWinder.LOGGER.error("Could not re-enable mod {}", modEntry.getModId(), e);
        }
        return false;
    }

    private static CompletableFuture<List<CompletableFuture<List<File>>>> downloadFromCurseforge(final ModEntry mod, String fileName, @Nullable String fileId) {
        // get the project information from the public API
        try {
            return HTTPRequestHelper.getJSON(new URL("https://curse.nikky.moe/api/addon/" + mod.getCurseId() + "/files"))
                    .thenApplyAsync(json -> {
                        // filter the files based on the name
                        JsonObject fileToDownload = null;
                        for (JsonElement el : json.getAsJsonArray()) {
                            String fileNameOnDisk = el.getAsJsonObject().get("fileNameOnDisk").getAsString();
                            if (fileNameOnDisk.equals(fileName) || el.getAsJsonObject().get("id").getAsString().equals(fileId)) {
                                fileToDownload = el.getAsJsonObject();
                                break;
                            }
                        }
                        if (fileToDownload == null) {
                            throw new InstallationException("Could not find file " + fileName + " in project " + mod.getCurseId());
                        }
                        // A list of every download triggered by this request
                        // The queried file is downloaded immediately but dependencies will be downloaded later
                        List<CompletableFuture<List<File>>> downloadedFiles = new ArrayList<>();

                        installDependencies(fileToDownload, downloadedFiles);

                        // download this file from the associated url
                        String downloadURL = fileToDownload.getAsJsonObject().get("downloadURL").getAsString();
                        mod.setInstallationState(new InstallationState(InstallationState.Status.INSTALLING, I18n.format("modwinder.status.downloading.file", fileName)));
                        Path temp = HTTPRequestHelper.downloadFile(fileName, downloadURL);
                        mod.setInstallationState(new InstallationState(InstallationState.Status.INSTALLING, I18n.format("modwinder.status.installing", fileName)));
                        // read required information and move to mod repository
                        File archived = moveToModRepository(temp, mod, fileName);
                        if (archived != null) {
                            downloadedFiles.add(CompletableFuture.completedFuture(Collections.singletonList(archived)));
                        }
                        mod.setInstallationState(INSTALLATION_END);
                        return downloadedFiles;
                    }, DOWNLOAD_THREAD);  // operate on the download thread to avoid concurrency issues with files
        } catch (MalformedURLException e) {
            throw new InstallationException("Invalid project id " + mod.getCurseId(), e);
        }
    }

    private static void installDependencies(JsonObject fileToDownload, List<CompletableFuture<List<File>>> downloadedFiles) {
        for (JsonElement dependency : fileToDownload.get("dependencies").getAsJsonArray()) {
            JsonObject dep = dependency.getAsJsonObject();
            if (dep.get("type").getAsString().equals("REQUIRED")) {
                int depCurseId = dep.get("addOnId").getAsInt();
                // If there is an existing entry for that dependency, update it, otherwise use a dummy
                ModEntry depEntry = ModWinderList.ALL.getModEntries().stream()
                        .filter(me -> me.getCurseId() == depCurseId)
                        .findAny()
                        .orElse(new DummyModEntry(depCurseId));
                // check that we actually need to download it first
                if (depEntry.getInstallationState().getStatus().canInstall(depEntry)) {
                    // We want the exceptions to be thrown when joining, so no handling right now
                    downloadedFiles.add(installLatestFromCurseforge(depEntry));
                }
            }
        }
    }

    /**
     * Moves a previously downloaded file to Forge's mod repository
     *
     * @param artifactPath the path to the downloaded file
     * @param mod          the mod being installed
     * @param originalName the initial file name of the downloaded file
     * @return the new location of the artifact
     */
    @Nullable
    private static File moveToModRepository(Path artifactPath, ModEntry mod, String originalName) {
        Pair<byte[], Attributes> ret = readManifestData(artifactPath);
        byte[] manifestData = ret.getLeft();
        Attributes meta = ret.getRight();
        File modsDir = new File(Launch.minecraftHome, "mods/" + MinecraftForge.MC_VERSION);
        @Nullable Artifact artifact = readArtifact(MOD_LIST.getRepository(), meta);

        try {
            if (artifact == null) {
                ModWinder.LOGGER.warn("{}'s Maven-Artifact attribute is absent, putting in mods", mod.getName());
                ModDeleter.scheduleModDeletion(mod.getModId());
                return Files.copy(artifactPath, modsDir.toPath().resolve(originalName)).toFile();
            }

            if (!artifact.getFile().getParentFile().exists() && !artifact.getFile().getParentFile().mkdirs()) {
                throw new InstallationException("Could not create parent directories for " + artifact.getFile());
            }
            MOD_LIST.getRepository().archive(artifact, artifactPath.toFile(), manifestData);
            // remove the current version of the mod
            List<Artifact> artifacts = ReflectionHelper.getPrivateValue(ModList.class, MOD_LIST, "artifacts");
            Map<String, Artifact> art_map = ReflectionHelper.getPrivateValue(ModList.class, MOD_LIST, "art_map");
            // if the map contains that exact artifact, it will get replaced during list.add()
            artifacts.removeIf(o -> !art_map.containsKey(o.toString()) && artifact.matchesID(o));

            // Remove manually installed mods.
            ModDeleter.scheduleModDeletion(mod.getModId());

            mod.setLocalArtifact(artifact);
            LOCAL_MODS.add(mod, artifact);
            MOD_LIST.add(artifact);

            // Extract contained files from the jar
            libraryManager$extractPacked.invoke(artifact.getFile(), MOD_LIST, modsDir);

            LOCAL_MODS.save();
            MOD_LIST.save();
        } catch (Throwable e) {
            throw new InstallationException("Could not archive downloaded mod " + (artifact != null ? artifact.getFilename() : originalName), e);
        }
        return artifact.getFile();
    }

    /**
     *
     * @param artifactPath the path to an existing JAR file
     * @return a pair of the manifest's binary data and its parsed attributes
     */
    @Nonnull
    private static Pair<byte[], Attributes> readManifestData(Path artifactPath) {
        // Read the jar's manifest to get required artifact information
        try {
            Attributes meta;
            byte[] manifestData;
            byte[] data = Files.readAllBytes(artifactPath);
            //We use zip input stream directly, as the current Oracle implementation of JarInputStream only works when the manifest is the First/Second entry in the jar...
            try (ZipInputStream zi = new ZipInputStream(new ByteArrayInputStream(data))) {
                ZipEntry ze;
                while ((ze = zi.getNextEntry()) != null) {
                    if (ze.getName().equalsIgnoreCase(JarFile.MANIFEST_NAME)) {
                        manifestData = IOUtils.toByteArray(zi);
                        meta = new Manifest(new ByteArrayInputStream(manifestData)).getMainAttributes();
                        return Pair.of(manifestData, meta);
                    }
                }
            }
            throw new InstallationException("Could not find manifest data in downloaded file " + artifactPath.toFile().getName());
        } catch (IOException e) {
            throw new InstallationException("Could not read manifest data from downloaded file " + artifactPath.toFile().getName(), e);
        }
    }

    @Nullable
    private static Artifact readArtifact(Repository repo, Attributes meta) {
        String timestamp = meta.getValue(new Attributes.Name("Timestamp"));
        if (timestamp != null) {
            timestamp = SnapshotJson.TIMESTAMP.format(new Date(Long.parseLong(timestamp)));
        }

        String mavenArtifact = meta.getValue(new Attributes.Name("Maven-Artifact"));
        if (mavenArtifact == null) {
            return null;
        }
        return new Artifact(repo, mavenArtifact, timestamp);
    }

}
