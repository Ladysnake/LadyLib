package ladylib.nbt;

import net.minecraft.nbt.NBTBase;

public interface NBTTypeAdapter<T, NBT extends NBTBase> {

    NBT toNBT(T value);

    T fromNBT(NBT nbt);

}
