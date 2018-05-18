package ladylib.nbt;

import com.google.gson.reflect.TypeToken;
import ladylib.LadyLib;
import ladylib.capability.internal.CapabilityRegistrar;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.logging.log4j.message.FormattedMessage;
import sun.reflect.ReflectionFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class ReflectiveNBTAdapterFactory implements NBTTypeAdapterFactory<Object, NBTTagCompound> {
    public static final ReflectiveNBTAdapterFactory INSTANCE = new ReflectiveNBTAdapterFactory();

    @SuppressWarnings("unchecked")
    public <T> NBTTypeAdapter<T, NBTTagCompound> create(Class<T> type) {
        return (NBTTypeAdapter<T, NBTTagCompound>) create(TypeToken.get(type), true);
    }

    @Override
    public NBTTypeAdapter<Object, NBTTagCompound> create(TypeToken type, boolean allowMutating) {
        try {
            MutatingReflectiveNBTAdapter<Object> ret = new MutatingReflectiveNBTAdapter<>(type.getRawType());
            if (!allowMutating) {
                return new ReflectiveNBTAdapter<>(type.getRawType(), ret);
            }
            return ret;
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new CapabilityRegistrar.UnableToGetFactoryException(e);
        }
    }

    public static class ReflectiveNBTAdapter<T> implements NBTTypeAdapter<T, NBTTagCompound> {
        private final MutatingReflectiveNBTAdapter<T> delegate;
        private final MethodHandle constructor;

        public ReflectiveNBTAdapter(Class<?> clazz, MutatingReflectiveNBTAdapter<T> delegate) throws NoSuchMethodException, IllegalAccessException {
            this.delegate = delegate;
            Constructor temp = clazz.getDeclaredConstructor();
            temp.setAccessible(true);
            constructor = MethodHandles.lookup().unreflectConstructor(temp);
        }

        @Override
        public NBTTagCompound toNBT(T value) {
            return delegate.toNBT(value);
        }

        @Override
        public T fromNBT(NBTBase nbtTagCompound) {
            try {
                @SuppressWarnings("unchecked") T ret = (T) constructor.invoke();
                return delegate.fromNBT(ret, nbtTagCompound);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    public static class MutatingReflectiveNBTAdapter<T> implements NBTMutatingTypeAdapter<T, NBTTagCompound> {
        private final List<FieldEntry> fieldEntries;

        public MutatingReflectiveNBTAdapter(Class<?> clazz) throws IllegalAccessException {
            Field[] fields = clazz.getDeclaredFields();
            fieldEntries = new ArrayList<>();
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            for (Field field : fields) {
                int modifiers = field.getModifiers();
                // do not save transient fields
                if (Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) continue;

                field.setAccessible(true);
                MethodHandle getter = lookup.unreflectGetter(field);
                MethodHandle setter = null;
                if (!Modifier.isFinal(modifiers)) {
                    setter = lookup.unreflectSetter(field);
                }
                NBTTypeAdapter adapter = TagAdapters.getNBTAdapter(field);
                fieldEntries.add(new FieldEntry(field.getName(), getter, setter, adapter));
            }
        }

        @Override
        public NBTTagCompound toNBT(T instance) {
            // return null if nothing needs saving
            if (fieldEntries.isEmpty()) return null;
            NBTTagCompound compound = new NBTTagCompound();
            for (FieldEntry fieldEntry : fieldEntries) {
                try {
                    @SuppressWarnings("unchecked") T value = (T) fieldEntry.getter.invoke(instance);
                    @SuppressWarnings("unchecked") NBTBase serialized = fieldEntry.adapter.toNBT(value);
                    compound.setTag(fieldEntry.name, serialized);
                } catch (Throwable throwable) {
                    LadyLib.LOGGER.error(new FormattedMessage("Could not write NBT for {} ", instance), throwable);
                }
            }
            return compound;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T fromNBT(T instance, NBTBase nbt) {
            for (FieldEntry fieldEntry : fieldEntries) {
                try {
                    NBTBase serialized = cast(nbt, NBTTagCompound.class).getTag(fieldEntry.name);
                    if (fieldEntry.adapter instanceof NBTMutatingTypeAdapter) {
                        T value = (T) fieldEntry.getter.invoke(instance);
                        fieldEntry.adapter.fromNBT(value, serialized);
                    } else if (fieldEntry.setter != null) {
                        T value = (T) fieldEntry.adapter.fromNBT(serialized);
                        fieldEntry.setter.invoke(instance, value);
                    } else {
                        LadyLib.LOGGER.warn("Could not write to final field {} in {}", fieldEntry.name, instance);
                    }
                } catch (Throwable throwable) {
                    LadyLib.LOGGER.error(new FormattedMessage("Could not read NBT for {} ", instance), throwable);
                }
            }
            return instance;
        }
    }

    private static class FieldEntry {
        private final String name;
        private final MethodHandle getter, setter;
        private final NBTTypeAdapter adapter;

        private FieldEntry(String name, MethodHandle getter, MethodHandle setter, NBTTypeAdapter adapter) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            this.adapter = adapter;
        }
    }
}
