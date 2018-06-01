package ladylib.networking.minecraft;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class CapabilitySyncMessage implements IMessage {
    private int id;

    public CapabilitySyncMessage(int id) {
        this.id = id;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.id = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(id);
    }
}
