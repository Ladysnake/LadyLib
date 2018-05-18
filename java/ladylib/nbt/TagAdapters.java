package ladylib.nbt;

import com.google.gson.reflect.TypeToken;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;

public class TagAdapters {
    private static final List<NBTTypeAdapterFactory> factories = new ArrayList<>();
    private static final Map<TypeToken<?>, NBTAdapterEntry> cache = new HashMap<>();

    static {
        addPrimitiveFactory(boolean.class, Boolean.class, BaseNBTAdapters.BooleanAdapter::new);
        addPrimitiveFactory(byte.class,    Byte.class,    BaseNBTAdapters.ByteAdapter::new);
        addPrimitiveFactory(short.class,   Short.class,   BaseNBTAdapters.ShortAdapter::new);
        addPrimitiveFactory(int.class,     Integer.class, BaseNBTAdapters.IntAdapter::new);
        addPrimitiveFactory(float.class,   Float.class,   BaseNBTAdapters.FloatAdapter::new);
        addPrimitiveFactory(long.class,    Long.class,    BaseNBTAdapters.LongAdapter::new);
        addPrimitiveFactory(double.class,  Double.class,  BaseNBTAdapters.DoubleAdapter::new);

        addFactory(String.class,     BaseNBTAdapters.StringAdapter::new);
        addFactory(ItemStack.class,  BaseNBTAdapters.ItemStackAdapter::new);
        addFactory(BlockPos.class,   BaseNBTAdapters.BlockPosAdapter::new);
        addFactory(UUID.class,       BaseNBTAdapters.UUIDAdapter::new);

        factories.add(new EnumNBTTypeAdapterFactory());
        factories.add(new SerializableNBTTypeAdapterFactory());
        factories.add(new RegistryEntryNBTAdapterFactory());
        factories.add(new ImmutableCollectionNBTAdapterFactory());
        factories.add(new CollectionNBTTypeAdapterFactory());
        factories.add(ReflectiveNBTAdapterFactory.INSTANCE);
    }

    private static <T> void addPrimitiveFactory(Class<T> primitive, Class<T> wrapper, Supplier<NBTTypeAdapter<T, ?>> factory) {
        factories.add((type, allowMutating) -> type.getRawType() == primitive || type.getRawType() == wrapper ? factory.get() : null);
    }

    private static <T> void addFactory(Class<T> primitive, Supplier<NBTTypeAdapter<T, ?>> factory) {
        factories.add((type, allowMutating) -> type.getRawType() == primitive ? factory.get() : null);
    }

    public static NBTTypeAdapter getNBTAdapter(Field field) {
        return getNBTAdapter(TypeToken.get(field.getGenericType()), true);
    }

    @SuppressWarnings("unchecked")
    public static <T, NBT extends NBTBase> NBTTypeAdapter<T, NBT> getNBTAdapter(TypeToken<T> type, boolean allowMutating) {
        return cache.computeIfAbsent(type, NBTAdapterEntry::new).computeTypeAdapter(allowMutating);

    }

    public static class NBTAdapterEntry {
        private TypeToken type;
        private NBTTypeAdapter typeAdapter;
        private NBTMutatingTypeAdapter mutatingTypeAdapter;

        public NBTAdapterEntry(TypeToken type) {
            this.type = type;
        }

        @Nonnull
        public NBTTypeAdapter computeTypeAdapter(boolean allowMutating) {
            if (allowMutating && mutatingTypeAdapter != null) {
                return mutatingTypeAdapter;
            }
            if (typeAdapter != null) {
                return typeAdapter;
            }
            for (NBTTypeAdapterFactory factory : factories) {
                NBTTypeAdapter candidate = factory.create(type, allowMutating);
                if (candidate != null) {
                    if (candidate instanceof NBTMutatingTypeAdapter) {
                        if (!allowMutating) {
                            throw new IllegalStateException("A factory returned a mutating type adapter despite them not being allowed");
                        }
                        this.mutatingTypeAdapter = (NBTMutatingTypeAdapter) candidate;
                    } else {
                        this.typeAdapter = candidate;
                    }
                    return candidate;
                }
            }
            throw new IllegalArgumentException("LadyLib does not know how to serialize " + type + " as NBT");
        }
    }

}
