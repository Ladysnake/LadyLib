package ladylib.nbt;

import com.google.gson.reflect.TypeToken;
import ladylib.capability.internal.CapabilityRegistrar;
import net.minecraft.nbt.NBTBase;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.function.Supplier;

public class SerializableNBTTypeAdapterFactory implements NBTTypeAdapterFactory<INBTSerializable<NBTBase>, NBTBase> {
    @Override
    public NBTTypeAdapter<INBTSerializable<NBTBase>, NBTBase> create(TypeToken type) {
        Class<?> rawType = type.getRawType();
        if (!INBTSerializable.class.isAssignableFrom(rawType)) return null;
        Supplier<INBTSerializable<NBTBase>> constructor =
                CapabilityRegistrar.createFactory(rawType, "get", Supplier.class);
        return new SerializableNBTTypeAdapter<>(constructor);
    }

    public static class SerializableNBTTypeAdapter<T extends INBTSerializable<NBT>, NBT extends NBTBase> implements NBTTypeAdapter<T, NBT> {
        private final Supplier<T> constructor;

        public SerializableNBTTypeAdapter(Supplier<T> constructor) {
            super();
            this.constructor = constructor;
        }

        @Override
        public NBT toNBT(T value) {
            return value.serializeNBT();
        }

        @Override
        public T fromNBT(NBT nbt) {
            T value = constructor.get();
            value.deserializeNBT(nbt);
            return value;
        }
    }
}
