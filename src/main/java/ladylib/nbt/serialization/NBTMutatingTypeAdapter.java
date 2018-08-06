package ladylib.nbt.serialization;

import com.google.common.annotations.Beta;
import net.minecraft.nbt.NBTBase;

/**
 * A mutating type adapter uses the given NBT data and uses it to mutate an existing object.
 * Mutating type adapters can generally not operate without an existing instance.
 * @param <T> the type of object that can be serialized by this adapter
 * @param <NBT> the type of tag into which objects are serialized
 */
@Beta
public interface NBTMutatingTypeAdapter<T, NBT extends NBTBase> extends NBTTypeAdapter<T, NBT> {

    @Override
    default T fromNBT(NBTBase nbt) {
        throw new UnsupportedOperationException();
    }

    T fromNBT(T value, NBTBase nbt);

}
