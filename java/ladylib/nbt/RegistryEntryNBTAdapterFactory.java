package ladylib.nbt;

import com.google.gson.reflect.TypeToken;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryManager;

public class RegistryEntryNBTAdapterFactory implements NBTTypeAdapterFactory<IForgeRegistryEntry, NBTTagString> {
    @Override
    public NBTTypeAdapter<IForgeRegistryEntry, NBTTagString> create(TypeToken type, boolean allowMutating) {
        if (!IForgeRegistryEntry.class.isAssignableFrom(type.getRawType())) return null;
        @SuppressWarnings("unchecked") IForgeRegistry registry = RegistryManager.ACTIVE.getRegistry(type.getRawType());
        if (registry == null) {
            return null;
        }
        @SuppressWarnings("unchecked") NBTTypeAdapter<IForgeRegistryEntry, NBTTagString> ret =
                new RegistryEntryNBTAdapter<>(registry);
        return ret;
    }

    public static class RegistryEntryNBTAdapter<V extends IForgeRegistryEntry<V>> implements NBTTypeAdapter<V, NBTTagString> {
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
            return registry.getValue(new ResourceLocation(cast(nbtTagString, NBTTagString.class).getString()));
        }
    }
}
