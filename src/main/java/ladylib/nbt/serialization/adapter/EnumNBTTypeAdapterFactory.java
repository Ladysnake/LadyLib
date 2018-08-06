package ladylib.nbt.serialization.adapter;

import com.google.gson.reflect.TypeToken;
import ladylib.nbt.serialization.NBTDeserializationException;
import ladylib.nbt.serialization.NBTTypeAdapter;
import ladylib.nbt.serialization.NBTTypeAdapterFactory;
import ladylib.nbt.serialization.TagAdapters;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagString;

public class EnumNBTTypeAdapterFactory implements NBTTypeAdapterFactory<Enum, NBTTagString> {
    @SuppressWarnings("unchecked")
    @Override
    public NBTTypeAdapter<Enum, NBTTagString> create(TypeToken type, boolean allowMutating) {
        Class enumClass = type.getRawType();
        if (!Enum.class.isAssignableFrom(enumClass) || enumClass == Enum.class) {
            return null;
        }
        if (!enumClass.isEnum()) {
            enumClass = enumClass.getSuperclass(); // handle anonymous subclasses
        }
        return new EnumNBTTypeAdapter(type, enumClass);
    }

    public static class EnumNBTTypeAdapter<E extends Enum<E>> extends AbstractNBTTypeAdapter<E, NBTTagString> {

        private final Class<E> enumClass;

        public EnumNBTTypeAdapter(TypeToken<E> tt, Class<E> enumClass) {
            super(tt);
            this.enumClass = enumClass;
        }

        @Override
        public NBTTagString toNBT(E value) {
            return new NBTTagString(value.name());
        }

        @Override
        public E fromNBT(NBTBase nbtTagString) {
            try {
                return castAnd(nbtTagString, NBTTagString.class, nbt -> Enum.valueOf(enumClass, nbt.getString()));
            } catch (IllegalArgumentException e) {
                TypeToken<E> typeToken = TypeToken.get(enumClass);
                return TagAdapters.getDefaultValue(typeToken).orElseThrow(() -> new NBTDeserializationException("Failed to deserialize enum field", e));
            }
        }
    }
}
