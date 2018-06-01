package ladylib.nbt;

import com.google.gson.reflect.TypeToken;
import ladylib.LadyLib;
import net.minecraft.nbt.NBTBase;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;

/**
 * This only handles the base capability class, as it is the only one that is guaranteed
 * to be covered by the default storage implementation
 */
public class CapabilityNBTTypeAdapterFactory<C> implements NBTTypeAdapterFactory<C, NBTBase> {
    private IdentityHashMap<String, Capability<?>> providers;

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public NBTTypeAdapter<C, NBTBase> create(TypeToken type, boolean allowMutating) {
        if (!allowMutating) {
            return null;
        }
        if (providers == null) {
            try {
                Field f = CapabilityManager.class.getDeclaredField("providers");
                f.setAccessible(true);
                providers = (IdentityHashMap<String, Capability<?>>) f.get(CapabilityManager.INSTANCE);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                LadyLib.LOGGER.error("Could not get capability providers", e);
                return null;
            }
        }
        Capability<?> capability = providers.get(type.getRawType().getName().intern());
        if (capability != null) {
            return new CapabilityNBTTypeAdapter(capability);
        }
        return null;
    }

    public static class CapabilityNBTTypeAdapter<C> implements NBTMutatingTypeAdapter<C, NBTBase> {
        private final Capability<C> capability;

        public CapabilityNBTTypeAdapter(Capability<C> capability) {
            this.capability = capability;
        }

        @Override
        public NBTBase toNBT(C value) {
            return capability.writeNBT(value, null);
        }

        @Override
        public C fromNBT(C value, NBTBase nbt) {
            capability.readNBT(value, null, nbt);
            return value;
        }
    }
}
