package ladylib.capability.internal;

import ladylib.LadyLib;
import ladylib.nbt.NBTMutatingTypeAdapter;
import ladylib.nbt.NBTTypeAdapter;
import ladylib.nbt.TagAdapters;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class ReflectiveCapabilityStorage<C> implements Capability.IStorage<C> {
    private final List<FieldInfo> fieldInfos;

    ReflectiveCapabilityStorage(Class<C> clazz) throws IllegalAccessException {
        Field[] fields = clazz.getDeclaredFields();
        fieldInfos = new ArrayList<>();
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            // do not save transient fields
            if (Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) {
                continue;
            }

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

    @Nullable
    @Override
    public NBTBase writeNBT(Capability capability, Object instance, EnumFacing side) {
        // return null if nothing needs saving
        if (fieldInfos.isEmpty()) {
            return null;
        }
        NBTTagCompound compound = new NBTTagCompound();
        for (FieldInfo fieldInfo : fieldInfos) {
            try {
                Object value = fieldInfo.getter.invoke(instance);
                @SuppressWarnings("unchecked") NBTBase serialized = fieldInfo.adapter.toNBT(value);
                compound.setTag(fieldInfo.name, serialized);
            } catch (Throwable throwable) {
                LadyLib.LOGGER.error("Could not write NBT for capability " + capability.getName(), throwable);
            }
        }
        return compound;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readNBT(Capability capability, Object instance, EnumFacing side, NBTBase nbt) {
        if (!(nbt instanceof NBTTagCompound)) {
            throw new IllegalArgumentException("Expected a NBTTagCompound, got a " + NBTBase.getTagTypeName(nbt.getId()));
        }
        NBTTagCompound compound = (NBTTagCompound) nbt;
        for (FieldInfo fieldInfo : fieldInfos) {
            try {
                NBTBase serialized = compound.getTag(fieldInfo.name);
                if (fieldInfo.adapter instanceof NBTMutatingTypeAdapter) {
                    Object value = fieldInfo.getter.invoke(instance);
                    fieldInfo.adapter.fromNBT(value, serialized);
                } else if (fieldInfo.setter != null) {
                    Object value = fieldInfo.adapter.fromNBT(serialized);
                    fieldInfo.setter.invoke(instance, value);
                } else {
                    LadyLib.LOGGER.warn("Could not write to final field {} in capability instance {}", fieldInfo.name, instance);
                }
            } catch (Throwable throwable) {
                LadyLib.LOGGER.error("Could not read NBT from capability " + capability.getName(), throwable);
            }
        }
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
