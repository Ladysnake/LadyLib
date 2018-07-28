package ladylib.registration.internal;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ladylib.LadyLib;
import ladylib.misc.ReflectionUtil;
import ladylib.registration.AutoRegister;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Optional;

abstract class AutoRegistryRef<T extends AnnotatedElement> {
    static final LoadingCache<Class<?>, Optional<MethodHandle>> UNLOCALIZED_NAMES_CACHE = CacheBuilder.newBuilder()
            .build(new CacheLoader<Class<?>, Optional<MethodHandle>>() {
                @Override
                public Optional<MethodHandle> load(@Nullable Class<?> type) {
                    if (type == null) {
                        return Optional.empty();
                    }
                    try {
                        Method m;
                        // Items and blocks have different obfuscated names for their setTranslationKey method
                        if (Item.class.isAssignableFrom(type)) {
                            m = findSetTranslationKey(Item.class, "func_77655_b");
                        } else if (Block.class.isAssignableFrom(type)) {
                            m = findSetTranslationKey(Block.class, "func_149663_c");
                        } else {
                            // If it has a setTranslationKey method, it is not from vanilla so not obfuscated
                            m = findSetTranslationKey(type, "setTranslationKey");
                            if (m == null) {    // account for older naming schemes
                                m = findSetTranslationKey(type, "setUnlocalizedName");
                            }
                        }
                        if (m != null) {
                            return Optional.of(MethodHandles.lookup().unreflect(m));
                        }
                    } catch (IllegalAccessException e) {
                        LadyLib.LOGGER.error("Error while getting a setUnlocalizedName handle", e);
                    }
                    return Optional.empty();
                }

                private Method findSetTranslationKey(Class<?> clazz, String obfName) {
                    try {
                        return ReflectionUtil.findMethodFromObfName(clazz, obfName, clazz, String.class);
                    } catch (ReflectionHelper.UnableToFindMethodException e) {
                        return null;
                    }
                }
            });

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

    abstract IForgeRegistryEntry nameAndGet();

}
