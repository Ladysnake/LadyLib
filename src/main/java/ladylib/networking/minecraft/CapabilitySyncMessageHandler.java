package ladylib.networking.minecraft;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

public class CapabilitySyncMessageHandler implements IMessageHandler<CapabilitySyncMessage, IMessage> {
    @Nullable
    @Override
    public IMessage onMessage(CapabilitySyncMessage message, MessageContext ctx) {

        return null;
    }
}
