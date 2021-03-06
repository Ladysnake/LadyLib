package ladylib.registration.internal;

import com.google.common.collect.ImmutableList;
import joptsimple.internal.Strings;
import ladylib.LadyLib;
import ladylib.capability.internal.CapabilityRegistrar;
import ladylib.misc.ReflectionFailedException;
import ladylib.registration.AutoRegister;
import ladylib.registration.AutoRegisterTile;
import ladylib.registration.BlockRegistrar;
import ladylib.registration.ItemRegistrar;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.LoaderException;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.ObjectHolderRegistry;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;

import java.lang.reflect.*;
import java.util.*;

/**
 * Class handling most of the registration work automatically
 */
public class AutoRegistrar {

    private ItemRegistrar itemRegistrar;
    private BlockRegistrar blockRegistrar;
    private CapabilityRegistrar capRegistrar;

    private List<AutoRegistryRef> references = new ArrayList<>();
    private Map<Class<? extends IForgeRegistryEntry>, Map<ResourceLocation, IForgeRegistryEntry>> remappings = new HashMap<>();

    public AutoRegistrar(ASMDataTable asmData) {
        this.itemRegistrar = new ItemRegistrar();
        this.blockRegistrar = new BlockRegistrar(itemRegistrar);
        this.capRegistrar = new CapabilityRegistrar();
        findRegistryHandlers(asmData);
        capRegistrar.findCapabilityImplementations(asmData);
    }

    private void findRegistryHandlers(ASMDataTable asmData) {
        // find all classes that will be handled by this registrar
        Set<ASMDataTable.ASMData> allRegistryHandlers = asmData.getAll(AutoRegister.class.getName());

        for (ASMDataTable.ASMData data : allRegistryHandlers) {
            String modId = (String) data.getAnnotationInfo().get("value");
            boolean injectObjectHolder = Boolean.TRUE.equals(data.getAnnotationInfo().get("injectObjectHolder"));
            String className = data.getClassName();
            String annotationTarget = data.getObjectName();
            boolean isClass = className.equals(annotationTarget);
            try {
                Class<?> clazz = Class.forName(data.getClassName(), false, getClass().getClassLoader());
                if (isClass) {
                    scanClassForFields(modId, clazz, injectObjectHolder);
                } else {
                    Field target = clazz.getDeclaredField(annotationTarget);
                    references.add(new FieldRef(modId, target));
                    if (injectObjectHolder) injectObjectHolderReference(modId, target);
                }
            } catch (ClassNotFoundException | NoSuchFieldException e) {
                LadyLib.LOGGER.warn("Could not automatically register annotated registrar element", e);
            }
        }
    }

    public void autoRegisterTileEntities(ASMDataTable asmData) {
        Set<ASMDataTable.ASMData> autoRegisterTypes = asmData.getAll(AutoRegisterTile.class.getName());
        for (ASMDataTable.ASMData data : autoRegisterTypes) {
            // each mod using this library has its own instance so we must only affect the owning mod
            String modId = (String) data.getAnnotationInfo().get("value");
            String className = data.getClassName();
            try {
                @SuppressWarnings("unchecked") Class<? extends TileEntity> teClass =
                        (Class<? extends TileEntity>) Class.forName(className, true, getClass().getClassLoader());
                String name = (String) data.getAnnotationInfo().get("name");
                if (Strings.isNullOrEmpty(name)) {
                    name = teClass.getSimpleName().toLowerCase(Locale.ENGLISH);
                }
                GameRegistry.registerTileEntity(teClass, new ResourceLocation(modId, name));
                if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
                    ClientRegistrar.registerTESR(teClass, (Type) data.getAnnotationInfo().get("renderer"));
                }
            } catch (ClassNotFoundException e) {
                LadyLib.LOGGER.warn("Could not look automatically register tile entity class {}", className, e);
            }
        }
    }

    private void scanClassForFields(String modId, Class<?> autoRegisterClass, boolean injectObjectHolder) {
        for (Field f : autoRegisterClass.getFields()) {
            int mods = f.getModifiers();
            // use the same criteria as ObjectHolderRegistry to detect candidates
            boolean isMatch = Modifier.isPublic(mods) && Modifier.isStatic(mods) && Modifier.isFinal(mods);
            // No point in trying to automatically register non registrable fields
            // also don't register annotated fields here
            if (isMatch && IForgeRegistryEntry.class.isAssignableFrom(f.getType()) &&
                    !f.isAnnotationPresent(AutoRegister.Ignore.class) && !f.isAnnotationPresent(AutoRegister.class)) {
                references.add(new FieldRef(modId, f));
                if (injectObjectHolder) injectObjectHolderReference(modId, f);
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    void onRegistryRegister(RegistryEvent.Register event) {
        shutupForge(() -> references.stream()
            // Only register for the right event, incidentally filters out entries with no corresponding registry
            .filter(ref -> ref.isValidForRegistry(event.getRegistry()))
            .forEach(ref -> {
                IForgeRegistryEntry value = ref.nameAndGet();
                for (String oldName : ref.getOldNames()) {
                    this.remappings
                            .computeIfAbsent(event.getRegistry().getRegistrySuperType(), a -> new HashMap())
                            .put(new ResourceLocation(ref.getModId(), oldName), value);
                }
                // items and blocks have additional registration behaviours
                if (value instanceof Item) {
                    itemRegistrar.addItem((Item) value, ref.isListed(), ref.getOreNames());
                } else if (value instanceof Block) {
                    blockRegistrar.addBlock((Block) value, ref.isListed(), ref.isMakeItemBlock(), ref.getOreNames());
                } else {
                    event.getRegistry().register(value);
                }
            })
        );
    }

    /**
     * Make forge not spew "dangerous alternative prefix" messages in this block.
     * Adapted from QuackLib (https://github.com/therealfarfetchd/QuackLib/blob/1.12/src/main/kotlin/therealfarfetchd/quacklib/common/api/util/AutoRegistry.kt#L230-L242)
     * @author TheRealFarfetchd
     * @param op the operation to run while warnings are disabled
     */
    private void shutupForge(Runnable op) {
        try {
            Logger log = FMLLog.log;
            Field privateConfigF = org.apache.logging.log4j.core.Logger.class.getDeclaredField("privateConfig");
            privateConfigF.setAccessible(true);
            Object privateConfig = privateConfigF.get(log);
            Field intLevelF = privateConfig.getClass().getDeclaredField("intLevel");
            intLevelF.setAccessible(true);
            int intLevel = intLevelF.getInt(privateConfig);
            intLevelF.set(privateConfig, 299);
            try {
                op.run();
            } finally {
                intLevelF.set(privateConfig, intLevel);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ReflectionFailedException("Could not make Forge shut up", e);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    void onRegistryMissingMappings(RegistryEvent.MissingMappings event) {
        ImmutableList<RegistryEvent.MissingMappings.Mapping> mappings = event.getMappings();
        Map<ResourceLocation, IForgeRegistryEntry> remaps = remappings.get(event.getRegistry().getRegistrySuperType());
        if (remaps == null) {
            return;
        }
        for (RegistryEvent.MissingMappings.Mapping mapping : mappings) {
            if (remaps.containsKey(mapping.key)) {
                mapping.remap(remaps.get(mapping.key));
            }
        }
    }

    public BlockRegistrar getBlockRegistrar() {
        return blockRegistrar;
    }

    public ItemRegistrar getItemRegistrar() {
        return itemRegistrar;
    }


    private static Constructor<?> objectHolderRefConstr;
    private static Method objectHolderRegistry$addHolderReference;

    /**
     * Injects an object holder reference into {@link ObjectHolderRegistry}. Reflection fest warning.
     */
    private static void injectObjectHolderReference(String modId, Field field) {
        if (objectHolderRefConstr == null || objectHolderRegistry$addHolderReference == null) {
            try {
                // private class
                Class<?> objectHolderRefClass = Class.forName("net.minecraftforge.registries.ObjectHolderRef");
                // package-private constructor
                objectHolderRefConstr = objectHolderRefClass.getDeclaredConstructor(Field.class, ResourceLocation.class, boolean.class);
                objectHolderRefConstr.setAccessible(true);
                // private method
                objectHolderRegistry$addHolderReference = ObjectHolderRegistry.class.getDeclaredMethod("addHolderReference", objectHolderRefClass);
                objectHolderRegistry$addHolderReference.setAccessible(true);
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                LadyLib.LOGGER.error("Could not setup Object Holder injection", e);
                throw new LoaderException(e);
            }
        }
        try {
            ResourceLocation injectedObject = new ResourceLocation(modId, field.getName().toLowerCase(Locale.ENGLISH));
            Object objectHolderRef = objectHolderRefConstr.newInstance(field, injectedObject, false);
            objectHolderRegistry$addHolderReference.invoke(ObjectHolderRegistry.INSTANCE, objectHolderRef);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LadyLib.LOGGER.warn("Could not inject ObjectHolderRef", e);
        }
    }
}
