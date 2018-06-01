package ladylib.client;

import ladylib.registration.ItemRegistrar;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Items that implement this interface can control their rendering registration
 */
public interface ICustomLocation {

    /**
     * Called by the default implementation of {@link #registerRender()}.
     * @return a path to the model json file for this item
     */
    ModelResourceLocation getModelLocation();

    /**
     * Called by {@link ladylib.registration.ItemRegistrar#registerRender(Item)}.
     * Use this to register the item's render.
     */
    @SideOnly(Side.CLIENT)
    default void registerRender() {
        ItemRegistrar.registerRender((Item) this, getModelLocation());
    }

}