package ladylib.nbt;

import com.google.gson.reflect.TypeToken;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryManager;

import javax.annotation.Nullable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class RegistryEntryNBTAdapterFactory implements NBTTypeAdapterFactory<IForgeRegistryEntry, NBTTagString> {
    @Override
    public NBTTypeAdapter<IForgeRegistryEntry, NBTTagString> create(TypeToken type, boolean allowMutating) {
        if (!IForgeRegistryEntry.class.isAssignableFrom(type.getRawType())) return null;
        @SuppressWarnings("unchecked") IForgeRegistry registry = RegistryManager.ACTIVE.getRegistry(getRegistryType(type.getRawType()));
        if (registry == null) {
            return null;
        }
        @SuppressWarnings("unchecked") NBTTypeAdapter<IForgeRegistryEntry, NBTTagString> ret =
                new RegistryEntryNBTAdapter<>(registry);
        return ret;
    }

    @Nullable
    private Class getRegistryType(Class clazz) {
        Type superclass = clazz.getGenericSuperclass();
        // In 99% of cases, registry entries extend a generic implementation
        if (superclass instanceof ParameterizedType) {
            Type superclassType = ((ParameterizedType) superclass).getActualTypeArguments()[0];
            TypeToken typeToken = TypeToken.get(superclassType);
            Class typeClass = typeToken.getRawType();
            // we have found the implementation
            if (IForgeRegistryEntry.class.isAssignableFrom(TypeToken.get(superclass).getRawType()) &&
                IForgeRegistryEntry.class.isAssignableFrom(typeClass)) {
                return typeClass;
            }
        }
        // we can still go up
        if (IForgeRegistryEntry.class.isAssignableFrom(clazz.getSuperclass())) {
            return getRegistryType(clazz.getSuperclass());
        }
        // This class does not subclass a generic implementation so
        // it directly implements either IForgeRegistryEntry or a sub-interface.
        for (Type interfaceType : clazz.getGenericInterfaces()) {
            Class interfaceClass = TypeToken.get(interfaceType).getRawType();
            if (interfaceType instanceof ParameterizedType) {
                Type superclassType = ((ParameterizedType) interfaceType).getActualTypeArguments()[0];
                TypeToken typeToken = TypeToken.get(superclassType);
                Class typeClass = typeToken.getRawType();
                // we have found the generic interface, return its type argument
                if (IForgeRegistryEntry.class.isAssignableFrom(interfaceClass) &&
                        IForgeRegistryEntry.class.isAssignableFrom(typeClass)) {
                    return typeClass;
                }
            }
            // Some people may implement an interface that extends IForgeRegistryEntry with defined type arguments
            Class registryType = getRegistryType(interfaceClass);
            if (registryType != null) {
                return registryType;
            }
        }
        // reached a dead end
        return null;
    }

    public static class RegistryEntryNBTAdapter<V extends IForgeRegistryEntry<V>> extends AbstractNBTTypeAdapter<V, NBTTagString> {
        private IForgeRegistry<V> registry;

        public RegistryEntryNBTAdapter(IForgeRegistry<V> registry) {
            this.registry = registry;
        }

        @Override
        public NBTTagString toNBT(V value) {
            return new NBTTagString(String.valueOf(value.getRegistryName()));
        }

        @Override
        public V fromNBT(NBTBase nbtTagString) {
            return castAnd(nbtTagString, NBTTagString.class, nbt -> registry.getValue(new ResourceLocation(nbt.getString())));
        }
    }
}
