package ladylib.modwinder.installer;

import java.net.URL;
import java.util.Collections;
import java.util.List;

public class DummyModEntry extends ModEntry {

    public DummyModEntry(int curseid) {
        this("", curseid, "", "", null, Collections.emptyList());
    }

    public DummyModEntry(String modid, int curseid, String name, String author, URL updateUrl, List<ModEntry> dlcs) {
        super(modid, curseid, name, author, updateUrl, dlcs);
    }

    @Override
    protected void init(boolean isDlc) {
        // NO-OP
    }
}
