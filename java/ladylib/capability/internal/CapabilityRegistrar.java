package ladylib.capability.internal;

import ladylib.LadyLib;
import ladylib.capability.AutoCapability;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import org.apache.logging.log4j.message.FormattedMessage;

import java.lang.invoke.*;
import java.lang.reflect.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public class CapabilityRegistrar {

    public void findRegistryHandlers(ASMDataTable asmData) {
        // find all classes that will be handled by this registrar
        Set<ASMDataTable.ASMData> allRegistryHandlers = asmData.getAll(AutoCapability.class.getName());
        CapabilityEventHandler handler = new CapabilityEventHandler();
        try {
            Field providersField = CapabilityManager.class.getDeclaredField("providers");
            providersField.setAccessible(true);
            @SuppressWarnings("unchecked") Map<String, Capability<?>> providers =
                    (Map<String, Capability<?>>) providersField.get(CapabilityManager.INSTANCE);
            for (ASMDataTable.ASMData data : allRegistryHandlers) {
                String className = data.getClassName();
                try {
                    Class<?> clazz = Class.forName(className, false, getClass().getClassLoader());
                    createRegisterCapability(clazz, providers, handler);
                } catch (IllegalArgumentException | UnableToGetFactoryException | ClassNotFoundException e) {
                    LadyLib.LOGGER.error(new FormattedMessage("Could not register a capability for the class {}", className), e);
                }
            }
            MinecraftForge.EVENT_BUS.register(handler);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LadyLib.LOGGER.fatal("Unable to process capabilities", e);
        }
    }

    private <T> void createRegisterCapability(Class<T> clazz, Map<String, Capability<?>> providers, CapabilityEventHandler handler) {
        Callable<T> factory = createFactory(clazz, "call", Callable.class);
        Capability.IStorage<T> storage = createStorage(clazz);
        CapabilityManager.INSTANCE.register(clazz, storage, factory);
        AutoCapability annotation = clazz.getAnnotation(AutoCapability.class);
        if (annotation.attachAutomatically()) {
            @SuppressWarnings("unchecked") Capability<T> capability = (Capability<T>) providers.get(clazz.getName().intern());
            addAttachHandlers(capability, clazz, factory, handler);
        }
    }

    private <T> void addAttachHandlers(Capability<T> capability, Class<T> capClass, Callable<T> factory, CapabilityEventHandler handler) {
        for (Method method : capClass.getMethods()) {
            AutoCapability.AttachCapabilityCheckHandler checker = method.getAnnotation(AutoCapability.AttachCapabilityCheckHandler.class);
            if (checker == null) continue;

            ResourceLocation key = new ResourceLocation(checker.value());
            if (!Modifier.isStatic(method.getModifiers())) {
                LadyLib.LOGGER.fatal("Found unexpected method signature for annotation AttachCapabilityCheckHandler. Such methods should be static.");
                continue;
            }
            if (method.getParameterTypes().length > 0) {
                LadyLib.LOGGER.fatal("Found unexpected method signature for annotation AttachCapabilityCheckHandler. Such methods should not have any parameter.");
                continue;
            }
            Type retType = method.getGenericReturnType();
            if (!(retType instanceof ParameterizedType) || !Predicate.class.isAssignableFrom(method.getReturnType())) {
                LadyLib.LOGGER.fatal("Found unexpected method signature for annotation AttachCapabilityCheckHandler. Such methods should have a generic return value implementing Predicate.");
                continue;
            }
            Type retParamType = ((ParameterizedType) retType).getActualTypeArguments()[0];
            if (retParamType instanceof Class) {
                Class<?> retClass = (Class) retParamType;
                Predicate predicate;
                try {
                    predicate = (Predicate) method.invoke(null);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LadyLib.LOGGER.fatal("Error while calling AttachCapabilityCheckHandler method", e);
                    continue;
                }
                if (Entity.class.isAssignableFrom(retClass)) {
                    @SuppressWarnings("unchecked") CapabilityEventHandler.ProviderInfo<Entity, ?> info = new CapabilityEventHandler.ProviderInfo<>(key, predicate, capability, factory);
                    handler.entityProviders.add(info);
                    continue;
                } else if (ItemStack.class.isAssignableFrom(retClass)) {
                    @SuppressWarnings("unchecked") CapabilityEventHandler.ProviderInfo<ItemStack, ?> info = new CapabilityEventHandler.ProviderInfo<>(key, predicate, capability, factory);
                    handler.itemProviders.add(info);
                    continue;
                } else if (TileEntity.class.isAssignableFrom(retClass)) {
                    @SuppressWarnings("unchecked") CapabilityEventHandler.ProviderInfo<TileEntity, ?> info = new CapabilityEventHandler.ProviderInfo<>(key, predicate, capability, factory);
                    handler.teProviders.add(info);
                    continue;
                }
            }
            LadyLib.LOGGER.fatal("Found unexpected method signature for annotation AttachCapabilityCheckHandler. The returned predicate should have a generic type of either Entity, ItemStack or TileEntity.");
        }
    }

    @SuppressWarnings("unchecked")
    public static <L, T, R> R createFactory(Class<T> clazz, String invokedName, Class<L> lambdaType) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.findConstructor(clazz, MethodType.methodType(void.class));
            CallSite metafactory = LambdaMetafactory.metafactory(
                    lookup,
                    invokedName,
                    MethodType.methodType(lambdaType),
                    MethodType.methodType(Object.class),
                    handle,
                    MethodType.methodType(clazz)
            );
            return (R) metafactory.getTarget().invoke();
        } catch (Throwable throwable) {
            throw new UnableToGetFactoryException(throwable);
        }
    }

    private <T> Capability.IStorage<T> createStorage(Class<T> capClass) {
        try {
            return new ReflectiveCapabilityStorage<>(capClass);
        } catch (IllegalAccessException e) {
            throw new UnableToCreateStorageException(e);
        }
    }

    public static class UnableToGetFactoryException extends RuntimeException {
        public UnableToGetFactoryException(Throwable cause) {
            super(cause);
        }
    }

    public static class UnableToCreateStorageException extends RuntimeException {
        public UnableToCreateStorageException(Throwable cause) {
            super(cause);
        }
    }
}
