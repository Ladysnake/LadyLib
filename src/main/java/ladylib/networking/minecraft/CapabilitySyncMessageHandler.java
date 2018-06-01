package ladylib.networking.minecraft;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class CapabilitySyncMessageHandler implements IMessageHandler<CapabilitySyncMessage, IMessage> {
    @Override
    public IMessage onMessage(CapabilitySyncMessage message, MessageContext ctx) {

        return null;
    }
}
