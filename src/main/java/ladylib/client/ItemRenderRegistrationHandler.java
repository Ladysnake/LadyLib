package ladylib.client;

import ladylib.registration.ItemRegistrar;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

/**
 * A registration function that handles the registration of an item's render.
 * <p>
 * Custom render registration handlers can be set using
 * {@link ItemRegistrar#setCustomRenderRegistrationHandler(ResourceLocation, ItemRenderRegistrationHandler)}
 * <p>
 * If an item implements this interface, it will automatically handle its own render registration, unless
 * another {@link ItemRenderRegistrationHandler} is registered for that item.
 */
@FunctionalInterface
public interface ItemRenderRegistrationHandler {

    /**
     * Called by {@link ladylib.registration.ItemRegistrar#registerRender(Item)}.
     * Use this to register the item's render.
     * @param item the item for which to register the render
     */
    @SideOnly(Side.CLIENT)
    void registerRender(Item item);

    /**
     * Accepts a string representing a valid model location and returns a
     * {@link ItemRenderRegistrationHandler} that registers item renders
     * with a {@link ModelResourceLocation} constructed from that string.
     * @param modelLocation a path to the model json file for this item
     */
    static ItemRenderRegistrationHandler forCustomLocation(@Nonnull String modelLocation) {
        final ModelResourceLocation mrl = new ModelResourceLocation(modelLocation);
        return item -> {
            ResourceLocation registryName = item.getRegistryName();
            if (registryName == null) {
                throw new IllegalStateException("Registry name must be set before render registration");
            }
            ItemRegistrar.registerRender(item, mrl);
        };
    }

}