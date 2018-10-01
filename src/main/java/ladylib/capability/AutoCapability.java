package ladylib.capability;

import ladylib.misc.CalledThroughReflection;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.capabilities.Capability;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoCapability {

    /**
     * The capability interface implemented by the annotated class, or <code>void</code>
     * if the class itself should be used for capability registration.
     */
    @CalledThroughReflection
    Class<?> value() default void.class;

    /**
     * The storage class for this capability implementation.
     * If left to <code>Capability.IStorage</code>, a default storage will be created
     * using LadyLib's NBT serialization system.
     */
    @CalledThroughReflection
    Class<? extends Capability.IStorage> storage() default Capability.IStorage.class;

    /**
     * A method annotated with this will be called when a {@link net.minecraftforge.event.AttachCapabilitiesEvent}
     * of the relevant type is fired.
     * The method should have no parameter.
     * It should return a predicate for either {@link Entity entities}, {@link ItemStack item stacks} or {@link TileEntity tile entities}.
     * The returned predicate should match for targets on which the capability should be attached.
     *
     * <p>
     * Example:<pre>
     *    &#64AutoCapability.AttachCapabilityCheckHandler("magicmod:mana")
     *     public static Predicate<Entity> isRelevantFor() {
     *         return o -> o instanceof EntityPlayer;
     *     }
     * </pre>
     *
     * <p>
     * If no check handler exists, the capability will not be attached automatically.
     *
     * @see #value()
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface AttachCapabilityCheckHandler {
        /**
         * Gets the key used to attach this capability to entities. <br>
         * The returned value will be used to construct a {@link net.minecraft.util.ResourceLocation}
         */
        String value();
    }

}
