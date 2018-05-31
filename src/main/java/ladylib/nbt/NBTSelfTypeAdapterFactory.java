package ladylib.nbt;

import com.google.gson.reflect.TypeToken;
import ladylib.misc.ReflectionUtil;
import net.minecraft.nbt.NBTBase;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.util.function.Function;
import java.util.function.Supplier;

public class NBTSelfTypeAdapterFactory implements NBTTypeAdapterFactory<NBTBase, NBTBase> {
    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public NBTTypeAdapter<NBTBase, NBTBase> create(TypeToken type, boolean allowMutating) {
        Class rawType = type.getRawType();
        if (NBTBase.class.isAssignableFrom(rawType)) {
            // Dynamically add nbt constructors as default value suppliers
            // Also use a hint of black magic to get the package-private constructor
            MethodHandles.Lookup lookup = ReflectionUtil.getTrustedLookup(rawType);
            TagAdapters.setDefaultValue(type, ReflectionUtil.createFactory(rawType, "get", Supplier.class, lookup));
            return new NBTSelfAdapter(type);
        }
        return null;
    }

    public static class NBTSelfAdapter<NBT extends NBTBase> extends AbstractNBTTypeAdapter<NBT, NBT> {

        protected NBTSelfAdapter(TypeToken<NBT> typeToken) {
            super(typeToken);
        }

        @Override
        public NBT toNBT(NBT value) {
            return value;
        }

        @SuppressWarnings("unchecked")
        @Override
        public NBT fromNBT(NBTBase nbt) {
            return castAnd(nbt, (Class<NBT>) typeToken.getRawType(), Function.identity());
        }
    }
}
