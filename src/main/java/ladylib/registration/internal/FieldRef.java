package ladylib.registration.internal;

import ladylib.LadyLib;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

class FieldRef extends AutoRegistryRef<Field> {

    FieldRef(String modId, Field field) {
        super(modId, field);
    }

    @Override
    boolean isValidForRegistry(IForgeRegistry<?> registry) {
        return registry.getRegistrySuperType().isAssignableFrom(referenced.getType());
    }

    @Override
    IForgeRegistryEntry nameAndGet() {
        try {
            String name = referenced.getName().toLowerCase(Locale.ENGLISH);
            IForgeRegistryEntry value = ((IForgeRegistryEntry) referenced.get(null));
            if (value == null) {
                LadyLib.LOGGER.error("A referenced marked to be automatically registered has a null value {}", referenced);
                return null;
            }
            value.setRegistryName(new ResourceLocation(modId, name));
            UNLOCALIZED_NAMES_CACHE.get(referenced.getType()).ifPresent(handle -> {
                try {
                    handle.invoke(value, modId + "." + name);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            });
            return value;
        } catch (ExecutionException | IllegalAccessException e) {
            LadyLib.LOGGER.error("Could not access an auto registered reference", e);
        }
        return null;
    }
}
