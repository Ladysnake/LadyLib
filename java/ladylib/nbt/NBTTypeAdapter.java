package ladylib.nbt;

import net.minecraft.nbt.NBTBase;

public interface NBTTypeAdapter<T, NBT extends NBTBase> {

    NBT toNBT(T value);

    default T fromNBT(T value, NBTBase nbt) {
        return fromNBT(nbt);
    }

    T fromNBT(NBTBase nbt);

    default NBT cast(NBTBase nbt, Class<NBT> clazz) {
        return castNBT(nbt, clazz);
    }

    static <NBT extends NBTBase> NBT castNBT(NBTBase nbt, Class<NBT> clazz) {
        if (clazz.isInstance(nbt)) {
            return clazz.cast(nbt);
        }
        throw new MalformedNBTException("Expected an instance of " + clazz.getName() + " but " + nbt + " is of type " + nbt.getClass().getName());
    }

}
