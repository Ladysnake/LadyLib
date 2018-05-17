package ladylib.capability;

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

    Class<?> type() default Object.class;

    Class<? extends Capability.IStorage> storage() default Capability.IStorage.class;

    /**
     * If set to false, LadyLib will not attempt to attach this capability during {@link net.minecraftforge.event.AttachCapabilitiesEvent}
     * @see AttachCapabilityCheckHandler
     */
    boolean attachAutomatically() default true;

    /**
     * A method annotated with this will be called when a {@link net.minecraftforge.event.AttachCapabilitiesEvent}
     * of the relevant type is fired.
     * The method should have no parameter.
     * It should return a predicate for either {@link Entity entities}, {@link ItemStack item stacks} or {@link TileEntity tile entities}.
     * The returned predicate should match for targets on which the capability should be attached.
     *
     * <p>
     * Example:
     * <pre>
     *    &nbsp;@AutoCapability.AttachCapabilityCheckHandler("magicmod:mana")
     *     public static Predicate<Entity> isRelevantFor() {
     *         return o -> o instanceof EntityPlayer;
     *     }
     * </pre>
     * </p>
     *
     * <p>
     * <strong> A checker method is required whenever attachAutomatically is set to true (the default)</strong>
     * </p>
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