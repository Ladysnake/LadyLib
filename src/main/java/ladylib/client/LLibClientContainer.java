package ladylib.client;

import ladylib.LLibContainer;
import ladylib.LadyLib;
import net.minecraftforge.fml.common.ModContainer;

public class LLibClientContainer extends LLibContainer {
    public LLibClientContainer(ModContainer owner) {
        super(owner);
    }

    /**
     * {@inheritDoc}
     * @param path  the path to the parent directory of the resource
     * @param files the name of one or more files, extension included
     */
    @Override
    public void addVanillaResourceOverride(String path, String... files) {
        LadyLib.INSTANCE.getClientHandler().addResourceOverride(getModId(), path, files);
    }
}
