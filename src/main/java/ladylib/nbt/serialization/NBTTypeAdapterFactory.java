package ladylib.nbt.serialization;

import com.google.common.annotations.Beta;
import com.google.gson.reflect.TypeToken;
import net.minecraft.nbt.NBTBase;

import javax.annotation.Nullable;

/**
 * A factory creating instances of type adapters for set of related types.
 * <p>Type adapter factories select which types they provide type adapters
 * for. If a factory cannot support a given type, it must return null when
 * that type is passed to {@link #create}. Factories should expect {@code
 * create()} to be called on them for many types and should return null for
 * most of those types.
 * @param <T> the common superclass of types this factory can create adapters for
 * @param <NBT> the type of NBT object used to serialize objects of those types
 */
@Beta
@FunctionalInterface
public interface NBTTypeAdapterFactory<T, NBT extends NBTBase> {

    @Nullable
    NBTTypeAdapter<? extends T, ? extends NBT> create(TypeToken type, boolean allowMutating);

}
