package ladylib.nbt;

import net.minecraft.nbt.NBTBase;

/**
 * A mutating type adapter uses the given NBT data and uses it to mutate an existing object.
 * Mutating type adapters can generally not operate without an existing instance.
 * @param <T>
 * @param <NBT>
 */
public interface NBTMutatingTypeAdapter<T, NBT extends NBTBase> extends NBTTypeAdapter<T, NBT> {

    @Override
    default T fromNBT(NBTBase nbt) {
        throw new UnsupportedOperationException();
    }

    T fromNBT(T value, NBTBase nbt);

}
