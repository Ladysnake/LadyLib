package ladylib.modwinder.installer;

import com.google.common.annotations.VisibleForTesting;
import ladylib.modwinder.ModWinder;
import net.minecraftforge.fml.common.Loader;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeleteOldMods extends Thread {
    private List<File> modsToDelete = new ArrayList<>();
    private File llSource = Loader.instance().getIndexedModList().get(ModWinder.MOD_ID).getSource();

    @Override
    public void run() {
        List<String> commandArgs = new ArrayList<>();
        File runDir = llSource;
        commandArgs.add("java");
        if (!llSource.isDirectory()) {
            commandArgs.add("-jar");
            commandArgs.add(llSource.getAbsolutePath());
            runDir = llSource.getParentFile();
        }
        commandArgs.add(DeleteOldMods.class.getName());
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
    static DeleteOldMods instance;
    @VisibleForTesting
    static Path modsDir = Paths.get("mods").toAbsolutePath().normalize();

    public static void scheduleModDeletion(String modId) {
        File currentSource = Loader.instance().getIndexedModList().get(modId).getSource();
        if (DeleteOldMods.isInModFolder(currentSource)) {
            DeleteOldMods.scheduleFileDeletion(currentSource);
        }
    }

    private static void scheduleFileDeletion(File mod) {
        if (instance == null) {
            instance = new DeleteOldMods();
            Runtime.getRuntime().addShutdownHook(instance);
        }
        instance.modsToDelete.add(mod);
    }

    public static void main(String... args) throws InterruptedException {
        if (args.length <= 0) {
            return;
        }
        modsDir = Paths.get(args[0]);
        // Wait for the main application to shutdown completely
        Thread.sleep(1_000);
        Arrays.stream(args)
                .skip(1)
                .map(File::new)
                .filter(File::exists)
                .filter(DeleteOldMods::isInModFolder)       // Just to make sure that we aren't doing a huge mistake
                .filter(f -> !f.delete())
                .map(File::getAbsolutePath)
                .reduce((s, s1) -> s + "\n" + s1)
                .ifPresent(DeleteOldMods::showManualDeletionDialog);
    }

    private static void showManualDeletionDialog(String oopsie) {
        try {
            Desktop.getDesktop().open(modsDir.toFile());
        } catch (IOException e) {
            Logger.getGlobal().log(Level.SEVERE, "Could not open mods directory", e);
        }
        JOptionPane.showMessageDialog(null, "The following mods need to be deleted manually:\n" + oopsie, "Failed to delete old mods.", JOptionPane.WARNING_MESSAGE);
    }

    private static boolean isInModFolder(File f) {
        return f.toPath().toAbsolutePath().normalize().startsWith(modsDir);
    }
}
