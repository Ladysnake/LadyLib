package ladylib.nbt;

import com.google.gson.reflect.TypeToken;
import ladylib.misc.ReflectionUtil;
import net.minecraft.nbt.NBTBase;
import net.minecraftforge.common.util.INBTSerializable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.function.Supplier;

public class SerializableNBTTypeAdapterFactory implements NBTTypeAdapterFactory<INBTSerializable<NBTBase>, NBTBase> {
    @Override
    public NBTTypeAdapter<INBTSerializable<NBTBase>, NBTBase> create(TypeToken type, boolean allowMutating) {
        Class<?> rawType = type.getRawType();
        if (!INBTSerializable.class.isAssignableFrom(rawType)) return null;
        Supplier<INBTSerializable<NBTBase>> constructor =
                ReflectionUtil.createFactory(rawType, "get", Supplier.class);
        Type elementType = Object.class;
        Type nbtType = type.getType();
        if (nbtType instanceof WildcardType) {
            nbtType = ((WildcardType) nbtType).getUpperBounds()[0];
        }
        if (nbtType instanceof ParameterizedType) {
            elementType = ((ParameterizedType) nbtType).getActualTypeArguments()[0];
        }
        return new SerializableNBTTypeAdapter<>(constructor, TypeToken.get(elementType).getRawType());
    }

    public static class SerializableNBTTypeAdapter<T extends INBTSerializable<NBT>, NBT extends NBTBase> extends AbstractNBTTypeAdapter<T, NBT> {
        private final Supplier<T> constructor;
        private final Class<NBT> nbtClass;

        @SuppressWarnings("unchecked")
        public SerializableNBTTypeAdapter(Supplier<T> constructor, Class nbtClass) {
            super();
            this.constructor = constructor;
            this.nbtClass = nbtClass;
        }

        @Override
        public NBT toNBT(T value) {
            return value.serializeNBT();
        }

        @Override
        public T fromNBT(NBTBase nbt) {
            return castAnd(nbt, nbtClass, tag -> {
                T value = constructor.get();
                value.deserializeNBT(tag);
                return value;
            });
        }
    }
}
