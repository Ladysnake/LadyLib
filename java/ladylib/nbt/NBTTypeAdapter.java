package ladylib.nbt;

import net.minecraft.nbt.NBTBase;
import org.jetbrains.annotations.Contract;

import java.util.Optional;

/**
 * A type adapter that can serialize and deserialize objects from an appropriate NBT data structure.
 *
 * @param <T>   the type of object handled by this adapter
 * @param <NBT> the nbt type used to serialize objects
 */
public interface NBTTypeAdapter<T, NBT extends NBTBase> {

    NBT toNBT(T value);

    /**
     * Implementations of this method that mutate the input value should always implement {@link NBTMutatingTypeAdapter}
     */
    default T fromNBT(T value, NBTBase nbt) {
        return fromNBT(nbt);
    }

    /**
     * Recreates an object of the appropriate type from the given NBT.
     * If the given NBT is invalid, attempts to obtain a default instance.
     *
     * @param nbt An nbt object representing an object of the appropriate type, as serialized by {@link #toNBT(Object)}
     * @return a new object created using the given nbt
     * @throws NBTDeserializationException if the operation fails, because no default type is available or otherwise.
     */
    T fromNBT(NBTBase nbt);

    /**
     * Helper method to cast an NBT object to a type usable by adapters, if possible. <br>
     * Casts the given value to the class represented by the given Class object.
     * Returns {@link Optional#EMPTY} if the value is null or if it is not an instance of this class.
     *
     * @return an optional containing an NBT object of the right type or an empty optional
     * if the input was incompatible.
     */
    @Contract(pure = true)
    static <NBT extends NBTBase> Optional<NBT> castNBT(NBTBase nbt, Class<NBT> clazz) {
        if (clazz.isInstance(nbt)) {
            return Optional.of(clazz.cast(nbt));
        }
        return Optional.empty();
    }

}
