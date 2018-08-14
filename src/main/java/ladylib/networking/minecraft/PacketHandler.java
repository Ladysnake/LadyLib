package ladylib.networking.minecraft;


import ladylib.LadyLib;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
    private PacketHandler() { }

    public static final SimpleNetworkWrapper NET = NetworkRegistry.INSTANCE.newSimpleChannel(LadyLib.MOD_ID);
    private static int nextId;

    public static void initPackets() {
        NET.registerMessage(CapabilitySyncMessageHandler.class, CapabilitySyncMessage.class, nextId++, Side.CLIENT);
    }
}