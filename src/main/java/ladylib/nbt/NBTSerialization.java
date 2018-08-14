package ladylib.nbt;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import ladylib.nbt.serialization.NBTDeserializationException;
import ladylib.nbt.serialization.NBTTypeAdapter;
import ladylib.nbt.serialization.TagAdapters;
import net.minecraft.nbt.NBTBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;

/**
 * A set of methods to serialize and deserialize arbitrary objects using Minecraft's Non Binary Tag format.
 * <p>
 * Works very similarly to GSON, but using static methods.
 */
public final class NBTSerialization {
    private NBTSerialization() { }


    /**
     * This method serializes the specified object into its equivalent Non Binary Tag representation.
     * This method should be used when the specified object is not a generic type. This method uses
     * {@link Class#getClass()} to get the type for the specified object, but the
     * {@code getClass()} loses the generic type information because of the Type Erasure feature
     * of Java. Note that this method works fine if the any of the object fields are of generic type,
     * just the object itself should not be of a generic type. If the object is of generic type, use
     * {@link #toNBT(Object, Type)} instead.
     *
     * @param src the object for which NBT representation is to be created
     * @return NBT representation of {@code src}.
     */
    @Nullable
    public static NBTBase toNBT(@Nullable Object src) {
        return src == null ? null : toNBT(src, src.getClass());
    }

    /**
     * This method serializes the specified object, including those of generic types, into its
     * equivalent NBT representation. This method must be used if the specified object is a generic
     * type. For non-generic objects, use {@link #toNBT(Object)} instead.
     *
     * @param src       the object for which NBT representation is to be created
     * @param typeOfSrc The specific genericized type of src. You can obtain
     *                  this type by using the {@link com.google.gson.reflect.TypeToken} class. For example,
     *                  to get the type for {@code Collection<Foo>}, you should use:
     *                  <pre>Type typeOfSrc = new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();</pre>
     * @return NBT representation of {@code src}
     *
     * @see #serializeNBT(Object)
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static NBTBase toNBT(Object src, Type typeOfSrc) {
        NBTTypeAdapter adapter = TagAdapters.getNBTAdapter(TypeToken.get(typeOfSrc), false);
        return adapter.toNBT(src);
    }

    /**
     * This method deserializes the NBT read from the specified parse tree into an object of the
     * specified type. It is not suitable to use if the specified class is a generic type since it
     * will not have the generic type information because of the Type Erasure feature of Java.
     * Therefore, this method should not be used if the desired type is a generic type. Note that
     * this method works fine if the any of the fields of the specified object are generics, just the
     * object itself should not be a generic type. For the cases when the object is of generic type,
     * invoke {@link #fromNBT(NBTBase, Type)}.
     *
     * @param <T>      the type of the desired object
     * @param nbt      the root of the NBT data structure from which the object is to
     *                 be deserialized
     * @param classOfT The class of T
     * @return an object of type T from the NBT. Returns {@code null} if {@code NBT} is {@code null}.
     */
    @Nullable
    public static <T> T fromNBT(@Nullable NBTBase nbt, Class<T> classOfT) throws NBTDeserializationException {
        return fromNBT(nbt, (Type) classOfT);
    }

    /**
     * This method deserializes the specified NBT data structure into an object of the
     * specified type. This method is useful if the specified object is a generic type.
     *
     * @apiNote NBT can be altered by a lot of external factors, your code should handle
     * deserialization failures adequately.
     *
     * @param <T>     the type of the desired object
     * @param nbt     the root of the NBT data structure from which the object is to
     *                be deserialized
     * @param typeOfT The specific genericized type of src. You can obtain this type by using the
     *                {@link TypeToken} class. <br>
     *                For example, to get the type for
     *                {@code Collection<Foo>}, you should use:
     *                <pre>Type typeOfT = new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();</pre>
     * @return an object of type T from the NBT. Returns {@code null} if {@code nbt} is {@code null}.
     * @throws NBTDeserializationException if an error occurred during the deserialization.
     *
     * @see #deserializeNBT(Object, NBTBase)
     *
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T fromNBT(@Nullable NBTBase nbt, Type typeOfT) throws NBTDeserializationException {
        if (nbt == null) {
            return null;
        }
        Preconditions.checkNotNull(typeOfT);
        NBTTypeAdapter adapter = TagAdapters.getNBTAdapter(TypeToken.get(typeOfT), false);
        return (T) adapter.fromNBT(nbt);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static NBTBase serializeNBT(@Nonnull Object src) {
        NBTTypeAdapter adapter = TagAdapters.getNBTAdapter(TypeToken.get(src.getClass()), true);
        return adapter.toNBT(src);
    }

    @SuppressWarnings("unchecked")
    public static void deserializeNBT(@Nonnull Object target, @Nullable NBTBase nbt) throws NBTDeserializationException {
        if (nbt == null) {
            return;
        }
        NBTTypeAdapter adapter = TagAdapters.getNBTAdapter(TypeToken.get(target.getClass()), true);
        adapter.fromNBT(target, nbt);
    }
}
