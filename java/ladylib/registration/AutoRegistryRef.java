package ladylib.registration;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ladylib.LadyLib;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

class AutoRegistryRef {
    private final LoadingCache<Class<?>, Optional<MethodHandle>> unlocalizedNamesCache = CacheBuilder.newBuilder()
            .build(CacheLoader.from(type -> {
                if (type == null) return Optional.empty();
                try {
                    Method m;
                    // Items and blocks have different obfuscated names for their setUnlocalizedName method
                    if (Item.class.isAssignableFrom(type))
                        m = ReflectionHelper.findMethod(type, "setUnlocalizedName", "func_77655_b", String.class);
                    else if (Block.class.isAssignableFrom(type))
                        m = ReflectionHelper.findMethod(type, "setUnlocalizedName", "func_149663_c", String.class);
                    else    // If it has a setUnlocalizedName method, it is not from vanilla so not obfuscated
                        m = type.getMethod("setUnlocalizedName", String.class);
                    if (m != null)
                        return Optional.of(MethodHandles.lookup().unreflect(m));
                } catch (NoSuchMethodException ignored) {
                } catch (IllegalAccessException e) {
                    LadyLib.LOGGER.error("Error while getting a getUnlocalizedName handle", e);
                }
                return Optional.empty();
    }));

    private Field field;
    private boolean listed;
    private boolean makeItemBlock;
    private String[] oreNames;
    private String modId;

    AutoRegistryRef(String modId, Field field) {
        this.modId = modId;
        this.field = field;
        listed = !field.isAnnotationPresent(AutoRegister.Unlisted.class);
        makeItemBlock = !field.isAnnotationPresent(AutoRegister.NoItem.class);
        oreNames = field.isAnnotationPresent(AutoRegister.Ore.class) ? field.getAnnotation(AutoRegister.Ore.class).value() : new String[0];
    }

    boolean isValidForRegistry(IForgeRegistry<?> registry) {
        return registry.getRegistrySuperType().isAssignableFrom(field.getType());
    }

    @SuppressWarnings("unchecked")
    <V extends IForgeRegistryEntry> V nameAndGet() {
        try {
            String name = field.getName().toLowerCase(Locale.ENGLISH);
            IForgeRegistryEntry value = ((IForgeRegistryEntry) field.get(null));
            if (value == null) {
                LadyLib.LOGGER.error("A field marked to be automatically registered has a null value {}", field);
                return null;
            }
            value.setRegistryName(new ResourceLocation(modId, name));
            unlocalizedNamesCache.get(field.getType()).ifPresent(handle -> {
                try {
                    handle.invoke(value, modId + "." + name);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            });
            return (V) value;
        } catch (ExecutionException | IllegalAccessException e) {
            LadyLib.LOGGER.error("Could not access an auto registered field", e);
        }
        return null;
    }

    /**
     * @return true if the item should appear in the creative and JEI tabs
     */
    boolean isListed() {
        return listed;
    }

    String[] getOreNames() {
        return oreNames;
    }

    /**
     * @return true if an item should be registered automatically for the underlying block field.
     * If the underlying field is not a block, this should be ignored.
     */
    boolean isMakeItemBlock() {
        return makeItemBlock;
    }
}
