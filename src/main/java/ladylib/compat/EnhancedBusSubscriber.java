package ladylib.compat;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLStateEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * An equivalent of {@link net.minecraftforge.fml.common.Mod.EventBusSubscriber} that can propagate {@link FMLStateEvent}
 * and can take a list of required mods as a precondition.
 * <p>
 * Unlike the former,
 * a) an instance of your class will be subscribed instead of the class itself and
 * b) it will be subscribed to the event bus during {@link FMLPreInitializationEvent} instead of at construction time.
 * <p>
 * As this annotation registers an instance of the class, such an instance needs to be available.
 * If a <code>static final</code> field named <tt>INSTANCE</tt> exists in the class, it will be subscribed.
 * Otherwise, a default constructor needs to be available (no matter the visibility).
 * <p>
 * If you want your class to receive state events, implement {@link StateEventReceiver}.
 * </p>
 */
public @interface EnhancedBusSubscriber {

    /**
     * A list of mod IDs that this class requires to be loaded.
     * If any of the mods is missing, the class will not be loaded nor subscribed.
     */
    String[] value() default "";

    /**
     * The physical sides on which this class can be subscribed to the event bus.
     */
    Side[] side() default {Side.CLIENT, Side.SERVER};

}
