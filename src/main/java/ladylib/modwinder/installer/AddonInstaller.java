package ladylib.modwinder.installer;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ladylib.misc.ReflectionUtil;
import ladylib.modwinder.ModWinder;
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

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
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

public class AddonInstaller {

    public static final InstallationState DOWNLOAD_START = new InstallationState(InstallationState.Status.INSTALLING, I18n.format("modwinder.status.downloading.start"));
    public static final InstallationState DOWNLOAD_FAILED = new InstallationState(InstallationState.Status.FAILED, I18n.format("modwinder.status.failed"));
    public static final InstallationState INSTALLATION_COMPLETE = new InstallationState(InstallationState.Status.INSTALLED, I18n.format("modwinder.status.installed"), I18n.format("modwinder.status.restart"));
    public static final InstallationState INSTALLATION_END = new InstallationState(InstallationState.Status.INSTALLING, I18n.format("modwinder.status.installing.end"));
    public static final InstallationState UNINSTALLED = new InstallationState(InstallationState.Status.UNINSTALLED, I18n.format("modwinder.status.uninstalled"), I18n.format("modwinder.status.restart"));
    public static final InstallationState UNINSTALL_FAILED = new InstallationState(InstallationState.Status.FAILED, I18n.format("modwinder.status.uninstalled.failed"));

    /**
     * A single thread used to download and manage files.
     */
    private static final Executor DOWNLOAD_THREAD = Executors.newSingleThreadExecutor(r -> new Thread(r, "Ladylib Installer"));
    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();
    private static final ModWinderModList LOCAL_MODS;
    private static final ModList MOD_LIST;
    private static final MethodHandle libraryManager$extractPacked = ReflectionUtil.findMethodHandleFromObfName(LibraryManager.class, "extractPacked", Pair.class, File.class, ModList.class, File[].class);

    static {
        Path modsPath = Paths.get( "mods", MinecraftForge.MC_VERSION);
        LOCAL_MODS = ModWinderModList.create(modsPath.resolve("modwinder_local_mods.json"));
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
        // holder class for required values
        class ParamHolder {
            private String fileName, fileId;

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
                        throw new RuntimeException(t);
                    } else {
                        mod.setInstallationState(INSTALLATION_COMPLETE);
                        return result;
                    }
                });
    }

    public static void uninstall(ModEntry modEntry) {
        try {
            for (ModEntry dlc : modEntry.getDlcs()) {
                uninstall(dlc);
            }
            List<Artifact> artifacts = ReflectionHelper.getPrivateValue(ModList.class, MOD_LIST, "artifacts");
            Map<String, Artifact> art_map = ReflectionHelper.getPrivateValue(ModList.class, MOD_LIST, "art_map");
            File modSource = Loader.instance().getIndexedModList().get(modEntry.getModId()).getSource();
            artifacts.removeIf(artifact -> {
                if (Objects.equals(artifact.getFile(), modSource)) {
                    art_map.remove(artifact.toString());
                    // just in case someone added the entry in the mod list manually, it doesn't hurt to add it here
                    LOCAL_MODS.add(modEntry, artifact);
                    return true;
                }
                return false;
            });
            MOD_LIST.save();
            LOCAL_MODS.save();
            modEntry.setInstallationState(UNINSTALLED);
        } catch (Exception e) {
            ModWinder.LOGGER.error("Could not uninstall mod {} ({})", modEntry.getName(), modEntry.getModId(), e);
            modEntry.setInstallationState(UNINSTALL_FAILED);
        }
    }

    /**
     * Re-enables a mod that has been uninstalled but is still loaded in the minecraft instance
     * @param modEntry the information regarding the mod to re-enable
     * @return true if the mod has been re-enabled, false if it needs to be installed from scratch
     */
    public static boolean attemptReEnabling(ModEntry modEntry) {
        try {
            if (modEntry.isDlc()) {
                // if any of the parent mods is not up to date, just update everything
                if (!modEntry.getParentMods().allMatch(AddonInstaller::attemptReEnabling)) {
                    return false;
                }
            }
            Artifact disabled = LOCAL_MODS.getArtifact(modEntry, MOD_LIST.getRepository());
            if (disabled == null) {
                return false;
            }
            MOD_LIST.add(disabled);
            MOD_LIST.save();
            if (Loader.isModLoaded(modEntry.getModId())) {
                // mod is already loaded, just remove the current indication
                modEntry.setInstallationState(InstallationState.NAUGHT);
            } else {
                // instantly complete the installation
                modEntry.setInstallationState(INSTALLATION_COMPLETE);
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
                        List<CompletableFuture<List<File>>> downloadedFiles = new ArrayList<>();
                        for (JsonElement dependency : fileToDownload.get("dependencies").getAsJsonArray()) {
                            JsonObject dep = dependency.getAsJsonObject();
                            if (dep.get("type").getAsString().equals("REQUIRED")) {
                                int depCurseId = dep.get("addOnId").getAsInt();
                                // If there is an existing entry for that dependency, update it, otherwise use a dummy
                                ModEntry depEntry = ModEntry.getLadysnakeMods().stream()
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
                        // download this file from the associated url
                        String downloadURL = fileToDownload.getAsJsonObject().get("downloadURL").getAsString();
                        mod.setInstallationState(new InstallationState(InstallationState.Status.INSTALLING, I18n.format("modwinder.status.downloading.file", fileName)));
                        Path temp = downloadFile(fileName, downloadURL);
                        mod.setInstallationState(new InstallationState(InstallationState.Status.INSTALLING, I18n.format("modwinder.status.installing", fileName)));
                        // read required information and move to mod repository
                        File archived = moveToModRepository(temp, mod);
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

    @Nullable
    private static File moveToModRepository(Path artifactPath, ModEntry mod) {
        Attributes meta;
        byte[] data;
        byte[] manifestData;
        readManifest:
        // Read the jar's manifest to get required artifact information
        try {
            data = Files.readAllBytes(artifactPath);
            //We use zip input stream directly, as the current Oracle implementation of JarInputStream only works when the manifest is the First/Second entry in the jar...
            try (ZipInputStream zi = new ZipInputStream(new ByteArrayInputStream(data))) {
                ZipEntry ze;
                while ((ze = zi.getNextEntry()) != null) {
                    if (ze.getName().equalsIgnoreCase(JarFile.MANIFEST_NAME)) {
                        manifestData = IOUtils.toByteArray(zi);
                        meta = new Manifest(new ByteArrayInputStream(manifestData)).getMainAttributes();
                        break readManifest;
                    }
                }
            }
            throw new InstallationException("Could not find manifest data in downloaded file " + artifactPath.toFile().getName());
        } catch (IOException e) {
            throw new InstallationException("Could not read manifest data from downloaded file " + artifactPath.toFile().getName(), e);
        }
        File modsDir = new File(Launch.minecraftHome, "mods/" + MinecraftForge.MC_VERSION);
        Artifact artifact = readArtifact(MOD_LIST.getRepository(), meta);

        if (!artifact.getFile().getParentFile().exists() && !artifact.getFile().getParentFile().mkdirs()) {
            throw new InstallationException("Could not create parent directories for " + artifact.getFile());
        }
        MOD_LIST.getRepository().archive(artifact, artifactPath.toFile(), manifestData);
        try {
            // remove the current version of the mod
            List<Artifact> artifacts = ReflectionHelper.getPrivateValue(ModList.class, MOD_LIST, "artifacts");
            Map<String, Artifact> art_map = ReflectionHelper.getPrivateValue(ModList.class, MOD_LIST, "art_map");
            // if the map contains that exact artifact, it will get replaced during list.add()
            artifacts.removeIf(o -> !art_map.containsKey(o.toString()) && artifact.matchesID(o));

            LOCAL_MODS.add(mod, artifact);
            MOD_LIST.add(artifact);

            // Extract contained files from the jar
            libraryManager$extractPacked.invoke(artifact.getFile(), MOD_LIST, new File[] {modsDir});

            LOCAL_MODS.save();
            MOD_LIST.save();
        } catch (Throwable e) {
            throw new InstallationException("Could not archive downloaded mod " + artifact.getFilename(), e);
        }
        return artifact.getFile();
    }

    private static Artifact readArtifact(Repository repo, Attributes meta) {
        String timestamp = meta.getValue(new Attributes.Name("Timestamp"));
        if (timestamp != null) {
            timestamp = SnapshotJson.TIMESTAMP.format(new Date(Long.parseLong(timestamp)));
        }

        String mavenArtifact = meta.getValue(new Attributes.Name("Maven-Artifact"));
        if (mavenArtifact == null) {
            throw new InstallationException("The Maven-Artifact attribute is absent");
        }
        return new Artifact(repo, mavenArtifact, timestamp);
    }

    private static Path downloadFile(String dest, String urlString) {
        try {
            Path temp = Files.createTempFile(dest, null);
            URL url = new URL(urlString);
            // download the file into the mods directory
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            try (FileOutputStream fos = new FileOutputStream(temp.toFile())) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
            // Make sure temporary files don't linger
            temp.toFile().deleteOnExit();
            return temp;
        } catch (IOException e) {
            throw new InstallationException("Could not download file " + dest, e);
        }
    }

}
