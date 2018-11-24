package ladylib.compat.internal;

import ladylib.LadyLib;
import ladylib.compat.EnhancedBusSubscriber;
import ladylib.compat.StateEventReceiver;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderException;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.asm.ModAnnotation;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

public class EnhancedAutomaticEventSubscriber {
    private static final Deque<StateEventReceiver> STATE_EVENT_RECEIVERS = new ArrayDeque<>();

    public static void inject(ASMDataTable data) {
        Set<ASMDataTable.ASMData> targets = data.getAll(EnhancedBusSubscriber.class.getName());
        ClassLoader mcl = Loader.instance().getModClassLoader();

        for (ASMDataTable.ASMData targ : targets) {
            try {
                List<ModAnnotation.EnumHolder> sidesEnum;
                List<String> requiredModIds;
                String ownerModId;
                Map<String, Object> annotationInfo = targ.getAnnotationInfo();
                if (annotationInfo.get("value") instanceof String) {
                    // Version 2.5+ of Ladylib, value now represents the owning mod id
                    //noinspection unchecked
                    requiredModIds = (List<String>) annotationInfo.get("dependencies");
                    ownerModId = (String) annotationInfo.get("value");
                } else {
                    // Older versions of Ladylib, value represents the required mods
                    //noinspection unchecked
                    requiredModIds = (List<String>) annotationInfo.get("value");
                    ownerModId = (String) annotationInfo.get("owner");
                }
                //noinspection unchecked
                sidesEnum = (List<ModAnnotation.EnumHolder>) annotationInfo.get("side");
                if (sidesEnum != null && sidesEnum.stream().map(ModAnnotation.EnumHolder::getValue).map(Side::valueOf).noneMatch(FMLCommonHandler.instance().getSide()::equals)) {
                    // wrong physical side
                    continue;
                }
                if (requiredModIds != null && !requiredModIds.stream().allMatch(Loader::isModLoaded)) {
                    // Missing some prerequisites
                    continue;
                }
                ModContainer current = Loader.instance().activeModContainer();
                ModContainer owner = Loader.instance().getIndexedModList().getOrDefault(ownerModId, current);
                LadyLib.LOGGER.debug("Registering @EnhancedBusSubscriber for {}", targ.getClassName());
                Class<?> subscriptionTarget = Class.forName(targ.getClassName(), true, mcl);
                Object instance = null;
                if (targ.getObjectName().equals(targ.getClassName())) {
                    // The class itself is annotated
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
                } else {
                    // It's a field inside the class
                    Field f = subscriptionTarget.getDeclaredField(targ.getObjectName());
                    if ((f.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) == 0) {
                        LadyLib.LOGGER.error("@EnhancedBusSubscriber can only be put on static final fields! (present on {})", f);
                        continue;
                    }
                    instance = f.get(null);
                }
                if (instance == null) {
                    LadyLib.LOGGER.error("The obtained instance is null, skipping event bus registration for {}", targ.getObjectName());
                }
                Loader.instance().setActiveModContainer(owner);
                MinecraftForge.EVENT_BUS.register(instance);
                Loader.instance().setActiveModContainer(current);
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
