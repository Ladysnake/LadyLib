package ladylib.compat.internal;

import ladylib.LadyLib;
import ladylib.compat.EnhancedBusSubscriber;
import ladylib.compat.StateEventReceiver;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderException;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.asm.ModAnnotation;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EnhancedAutomaticEventSubscriber {
    private static final List<StateEventReceiver> STATE_EVENT_RECEIVERS = new ArrayList<>();

    public static void inject(ASMDataTable data) {
        Set<ASMDataTable.ASMData> targets = data.getAll(EnhancedBusSubscriber.class.getName());
        ClassLoader mcl = Loader.instance().getModClassLoader();

        for (ASMDataTable.ASMData targ : targets) {
            try {
                //noinspection unchecked
                @SuppressWarnings("unchecked")
                List<ModAnnotation.EnumHolder> sidesEnum = (List<ModAnnotation.EnumHolder>) targ.getAnnotationInfo().get("side");
                if (sidesEnum != null && sidesEnum.stream().map(ModAnnotation.EnumHolder::getValue).map(Side::valueOf).noneMatch(FMLCommonHandler.instance().getSide()::equals)) {
                    // wrong physical side
                    return;
                }
                @SuppressWarnings("unchecked")
                List<String> requiredModIds = (List<String>) targ.getAnnotationInfo().get("value");
                if (requiredModIds != null && !requiredModIds.stream().allMatch(Loader::isModLoaded)) {
                    // Missing some prerequisites
                    return;
                }
                LadyLib.LOGGER.debug("Registering @EventBusSubscriber for {}", targ.getClassName());
                Class<?> subscriptionTarget = Class.forName(targ.getClassName(), true, mcl);
                Object instance = null;
                // Search for an existing instance field
                for (Field f : subscriptionTarget.getDeclaredFields()) {
                    if (isInstanceField(f, subscriptionTarget)) {
                        f.setAccessible(true);
                        instance = f.get(null);
                        break;
                    }
                }
                // Create the instance as none already exists
                if (instance == null) {
                    Constructor<?> constructor = subscriptionTarget.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    instance = constructor.newInstance();
                }
                MinecraftForge.EVENT_BUS.register(instance);
                if (instance instanceof StateEventReceiver) {
                    STATE_EVENT_RECEIVERS.add((StateEventReceiver) instance);
                }
                LadyLib.LOGGER.debug("Injected @EventBusSubscriber class {}", targ.getClassName());
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                LadyLib.LOGGER.error("The class {} cannot be instantiated. Please add a valid constructor or an instance field", targ.getClassName(), e);
                throw new LoaderException(e);
            } catch (ExceptionInInitializerError e) {
                LadyLib.LOGGER.error("Class {} errored during static initialization", targ.getClassName(), e);
                throw e;
            } catch (Throwable e) {
                LadyLib.LOGGER.error("An error occurred trying to load an EventBusSubscriber {}", targ.getClassName(), e);
                throw new LoaderException(e);
            }
        }
    }

    private static boolean isInstanceField(Field f, Class<?> subscriptionTarget) {
        return f.getName().equalsIgnoreCase("instance") && subscriptionTarget.isAssignableFrom(f.getType())
                && Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers());
    }

    public static void redistributeEvent(FMLStateEvent event) {
        // Doing it like this because only 4 events and lazy
        for (StateEventReceiver receiver : STATE_EVENT_RECEIVERS) {
            if (event instanceof FMLPreInitializationEvent) {
                receiver.preInit((FMLPreInitializationEvent) event);
            } else if (event instanceof FMLInitializationEvent) {
                receiver.init((FMLInitializationEvent) event);
            } else if (event instanceof FMLPostInitializationEvent) {
                receiver.postInit((FMLPostInitializationEvent) event);
            } else if (event instanceof FMLServerStartingEvent) {
                receiver.serverStarting((FMLServerStartingEvent) event);
            }
        }
    }
}
