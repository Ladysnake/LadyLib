package ladylib.modwinder.installer;

import org.junit.Test;

import java.awt.*;
import java.io.*;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class DeleteOldModsTest {

    @Test
    public void run() {
        // TODO
    }

    @Test
    public void scheduleModDeletion() {
        // TODO
    }

    @Test
    public void main() throws IOException, InterruptedException {
        Path defaultModsDir = Paths.get(DeleteOldMods.modsDir.toString());
        DeleteOldMods.main();
        assertEquals("Path should not have been modified", defaultModsDir, DeleteOldMods.modsDir);
        Path testFile = Files.createTempFile("test", null);
        Path validModDir = testFile.getParent();
        Path invalidModDir = Files.createTempDirectory("testdir");
        DeleteOldMods.main(invalidModDir.toString(), testFile.toString());
        assertTrue("The file should not have been deleted", Files.exists(testFile));
        DeleteOldMods.main(validModDir.toString(), testFile.toString());
        assertFalse("The file should have been deleted", Files.exists(testFile));
    }
}