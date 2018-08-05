package ladylib.registration;

import com.google.common.base.Preconditions;
import ladylib.LadyLib;
import ladylib.client.ItemRenderRegistrationHandler;
import ladylib.registration.AutoRegister.Unlisted;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
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

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
                        Utility methods
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * Utility method to set the registry name and translation key of an item from a single value. <br>
     * <p>
     * The namespace will be deduced from the currently active mod container.
     * The translation key will be generated from the obtained namespace and the provided name, separated by a dot
     * ("<tt>modid.name</tt>")
     * </p>
     * The item must not have a registry name already set.
     *
     * @param item the item to name
     * @param name the main name of the item
     * @param <T> the type of the item
     * @return <code>item</code>
     */
    @SuppressWarnings({"ConstantConditions"})
    public static <T extends Item> T name(T item, String name) {
        item.setRegistryName(name).setTranslationKey(item.getRegistryName().getNamespace() + "." + name);
        return item;
    }

    /**
     * Sets an item's model to the one designated by the passed in location.
     * @param item an item for which to register a render
     * @param loc a model location
     */
    public static void registerRender(Item item, ModelResourceLocation loc) {
        ModelLoader.setCustomModelResourceLocation(item, 0, loc);
    }

    /**
     * The default render registration method for items.
     * <p>
     * Sets the item's model based on its registry name. If the item implements {@link ItemRenderRegistrationHandler},
     * the registration is delegated to the item instead.
     * @param item an item for which to register a render.
     */
    public static void registerRender(Item item) {
        if (item instanceof ItemRenderRegistrationHandler) {
            // let the item handle its model registration
            ((ItemRenderRegistrationHandler) item).registerRender(item);
        } else {
            // use the standard procedure
            registerRender(item, new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()).toString()));
        }
    }

     /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
                        Registrar API methods
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * Adds an item to the list of entries to automatically register
     *
     * @param item   the item to be registered
     * @param listed whether this item will appear in the creative and JEI tabs
     * @throws IllegalArgumentException if the given item has a null registry name
     */
    public void addItem(@Nonnull Item item, boolean listed, String... oreNames) {
        Preconditions.checkNotNull(item.getRegistryName(), "Can't use a null-name for the registry, object %s.", item);
        allItems.put(item, new ItemInfo(listed, oreNames));
        if (item instanceof ItemRenderRegistrationHandler) {
            setModelRegistrationHandler(item.getRegistryName(), ItemRegistrar::registerRender);
        }
    }

    /**
     * Sets the {@link ItemRenderRegistrationHandler} to use for the item with the given registry name
     * <p>
     * This method can be called at any time before {@link ModelRegistryEvent}.
     *
     * @param registryName the registry name of an item
     * @param handler a render registration handler
     */
    public void setModelRegistrationHandler(ResourceLocation registryName, ItemRenderRegistrationHandler handler) {
        this.modelRegistrationHandlers.put(registryName, handler);
    }

    /**
     *
     * @return the list of every item known to this registrar
     */
    public Collection<Item> getAllItems() {
        return allItems.keySet();
    }

    /**
     *
     * @return the list of every invisible item known to this registrar according to {@link Unlisted}
     */
    public Stream<Item> getInvisibleItems() {
        return allItems.entrySet().stream()
                .filter(entry -> !entry.getValue().listed)
                .map(Map.Entry::getKey);
    }


    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
                            Internal
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * A map tracking all registered items and the associated info
     */
    private Map<Item, ItemInfo> allItems = new HashMap<>();
    private Map<ResourceLocation, ItemRenderRegistrationHandler> modelRegistrationHandlers = new HashMap<>();

    // Needs to be called after the main registrar has discovered all items
    @SubscribeEvent(priority = EventPriority.LOW)
    void registerItems(RegistryEvent.Register<Item> event) {
        allItems.forEach((item, info) -> {
            event.getRegistry().register(item);
            if (info.listed) {
                item.setCreativeTab(LadyLib.instance.getContainer(item.getRegistryName().getNamespace()).getCreativeTab());
            }
            for (String oreName : info.oreNames) {
                OreDictionary.registerOre(oreName, item);
            }
        });
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    void registerRenders(ModelRegistryEvent event) {
        allItems.keySet().forEach(item -> modelRegistrationHandlers.get(item.getRegistryName()).registerRender(item));
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
