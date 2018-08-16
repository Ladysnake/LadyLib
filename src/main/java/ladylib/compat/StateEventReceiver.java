package ladylib.compat;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;

/**
 * Marks a class as a receiver for basic {@link FMLStateEvent}.
 *
 * @apiNote While this can be used as an easy template for new mod classes, it is functionally useless on a class that
 * is not annotated with {@link EnhancedBusSubscriber}
 */
public interface StateEventReceiver {
    @Mod.EventHandler
    default void preInit(FMLPreInitializationEvent event) {
        // NO-OP
    }

    @Mod.EventHandler
    default void init(FMLInitializationEvent event) {
        // NO-OP
    }

    @Mod.EventHandler
    default void postInit(FMLPostInitializationEvent event) {
        // NO-OP
    }

    @Mod.EventHandler
    default void serverStarting(FMLServerStartingEvent event) {
        // NO-OP
    }
}
