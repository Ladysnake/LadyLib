package ladylib.nbt;

import com.google.gson.reflect.TypeToken;
import net.minecraft.nbt.NBTBase;

@FunctionalInterface
public interface NBTTypeAdapterFactory<T, NBT extends NBTBase> {

    NBTTypeAdapter<T, NBT> create(TypeToken type, boolean allowMutating);

}
