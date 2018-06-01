package ladylib.registration;

import com.google.common.base.Preconditions;
import ladylib.LadyLib;
import ladylib.client.ICustomLocation;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Class in charge of item-specific registration behaviour
 */
public class ItemRegistrar {

    @SuppressWarnings({"ConstantConditions"})
    public static <T extends Item> T name(T item, String name) {
        item.setRegistryName(name).setUnlocalizedName(item.getRegistryName().getResourceDomain() + "." + name);
        return item;
    }

    /**
     * A map tracking all registered items and the associated info
     */
    private Map<Item, ItemInfo> allItems = new HashMap<>();

    /**
     * Adds an item to the list
     *
     * @param item   the item to be registered
     * @param listed whether this item will appear in the creative and JEI tabs
     * @throws IllegalArgumentException if the given item has a null registry name
     */
    public void addItem(@Nonnull Item item, boolean listed, String... oreNames) {
        Preconditions.checkNotNull(item.getRegistryName(), "Can't use a null-name for the registry, object %s.", item);
        allItems.put(item, new ItemInfo(listed, oreNames));
    }

    // Needs to be called after the main registrar has discovered all items
    @SubscribeEvent(priority = EventPriority.LOW)
    void registerItems(RegistryEvent.Register<Item> event) {
        allItems.forEach((item, info) -> {
            event.getRegistry().register(item);
            if (info.listed) {
                item.setCreativeTab(LadyLib.instance.getContainer(item.getRegistryName().getResourceDomain()).getCreativeTab());
            }
            for (String oreName : info.oreNames) {
                OreDictionary.registerOre(oreName, item);
            }
        });
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    void registerRenders(ModelRegistryEvent event) {
        allItems.keySet().stream()
                .filter(itemIn -> !(Block.getBlockFromItem(itemIn) instanceof BlockFluidBase))
                .forEach(ItemRegistrar::registerRender);
    }

    @SideOnly(Side.CLIENT)
    public static void registerRender(Item item) {
        if (item instanceof ICustomLocation) {
            // let the item handle its model registration
            ((ICustomLocation) item).registerRender();
        } else {
            // use the standard procedure
            registerRender(item, new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()).toString()));
        }
    }

    @SideOnly(Side.CLIENT)
    public static void registerRender(Item item, ModelResourceLocation loc) {
        ModelLoader.setCustomModelResourceLocation(item, 0, loc);
    }

    public Collection<Item> getAllItems() {
        return allItems.keySet();
    }

    public Stream<Item> getInvisibleItems() {
        return allItems.entrySet().stream()
                .filter(entry -> !entry.getValue().listed)
                .map(Map.Entry::getKey);
    }

    static class ItemInfo {
        /**
         * whether the item should be invisible in the creative and JEI tabs
         */
        boolean listed;
        String[] oreNames;

        ItemInfo(boolean listed, String[] oreNames) {
            this.listed = listed;
            this.oreNames = oreNames;
        }
    }

}
