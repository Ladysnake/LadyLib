package ladylib.compat;

import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.relauncher.Side;

/**
 * An equivalent of {@link net.minecraftforge.fml.common.Mod.EventBusSubscriber} that redistributes {@link FMLStateEvent}
 * and can take a list of required mods as a precondition. Unlike the former, a) an instance of your class will be subscribed
 * and b) it will be subscribed to the event bus during {@link FMLPreInitializationEvent}
 * <p>
 * If you want your class to receive state events, implement {@link StateEventReceiver} and leave a default constructor
 * </p>
 */
public @interface EnhancedBusSubscriber {

    /**
     * A list of mod IDs that this class requires to be loaded
     */
    String[] value() default "";

    /**
     * The physical sides on which this class can be subscribed to the event bus.
     */
    Side[] side() default {Side.CLIENT, Side.SERVER};

    interface StateEventReceiver {
        default void preInit(FMLPreInitializationEvent event) {
            // NO-OP
        }

        default void init(FMLInitializationEvent event) {
            // NO-OP
        }

        default void postInit(FMLPostInitializationEvent event) {
            // NO-OP
        }

        default void serverStarting(FMLServerStartingEvent event) {
            // NO-OP
        }
    }
}
