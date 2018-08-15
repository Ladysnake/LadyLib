package ladylib.modwinder.installer;

import com.google.common.annotations.VisibleForTesting;
import ladylib.modwinder.ModWinder;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class doubling as a standalone application used to removed mods that are currently loaded by the JVM
 */
public class ModDeleter extends Thread {
    private Set<File> modsToDelete = new HashSet<>();
    private File llSource = Loader.instance().getIndexedModList().getOrDefault(ModWinder.MOD_ID, new DummyModContainer()).getSource();

    /**
     * Starts a new java process that will delete every file previously scheduled
     */
    @Override
    public void run() {
        List<String> commandArgs = new ArrayList<>();
        File runDir = llSource;
        commandArgs.add(System.getProperty("java.home"));
        if (!llSource.isDirectory()) {
            commandArgs.add("-jar");
            commandArgs.add(llSource.getAbsolutePath());
            runDir = runDir.getParentFile();
        }
        commandArgs.add(ModDeleter.class.getName());
        commandArgs.add(modsDir.toString());
        modsToDelete.stream().map(File::getAbsolutePath).forEach(commandArgs::add);
        ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
        processBuilder.directory(runDir);
        try {
            processBuilder.start();
        } catch (IOException e) {
            ModWinder.LOGGER.error("Could not start mod remover process", e);
        }
    }

    @VisibleForTesting
    static ModDeleter instance;
    @VisibleForTesting
    static Path modsDir = Paths.get("mods").toAbsolutePath().normalize();

    /**
     * Checks if a file is in the currently defined "mods" directory. <br>
     * More specifically, it will return true for any file which path starts with
     * the path of the "mods" directory.
     *
     * @param file the representation of the file to test
     * @return true if <code>file</code> is in the "mods" folder
     */
    public static boolean isInModFolder(File file) {
        return file.toPath().toAbsolutePath().normalize().startsWith(modsDir);
    }

    /**
     * Schedules a loaded mod for deletion on Minecraft exit.
     *
     * @param modId the id of the mod to delete
     */
    public static void scheduleModDeletion(String modId) {
        ModContainer mc = Loader.instance().getIndexedModList().get(modId);
        if (mc == null) {
            return;
        }
        File currentSource = mc.getSource();
        if (ModDeleter.isInModFolder(currentSource)) {
            ModDeleter.scheduleFileDeletion(currentSource);
        }
    }

    /**
     * Schedules a file to be deleted when the program exits
     * @param mod the file to delete
     */
    @VisibleForTesting
    static void scheduleFileDeletion(File mod) {
        if (instance == null) {
            instance = new ModDeleter();
            Runtime.getRuntime().addShutdownHook(instance);
        }
        instance.modsToDelete.add(mod);
    }

    /**
     * Deletes files that have been passed as args.
     * The first file will not be deleted and should represent the mods folder.
     * @param args a list of strings representing file paths
     */
    public static void main(String... args) {
        if (args.length <= 0) {
            return;
        }
        modsDir = Paths.get(args[0]);
        // Wait for the main application to shutdown completely
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            Logger.getGlobal().log(Level.WARNING, "Got interrupted, aborting operation", e);
            return;
        }
        Arrays.stream(args)
                .skip(1)
                .map(File::new)
                .filter(File::exists)
                .filter(ModDeleter::isInModFolder)       // Just to make sure that we aren't doing a huge mistake
                .filter(f -> !f.delete())
                .map(File::getAbsolutePath)
                .reduce((s, s1) -> s + "\n" + s1)
                .ifPresent(ModDeleter::showManualDeletionDialog);
    }

    /**
     * Displays a dialog telling the user to delete mods manually
     * @param oopsie a string representing the list of mods that couldn't be deleted
     */
    private static void showManualDeletionDialog(String oopsie) {
        try {
            Desktop.getDesktop().open(modsDir.toFile());
        } catch (IOException e) {
            Logger.getGlobal().log(Level.SEVERE, "Could not open mods directory", e);
        }
        JOptionPane.showMessageDialog(
                null,
                "The following mods need to be deleted manually:\n" + oopsie,
                "Failed to delete old mods.",
                JOptionPane.WARNING_MESSAGE
        );
    }
}
