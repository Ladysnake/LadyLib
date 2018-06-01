package ladylib.networking.minecraft;


import ladylib.LadyLib;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

public class PacketHandler {
    public static final SimpleNetworkWrapper NET = NetworkRegistry.INSTANCE.newSimpleChannel(LadyLib.MOD_ID);
    private static int nextId;

    public static void initPackets() {
//        NET.registerMessage(new BreathMessageHandler(), BreathMessage.class, nextId++, Side.CLIENT);
//        NET.registerMessage(new SpecialRewardMessageHandler(), SpecialRewardMessage.class, nextId++, Side.SERVER);
    }
}