package ladylib.modwinder.installer;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class ModDeleterTest {

    @After
    public void tearDown() {
        ModDeleter.modsDir = Paths.get("mods").toAbsolutePath().normalize();
        ModDeleter.instance = null;
    }

    @Test
    public void scheduleFileDeletion() throws IOException {
        ModDeleter.scheduleFileDeletion(Files.createTempFile(null, null).toFile());
        assertNotNull("Failed to create instance", ModDeleter.instance);
        assertTrue("The shutdown hook was not registered", Runtime.getRuntime().removeShutdownHook(ModDeleter.instance));
    }

    @Test
    public void isInModFolder() {
        assertTrue(ModDeleter.isInModFolder(ModDeleter.modsDir.resolve("abc/123/dissolution.jar").toFile()));
        assertFalse(ModDeleter.isInModFolder(ModDeleter.modsDir.resolve("../abc/123/dissolution.jar").toFile()));
    }

    @Test
    public void main() throws IOException {
        Path testFile = Files.createTempFile("test", null);
        Path validModDir = testFile.getParent();
        Path invalidModDir = Files.createTempDirectory("testdir");
        ModDeleter.main(invalidModDir.toString(), testFile.toString());
        assertTrue("The file should not have been deleted", Files.exists(testFile));
        ModDeleter.main(validModDir.toString(), testFile.toString());
        assertFalse("The file should have been deleted", Files.exists(testFile));
        assertEquals("The mods directory didn't get updated", ModDeleter.modsDir, validModDir);
    }
}