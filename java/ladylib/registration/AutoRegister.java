package ladylib.registration;

import net.minecraft.block.Block;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

/**
 * AutoRegister can be used to automatically populate appropriate registries from the class' public static final fields.
 * It is effectively the counterpart to {@link net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder}
 *
 * <p>
 * Blocks will be automatically associated with a corresponding ItemBlock. Use {@link NoItem} to prevent this behaviour.
 * Items will be automatically added to the mod's creative tab. Use {@link Unlisted} to hide an item.
 * </p>
 *
 * @see Ignore
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoRegister {

    /**
     * The mod id for which the annotated element should automatically register stuff
     */
    String value();

    /**
     * If put on a referenced inside a class annotated with AutoRegister, that referenced will be excluded from automatic registration.
     * This allows you to exclude unwanted entries or to do a more specific registration yourself.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Ignore {
    }

    /**
     * If put on an Item or Block referenced, the corresponding item will not be added to the creative tab.
     * If JEI is installed, will also prevent the item from being displayed in its ingredient list.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Unlisted {
    }

    /**
     * If put on a Block referenced, no corresponding ItemBlock will be created
     * For more control over the ItemBlock creation, ignore the referenced
     * and directly call {@link BlockRegistrar#addBlock(Block, Function, boolean, String...)} instead
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface NoItem {
    }

    /**
     * If put on an Item or Block referenced, the corresponding item will automatically be added to the ore dictionary with
     * the given names
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Ore {
        String[] value();
    }

    /**
     * When put on a referenced, missing mappings that use one of the given old names will be remapped to this entry
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface OldNames {
        String[] value();
    }

}
