package ladylib.registration.internal;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ladylib.LadyLib;
import ladylib.registration.AutoRegister;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Optional;

abstract class AutoRegistryRef<T extends AnnotatedElement> {
    static final LoadingCache<Class<?>, Optional<MethodHandle>> UNLOCALIZED_NAMES_CACHE = CacheBuilder.newBuilder()
            .build(CacheLoader.from(type -> {
                if (type == null) {
                    return Optional.empty();
                }
                try {
                    Method m;
                    // Items and blocks have different obfuscated names for their setTranslationKey method
                    if (Item.class.isAssignableFrom(type)) {
                        m = ReflectionHelper.findMethod(Item.class, "setTranslationKey", "func_77655_b", String.class);
                    } else if (Block.class.isAssignableFrom(type)) {
                        m = ReflectionHelper.findMethod(Block.class, "setTranslationKey", "func_149663_c", String.class);
                    } else    // If it has a setTranslationKey method, it is not from vanilla so not obfuscated
                    {
                        m = type.getMethod("setTranslationKey", String.class);
                    }
                    if (m != null) {
                        return Optional.of(MethodHandles.lookup().unreflect(m));
                    }
                } catch (NoSuchMethodException ignored) {
                } catch (IllegalAccessException e) {
                    LadyLib.LOGGER.error("Error while getting a getUnlocalizedName handle", e);
                }
                return Optional.empty();
            }));

    T referenced;
    String modId;
    private boolean listed;
    private boolean makeItemBlock;
    private String[] oldNames;
    private String[] oreNames;

    AutoRegistryRef(String modId, T referenced) {
        this.modId = modId;
        this.referenced = referenced;
        listed = !referenced.isAnnotationPresent(AutoRegister.Unlisted.class);
        makeItemBlock = !referenced.isAnnotationPresent(AutoRegister.NoItem.class);
        oldNames = referenced.isAnnotationPresent(AutoRegister.OldNames.class) ? referenced.getAnnotation(AutoRegister.OldNames.class).value() : new String[0];
        oreNames = referenced.isAnnotationPresent(AutoRegister.Ore.class) ? referenced.getAnnotation(AutoRegister.Ore.class).value() : new String[0];
    }


    /**
     * @return true if the item should appear in the creative and JEI tabs
     */
    boolean isListed() {
        return listed;
    }

    /**
     * @return every known name that this entry has had in previous versions
     */
    String[] getOldNames() {
        return oldNames;
    }

    String[] getOreNames() {
        return oreNames;
    }

    String getModId() {
        return modId;
    }

    /**
     * @return true if an item should be registered automatically for the underlying block referenced.
     * If the underlying referenced is not a block, this should be ignored.
     */
    boolean isMakeItemBlock() {
        return makeItemBlock;
    }

    abstract boolean isValidForRegistry(IForgeRegistry<?> registry);

    abstract <V extends IForgeRegistryEntry> V nameAndGet();

}
