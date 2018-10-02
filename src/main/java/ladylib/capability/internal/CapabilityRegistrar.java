package ladylib.capability.internal;

import ladylib.LadyLib;
import ladylib.capability.AutoCapability;
import ladylib.capability.ReflectiveCapabilityStorage;
import ladylib.misc.ReflectionUtil;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import org.apache.logging.log4j.message.FormattedMessage;

import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public class CapabilityRegistrar {

    public void findCapabilityImplementations(ASMDataTable asmData) {
        // find all classes that will be handled by this registrar
        Set<ASMDataTable.ASMData> allRegistryHandlers = asmData.getAll(AutoCapability.class.getName());
        CapabilityEventHandler handler = new CapabilityEventHandler();
        try {
            Field providersField = CapabilityManager.class.getDeclaredField("providers");
            providersField.setAccessible(true);
            @SuppressWarnings("unchecked") Map<String, Capability<?>> providers =
                    (Map<String, Capability<?>>) providersField.get(CapabilityManager.INSTANCE);
            for (ASMDataTable.ASMData data : allRegistryHandlers) {
                findCapabilityImplementation(handler, providers, data);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LadyLib.LOGGER.fatal("Unable to process capabilities", e);
        }
    }

    private <T> void findCapabilityImplementation(CapabilityEventHandler handler, Map<String, Capability<?>> providers, ASMDataTable.ASMData data) throws IllegalAccessException {
        String className = data.getClassName();
        Map<String, Object> annotationInfo = data.getAnnotationInfo();
        @Nullable org.objectweb.asm.Type intfName = (org.objectweb.asm.Type) annotationInfo.get("value");
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            @SuppressWarnings("unchecked")
            Class<? extends T> impl = (Class<T>) Class.forName(className, false, classLoader);
            @SuppressWarnings("unchecked")
            Class<T> cap = (Class<T>) (intfName == null
                    ? impl
                    : Class.forName(intfName.getClassName(), false, classLoader)
            );
            if (!cap.isAssignableFrom(impl)) {
                throw new IllegalArgumentException("The given interface " + cap + " is not implemented by the capability " + impl);
            }
            Capability.IStorage<T> storage = createStorage(impl, (org.objectweb.asm.Type) annotationInfo.get("storage"));
            createRegisterCapability(cap, impl, storage, handler, providers);
        } catch (IllegalArgumentException | ReflectionUtil.UnableToGetFactoryException | ClassNotFoundException | InstantiationException e) {
            LadyLib.LOGGER.error(new FormattedMessage("Could not register a capability for the class {}", className), e);
        }
    }

    private <T> void createRegisterCapability(Class<T> clazz, Class<? extends T> implementation, Capability.IStorage<T> storage, CapabilityEventHandler handler, Map<String, Capability<?>> providers) {
        Callable<? extends T> factory = ReflectionUtil.createFactory(implementation, "call", Callable.class);
        CapabilityManager.INSTANCE.register(clazz, storage, factory);
        @SuppressWarnings("unchecked") Capability<T> capability = (Capability<T>) providers.get(implementation.getName().intern());
        addAttachHandlers(capability, clazz, factory, handler);
    }

    @SuppressWarnings("unchecked")
    private <T> Capability.IStorage<T> createStorage(Class<? extends T> impl, @Nullable org.objectweb.asm.Type storage) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<?> clazz = storage == null ? null : Class.forName(storage.getClassName());
        if (clazz == null) {
            return new ReflectiveCapabilityStorage(impl);
        } else if (Capability.IStorage.class.isAssignableFrom(clazz)){
            return (Capability.IStorage<T>) clazz.newInstance();
        } else {
            throw new IllegalStateException("Invalid annotation info, " + storage + " is not a valid storage type");
        }
    }

    private <T> void addAttachHandlers(Capability<T> capability, Class<T> capClass, Callable<? extends T> factory, CapabilityEventHandler handler) {
        boolean attached = false;
        for (Method method : capClass.getMethods()) {
            attached |= addAttachedHandler(capability, factory, handler, method);
        }
        // Only register the event handler if at least one capability needs it
        // this can be called multiple times safely, the event bus will ignore the superfluous registrations
        if (attached) {
            MinecraftForge.EVENT_BUS.register(handler);
        }
    }

    private <T> boolean addAttachedHandler(Capability<T> capability, Callable<? extends T> factory, CapabilityEventHandler handler, Method method) {
        AutoCapability.AttachCapabilityCheckHandler checker = method.getAnnotation(AutoCapability.AttachCapabilityCheckHandler.class);
        if (checker == null) {
            return false;
        }

        ResourceLocation key = new ResourceLocation(checker.value());
        if (!Modifier.isStatic(method.getModifiers())) {
            logBadSignature(method, " Such methods should be static.");
            return false;
        }
        if (method.getParameterTypes().length > 0) {
            logBadSignature(method, "Such methods should not have any parameter.");
            return false;
        }
        Type retType = method.getGenericReturnType();
        if (!(retType instanceof ParameterizedType) || !Predicate.class.isAssignableFrom(method.getReturnType())) {
            logBadSignature(method, "Such methods should have a generic return value implementing Predicate.");
            return false;
        }
        Type retParamType = ((ParameterizedType) retType).getActualTypeArguments()[0];
        if (retParamType instanceof Class) {
            Class<?> retClass = (Class) retParamType;
            Predicate predicate;
            try {
                predicate = (Predicate) method.invoke(null);
            } catch (IllegalAccessException | InvocationTargetException e) {
                LadyLib.LOGGER.error("Error while calling AttachCapabilityCheckHandler method", e);
                return false;
            }
            @SuppressWarnings("unchecked") CapabilityEventHandler.ProviderInfo info = new CapabilityEventHandler.ProviderInfo<>(key, predicate, capability, factory);
            if (Entity.class.isAssignableFrom(retClass)) {
                addProvider(info, handler.entityProviders);
                return true;
            } else if (ItemStack.class.isAssignableFrom(retClass)) {
                addProvider(info, handler.itemProviders);
                return true;
            } else if (TileEntity.class.isAssignableFrom(retClass)) {
                addProvider(info, handler.teProviders);
                return true;
            }
        }
        logBadSignature(method, "The returned predicate should have a generic type of either Entity, ItemStack or TileEntity.");
        return false;
    }

    @SuppressWarnings("unchecked")
    private void addProvider(CapabilityEventHandler.ProviderInfo info, List list) {
        list.add(info);
    }

    private void logBadSignature(Method method, String s) {
        LadyLib.LOGGER.error("Found unexpected method signature {} for annotation AttachCapabilityCheckHandler: {}", method, s);
    }

}
