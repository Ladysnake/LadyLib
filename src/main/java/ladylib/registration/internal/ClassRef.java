package ladylib.registration.internal;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nonnull;
import java.util.Locale;

// This may be used for entity automatic registration in the future
class ClassRef<T extends IForgeRegistryEntry<T>> extends AutoRegistryRef<Class<T>> {
    @Nonnull
    private T value;

    ClassRef(String modId, Class<T> type, @Nonnull T value) {
        super(modId, type);
        this.value = value;
    }

    @Override
    boolean isValidForRegistry(IForgeRegistry<?> registry) {
        return registry.getRegistrySuperType().isAssignableFrom(referenced);
    }

    @SuppressWarnings("unchecked")
    @Override
    IForgeRegistryEntry nameAndGet() {
        String name = referenced.getName().toLowerCase(Locale.ENGLISH);
        value.setRegistryName(new ResourceLocation(modId, name));
        return value;
    }
}
