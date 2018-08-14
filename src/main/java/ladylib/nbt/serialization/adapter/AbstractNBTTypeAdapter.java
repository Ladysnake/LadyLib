package ladylib.nbt.serialization.adapter;

import com.google.gson.reflect.TypeToken;
import ladylib.nbt.serialization.NBTDeserializationException;
import ladylib.nbt.serialization.NBTTypeAdapter;
import ladylib.nbt.serialization.TagAdapters;
import net.minecraft.nbt.NBTBase;
import org.jetbrains.annotations.Contract;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Function;

public abstract class AbstractNBTTypeAdapter<T, NBT extends NBTBase> implements NBTTypeAdapter<T, NBT> {

    protected final TypeToken<T> typeToken;

    @SuppressWarnings("unchecked")
    protected AbstractNBTTypeAdapter() {
        Type superclass = getClass().getGenericSuperclass();
        Type superclassType;
        if (superclass instanceof ParameterizedType) {
            superclassType = ((ParameterizedType) superclass).getActualTypeArguments()[0];
        } else {
            throw new IllegalArgumentException("Missing specific type parameter. Use the explicit constructor instead.");
        }
        typeToken = (TypeToken<T>) TypeToken.get(superclassType);
    }

    protected AbstractNBTTypeAdapter(TypeToken<T> typeToken) {
        this.typeToken = typeToken;
    }

    /**
     * Helper function to cast passed in NBT to the relevant type
     */
    @Contract(pure = true)
    protected Optional<NBT> cast(NBTBase nbt, Class<NBT> clazz) {
        return NBTTypeAdapter.castNBT(nbt, clazz);
    }

    /**
     * If the passed nbt is of the right type, casts it and applies the given conversion.
     * Otherwise, returns a default value.
     * @throws NBTDeserializationException if the NBT data is of the wrong type and no default value is available
     */
    protected T castAnd(NBTBase nbt, Class<NBT> clazz, Function<NBT, T> conversion) {
        return cast(nbt, clazz)
                .map(conversion)
                .orElseGet(() -> TagAdapters.getDefaultValue(typeToken)
                        .orElseThrow(() -> new NBTDeserializationException("Expected an instance of " + clazz.getName() + " but " + nbt + " is of type " + nbt.getClass().getName()))
                );
    }

}
