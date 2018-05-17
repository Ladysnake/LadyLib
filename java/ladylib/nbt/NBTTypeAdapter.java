package ladylib.nbt;

import net.minecraft.nbt.NBTBase;

public interface NBTTypeAdapter<T, NBT extends NBTBase> {

    NBT toNBT(T value);

    default T fromNBT(T value, NBT nbt) {
        return fromNBT(nbt);
    }

    T fromNBT(NBT nbt);

}
