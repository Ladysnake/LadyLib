package ladylib.registration;

import ladylib.LadyLib;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Class handling most of the registration work automatically
 */
public class AutoRegistrar {

    private ItemRegistrar itemRegistrar;
    private BlockRegistrar blockRegistrar;

    private List<AutoRegistryRef> references = new ArrayList<>();

    public AutoRegistrar(LadyLib ladyLib, ASMDataTable asmData) {
        this.itemRegistrar = new ItemRegistrar(ladyLib);
        this.blockRegistrar = new BlockRegistrar(ladyLib, itemRegistrar);
        // find all classes that will be handled by this registrar
        Set<ASMDataTable.ASMData> allRegistryHandlers = asmData.getAll(AutoRegister.class.getName());
        for (ASMDataTable.ASMData data : allRegistryHandlers) {
            // each mod using this library has its own instance so we must only affect the owning mod
            String modId = (String) data.getAnnotationInfo().get("value");
            if (modId.equals(ladyLib.getModId())) {
                String className = data.getClassName();
                String annotationTarget = data.getObjectName();
                boolean isClass = className.equals(annotationTarget);
                try {
                    Class<?> clazz = Class.forName(data.getClassName(), false, getClass().getClassLoader());
                    if (isClass)
                        scanClassForFields(modId, clazz);
                    else
                        references.add(new AutoRegistryRef(modId, clazz.getDeclaredField(annotationTarget)));
                } catch (ClassNotFoundException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
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
                references.add(new AutoRegistryRef(modId, f));
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void onRegistryRegister(RegistryEvent.Register event) {
        references.stream()
                // Only register for the right event, incidentally filters out entries with no corresponding registry
                .filter(ref -> ref.isValidForRegistry(event.getRegistry()))
                .forEach(ref -> {
                    IForgeRegistryEntry value = ref.nameAndGet();
                    // items and blocks have additional registration behaviours
                    if (value instanceof Item) {
                        itemRegistrar.addItem((Item) value, ref);
                    } else if (value instanceof Block) {
                        blockRegistrar.addBlock((Block) value, ref);
                    } else {
                        event.getRegistry().register(value);
                    }
                });
    }

    public BlockRegistrar getBlockRegistrar() {
        return blockRegistrar;
    }

    public ItemRegistrar getItemRegistrar() {
        return itemRegistrar;
    }
}
