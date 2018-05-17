package ladylib.nbt;

import com.google.gson.reflect.TypeToken;
import ladylib.LadyLib;
import ladylib.capability.internal.CapabilityRegistrar;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.logging.log4j.message.FormattedMessage;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class ReflectiveNBTAdapterFactory implements NBTTypeAdapterFactory<Object, NBTTagCompound> {
    public static final ReflectiveNBTAdapterFactory INSTANCE = new ReflectiveNBTAdapterFactory();

    @SuppressWarnings("unchecked")
    public <T> NBTTypeAdapter<T, NBTTagCompound> create(Class<T> type) {
        return (NBTTypeAdapter<T, NBTTagCompound>) create(TypeToken.get(type));
    }

    @Override
    public NBTTypeAdapter<Object, NBTTagCompound> create(TypeToken type) {
        try {
            return new ReflectiveNBTAdapter<>(type.getRawType());
        } catch (IllegalAccessException e) {
            throw new CapabilityRegistrar.UnableToCreateStorageException(e);
        }
    }

    public static class ReflectiveNBTAdapter<T> implements NBTMutatingTypeAdapter<T, NBTTagCompound> {
        private final List<FieldInfo> fieldInfos;

        public ReflectiveNBTAdapter(Class<?> clazz) throws IllegalAccessException {
            Field[] fields = clazz.getDeclaredFields();
            fieldInfos = new ArrayList<>();
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
                fieldInfos.add(new FieldInfo(field.getName(), getter, setter, adapter));
            }
        }

        @Override
        public NBTTagCompound toNBT(T instance) {
            // return null if nothing needs saving
            if (fieldInfos.isEmpty()) return null;
            NBTTagCompound compound = new NBTTagCompound();
            for (FieldInfo fieldInfo : fieldInfos) {
                try {
                    @SuppressWarnings("unchecked") T value = (T) fieldInfo.getter.invoke(instance);
                    @SuppressWarnings("unchecked") NBTBase serialized = fieldInfo.adapter.toNBT(value);
                    compound.setTag(fieldInfo.name, serialized);
                } catch (Throwable throwable) {
                    LadyLib.LOGGER.error(new FormattedMessage("Could not write NBT for {} ", instance), throwable);
                }
            }
            return compound;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T fromNBT(T instance, NBTTagCompound nbt) {
            for (FieldInfo fieldInfo : fieldInfos) {
                try {
                    NBTBase serialized = nbt.getTag(fieldInfo.name);
                    if (fieldInfo.adapter instanceof NBTMutatingTypeAdapter) {
                        T value = (T) fieldInfo.getter.invoke(instance);
                        fieldInfo.adapter.fromNBT(value, serialized);
                    } else if (fieldInfo.setter != null) {
                        T value = (T) fieldInfo.adapter.fromNBT(serialized);
                        fieldInfo.setter.invoke(instance, value);
                    }
                } catch (Throwable throwable) {
                    LadyLib.LOGGER.error(new FormattedMessage("Could not read NBT for {} ", instance), throwable);
                }
            }
            return instance;
        }

        private static class FieldInfo {
            private final String name;
            private final MethodHandle getter, setter;
            private final NBTTypeAdapter adapter;

            private FieldInfo(String name, MethodHandle getter, MethodHandle setter, NBTTypeAdapter adapter) {
                this.name = name;
                this.getter = getter;
                this.setter = setter;
                this.adapter = adapter;
            }
        }
    }
}
