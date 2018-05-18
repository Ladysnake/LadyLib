package ladylib.nbt;

import net.minecraft.nbt.NBTBase;

public interface NBTMutatingTypeAdapter<T, NBT extends NBTBase> extends NBTTypeAdapter<T, NBT> {

    @Override
    default T fromNBT(NBTBase nbt) {
        throw new UnsupportedOperationException();
    }

    T fromNBT(T value, NBTBase nbt);

}
