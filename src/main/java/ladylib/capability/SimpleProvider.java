package ladylib.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;

/**
 * A capability provider that can wrap any capability instance
 * as long as it is handled by the capability's default storage
 * @param <C> the capability type
 */
public class SimpleProvider<C> implements ICapabilitySerializable<NBTBase> {

    private Capability<C> capability;
    private C instance;

    public SimpleProvider(Capability<C> capability, C instance) {
        this.capability = capability;
        this.instance = instance;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing facing) {
        return capability == this.capability;
    }

    @Override
    public <V> V getCapability(@Nonnull Capability<V> capability, EnumFacing facing) {
        if (capability == this.capability) {
            return this.capability.cast(this.instance);
        }
        return null;
    }

    @Override
    public NBTBase serializeNBT() {
        return this.capability.getStorage().writeNBT(this.capability, this.instance, null);
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        this.capability.getStorage().readNBT(this.capability, this.instance, null, nbt);
    }
}
