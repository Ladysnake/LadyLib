package ladylib.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;

public class SimpleProvider<T> implements ICapabilitySerializable<NBTBase> {

    private Capability<T> capability;
    private T instance;

    public SimpleProvider(Capability<T> capability, T instance) {
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
