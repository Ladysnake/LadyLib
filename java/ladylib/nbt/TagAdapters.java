package ladylib.nbt;

import com.google.gson.reflect.TypeToken;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;

public class TagAdapters {
    private static final List<NBTTypeAdapterFactory> factories = new ArrayList<>();
    private static final Map<TypeToken<?>, NBTTypeAdapter> cache = new HashMap<>();

    static {
        addPrimitiveFactory(boolean.class, Boolean.class, BaseTypeAdapters.BooleanAdapter::new);
        addPrimitiveFactory(byte.class,    Byte.class,    BaseTypeAdapters.ByteAdapter::new);
        addPrimitiveFactory(short.class,   Short.class,   BaseTypeAdapters.ShortAdapter::new);
        addPrimitiveFactory(int.class,     Integer.class, BaseTypeAdapters.IntAdapter::new);
        addPrimitiveFactory(float.class,   Float.class,   BaseTypeAdapters.FloatAdapter::new);
        addPrimitiveFactory(long.class,    Long.class,    BaseTypeAdapters.LongAdapter::new);
        addPrimitiveFactory(double.class,  Double.class,  BaseTypeAdapters.DoubleAdapter::new);

        addFactory(String.class,     BaseTypeAdapters.StringAdapter::new);
        addFactory(ItemStack.class,  BaseTypeAdapters.ItemStackAdapter::new);
        addFactory(BlockPos.class,   BaseTypeAdapters.BlockPosAdapter::new);
        addFactory(UUID.class,       BaseTypeAdapters.UUIDAdapter::new);

        factories.add(new SerializableNBTTypeAdapterFactory());
    }

    private static <T> void addPrimitiveFactory(Class<T> primitive, Class<T> wrapper, Supplier<NBTTypeAdapter<T, ?>> factory) {
        factories.add(type -> type.getRawType() == primitive || type.getRawType() == wrapper ? factory.get() : null);
    }

    private static <T> void addFactory(Class<T> primitive, Supplier<NBTTypeAdapter<T, ?>> factory) {
        factories.add(type -> type.getRawType() == primitive ? factory.get() : null);
    }

    public static NBTTypeAdapter getNBTAdapter(Field field) {
        return getNBTAdapter(TypeToken.get(field.getGenericType()));
    }

    public static NBTTypeAdapter getNBTAdapter(TypeToken type) {
        return cache.computeIfAbsent(type,
                tt -> {
                    for (NBTTypeAdapterFactory factory : factories) {
                        NBTTypeAdapter candidate = factory.create(tt);
                        if (candidate != null) {
                            return candidate;
                        }
                    }
                    throw new IllegalArgumentException("LadyLib does not know how to serialize " + tt + " as NBT");
                });
    }

}
