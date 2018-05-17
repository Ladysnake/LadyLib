package ladylib.registration;

import com.google.common.collect.ImmutableList;
import ladylib.capability.internal.CapabilityRegistrar;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
        capRegistrar.findRegistryHandlers(asmData);
    }

    private void findRegistryHandlers(ASMDataTable asmData) {
        // find all classes that will be handled by this registrar
        Set<ASMDataTable.ASMData> allRegistryHandlers = asmData.getAll(AutoRegister.class.getName());
        for (ASMDataTable.ASMData data : allRegistryHandlers) {
            String modId = (String) data.getAnnotationInfo().get("value");
            String className = data.getClassName();
            String annotationTarget = data.getObjectName();
            boolean isClass = className.equals(annotationTarget);
            try {
                Class<?> clazz = Class.forName(data.getClassName(), false, getClass().getClassLoader());
                if (isClass)
                    scanClassForFields(modId, clazz);
                else
                    references.add(new FieldRef(modId, clazz.getDeclaredField(annotationTarget)));
            } catch (ClassNotFoundException | NoSuchFieldException e) {
                e.printStackTrace();
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
                if (name == null || name.isEmpty())
                    name = teClass.getSimpleName().toLowerCase(Locale.ENGLISH);
                GameRegistry.registerTileEntity(teClass, modId + ":" + name);
                if (FMLCommonHandler.instance().getSide() == Side.CLIENT)
                    ClientRegistrar.registerTESR(teClass, (Type) data.getAnnotationInfo().get("renderer"));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void scanClassForFields(String modId, Class<?> autoRegisterClass) {
        for (Field f : autoRegisterClass.getFields()) {
            int mods = f.getModifiers();
            // use the same criteria as ObjectHolderRegistry to detect candidates
            boolean isMatch = Modifier.isPublic(mods) && Modifier.isStatic(mods) && Modifier.isFinal(mods);
            // No point in trying to automatically register non registrable fields
            // also don't register annotated fields here
            if (isMatch && IForgeRegistryEntry.class.isAssignableFrom(f.getType()) &&
                    !f.isAnnotationPresent(AutoRegister.Ignore.class) && !f.isAnnotationPresent(AutoRegister.class)) {
                references.add(new FieldRef(modId, f));
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void onRegistryRegister(RegistryEvent.Register event) {
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
                    itemRegistrar.addItem((Item) value, ref);
                } else if (value instanceof Block) {
                    blockRegistrar.addBlock((Block) value, ref);
                } else {
                    event.getRegistry().register(value);
                }
            }));
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
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void onRegistryMissingMappings(RegistryEvent.MissingMappings event) {
        ImmutableList<RegistryEvent.MissingMappings.Mapping> mappings = event.getMappings();
        Map<ResourceLocation, IForgeRegistryEntry> remaps = remappings.get(event.getRegistry().getRegistrySuperType());
        if (remaps == null) return;
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
}
